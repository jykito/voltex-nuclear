package com.jykito.voltexnuclear.core.reactor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The fission reactor at work, tick by tick (DESIGN steps 4 to 8, numbers in {@link Fission}), with no Minecraft in it;
 * the reactor block (stage 6) feeds it the layout and the coolant and takes the energy, the spent rods and the products.
 * It follows the reactor mock-up (tools/texture_viewer/nuclear_reactor.html in the Voltex workshop).
 * <p>
 * Power: the knob, never below the floor the control rods allow; nothing while stopped or after SCRAM. Each tick a fuel
 * cell gives {@link Fission#FE_PER_FLUX} FE and {@link Fission#HEAT_PER_FLUX} heat (times its fuel's heat factor) per
 * unit of its flux, times the power. Its heat is shared out evenly among the outlets next to it (heat sinks, water and
 * cryo cells) and the exchanger groups next to it; a group passes its share on evenly to the outlets around any of its
 * exchangers. Heat with nowhere to go, and what an outlet cannot take, goes into the hull. An outlet takes up to its
 * rate ({@link Cooling}), a cell only while there is its fluid: water for a water cell, Voltex coolant for a cryo cell,
 * a pack's fluid for a pack's cell. The hull cools a little by itself; full, it SCRAMs the reactor. Reset after SCRAM
 * cools the hull, as in the mock-up.
 * <p>
 * Wear: a rod burns at the power it runs at and leaves the zone after its life at full power ({@link Part#life}, longer
 * for an assembly). A loaded channel gathers the flux it sees times the power and gives a product at the work of its blank
 * ({@link Part#work}).
 * <p>
 * The Watcher, the share of output into heat and the energy buffer belong to later stages (the Voltex layer, the block).
 */
public final class Reactor {

    public record Cell(int row, int col) {}

    /** A rod that burnt out on this tick and left the zone; the block sends it out as spent fuel. */
    public record Burnt(Cell cell, Part part) {}

    /**
     * What one tick did: the power it ran at, the energy, the heat the fuel gave, the heat the outlets took away, the
     * heat that went into the hull, the mB of each fluid the cells used (by its id), the rods that burnt out and the
     * channels that finished a product.
     */
    public record Tick(double power, double energy, double heat, double removed, double toHull, Map<String, Double> used,
                       List<Burnt> burntOut, List<Cell> products) {

        /** mB of the fluid the cells used. */
        public double used(String fluid) {
            return used.getOrDefault(fluid, 0.0);
        }

        /** The water the water cells boiled away, mB. */
        public double waterMb() {
            return used(Cooling.WATER);
        }

        /** The Voltex coolant the cryo cells used, mB. */
        public double coolantMb() {
            return used(Cooling.COOLANT);
        }
    }

    private final Zone zone;
    private FluxMap map;
    /** Exchanger group of each cell (-1 for none) and the outlets around each group. */
    private final int[][] group = new int[Zone.SIZE][Zone.SIZE];
    private final List<List<Cell>> groupOutlets = new ArrayList<>();

    private final double[][] wear = new double[Zone.SIZE][Zone.SIZE], channel = new double[Zone.SIZE][Zone.SIZE];
    /** The heat each outlet was given on the last tick, and what it took of it, for the window. */
    private double[][] lastLoad = new double[Zone.SIZE][Zone.SIZE], lastTook = new double[Zone.SIZE][Zone.SIZE];
    private double hull, knob = 1;
    private boolean running, scram, held;

    public Reactor(Zone zone) {
        this.zone = zone;
        changed();
    }

    public Zone zone() {
        return zone;
    }

    public FluxMap flux() {
        return map;
    }

    /** Puts a part into a cell (null takes it out); the cell starts fresh: no wear, no irradiation. */
    public void place(int row, int col, Part part) {
        place(row, col, part, 0);
    }

    /**
     * Puts a part into a cell with the work its item carries: a rod's wear in ticks at full power, a channel blank's
     * irradiation (DESIGN step 17, the blank itself stands in the cell). Other parts keep nothing; a value out of range is
     * clamped, as an item's data comes from outside.
     */
    public void place(int row, int col, Part part, double worn) {
        zone.set(row, col, part);
        boolean blank = part != null && part.kind() == Part.Kind.CHANNEL;
        wear[row][col] = part != null && part.isFuel() && !Double.isNaN(worn) ? Math.max(0, Math.min(worn, part.life())) : 0;
        channel[row][col] = blank && !Double.isNaN(worn) ? Math.max(0, Math.min(worn, part.work())) : 0;
        changed();
    }

    /** Puts the flux and the heat paths right after the layout or the chambers changed. */
    public void changed() {
        map = FluxMap.of(zone);
        groupOutlets.clear();
        for (int[] row : group) Arrays.fill(row, -1);
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++)
                if (zone.kind(r, c) == Part.Kind.HEAT_EXCHANGER && group[r][c] < 0) floodGroup(r, c, groupOutlets.size());
    }

    private void floodGroup(int r0, int c0, int id) {
        List<Cell> outlets = new ArrayList<>();
        List<Cell> open = new ArrayList<>(List.of(new Cell(r0, c0)));
        group[r0][c0] = id;
        while (!open.isEmpty()) {
            Cell at = open.remove(open.size() - 1);
            for (int[] d : FluxMap.SIDES) {
                int r = at.row() + d[0], c = at.col() + d[1];
                Part.Kind k = zone.kind(r, c);
                if (k == Part.Kind.HEAT_EXCHANGER && group[r][c] < 0) {
                    group[r][c] = id;
                    open.add(new Cell(r, c));
                } else if (k != null && k.outlet() && !outlets.contains(new Cell(r, c))) outlets.add(new Cell(r, c));
            }
        }
        groupOutlets.add(outlets);
    }

    // ---- the controls of the window

    /** The power knob, 0 to 1; the reactor runs at no less than {@link FluxMap#minPower()}. */
    public void setKnob(double knob) {
        this.knob = Math.max(0, Math.min(1, knob));
    }

    public double knob() {
        return knob;
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    /** SCRAM by hand: the reactor stops at once. */
    public void scram() {
        scram = true;
        running = false;
    }

    /** Clears a SCRAM; the hull is taken as cooled down. */
    public void reset() {
        if (!scram) return;
        scram = false;
        hull = 0;
    }

    /**
     * The block holds the reactor while it cannot take what the reactor gives (its output for spent rods is full): no
     * power and no wear, but the hull still cools. It runs on by itself once let go.
     */
    public void hold(boolean held) {
        this.held = held;
    }

    /** Puts back the state the block saved: the hull, whether it runs, whether it is SCRAMmed. */
    public void restore(double hull, boolean running, boolean scram) {
        this.hull = Double.isNaN(hull) ? 0 : Math.max(0, Math.min(hull, Fission.HULL_CAPACITY));
        this.running = running && !scram;
        this.scram = scram;
    }

    public boolean running() {
        return running;
    }

    public boolean scrammed() {
        return scram;
    }

    public boolean held() {
        return held;
    }

    public double hull() {
        return hull;
    }

    /** The power the reactor runs at now. */
    public double power() {
        return running && !scram && !held ? Math.max(knob, map.minPower()) : 0;
    }

    /** The heat an outlet took in on the last tick (and could not, which went into the hull); 0 for other cells. */
    public double load(int row, int col) {
        return lastLoad[row][col];
    }

    /**
     * The heat an outlet took away on the last tick: its load, or less when it is at its rate or a cell lacks its fluid
     * (the rest went into the hull). The window tells a cell short of fluid by this falling under the load.
     */
    public double took(int row, int col) {
        return lastTook[row][col];
    }

    // ---- channel blanks and what the cells hold

    /** Irradiation a channel blank has gathered, the flux it saw times the power, summed over ticks. */
    public double channelWork(int row, int col) {
        return channel[row][col];
    }

    /** Share of its product a channel blank has gathered, 0 to 1. */
    public double channelProgress(int row, int col) {
        Part part = zone.get(row, col);
        return part == null || part.kind() != Part.Kind.CHANNEL ? 0 : channel[row][col] / part.work();
    }

    /** Ticks at full power a rod has burnt. */
    public double wear(int row, int col) {
        return wear[row][col];
    }

    /** The ways of cooling in the zone that drink a fluid, each once, in the order of the cells: what the block asks its ports for. */
    public List<Cooling> drinking() {
        Map<Cooling, Boolean> seen = new LinkedHashMap<>();
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                Part p = zone.get(r, c);
                if (p != null && p.cooling() != null && p.cooling().drinks()) seen.put(p.cooling(), true);
            }
        return List.copyOf(seen.keySet());
    }

    // ---- the tick

    /** One tick with only the add-on's own fluids: the water and the Voltex coolant the cells may use, mB. */
    public Tick tick(double waterMb, double coolantMb) {
        return tick(Map.of(Cooling.WATER, waterMb, Cooling.COOLANT, coolantMb));
    }

    /**
     * One tick; {@code fluids} are the mB of each fluid (by its id) the cells may use this tick. A fluid that comes back
     * (the cryo cell's coolant, hot) is the caller's to give no more of than the output can take.
     */
    public Tick tick(Map<String, Double> fluids) {
        double m = power(), energy = 0, heat = 0, toHull = 0;
        double[][] load = new double[Zone.SIZE][Zone.SIZE];
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                Part part = zone.get(r, c);
                if (part == null || !part.isFuel()) continue;
                double f = map.flux(r, c), q = f * Fission.HEAT_PER_FLUX * part.fuel().heat * m;
                energy += f * Fission.FE_PER_FLUX * m;
                heat += q;
                toHull += share(r, c, q, load);
            }

        // what the cells used is summed up, not taken as "given minus left": the tests give an endless supply
        double removed = 0;
        Map<String, Double> used = new HashMap<>();
        double[][] tookAt = new double[Zone.SIZE][Zone.SIZE];
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                double h = load[r][c];
                if (h <= 0) continue;
                Cooling k = zone.get(r, c).cooling();
                double room = k.rate();
                if (k.drinks()) room = Math.min(room, Math.max(0, fluids.getOrDefault(k.fluid(), 0.0) - used.getOrDefault(k.fluid(), 0.0)) * k.heatPerMb());
                double took = Math.min(h, room);
                if (k.drinks() && took > 0) used.merge(k.fluid(), took / k.heatPerMb(), Double::sum);
                tookAt[r][c] = took;
                removed += took;
                toHull += h - took;
            }

        lastLoad = load;
        lastTook = tookAt;
        hull = Math.max(0, hull + toHull - Fission.HULL_COOLING);
        if (hull >= Fission.HULL_CAPACITY) scram = true;

        List<Burnt> burnt = new ArrayList<>();
        List<Cell> products = new ArrayList<>();
        if (m > 0) wear(m, burnt, products);
        return new Tick(m, energy, heat, removed, toHull, Map.copyOf(used), burnt, products);
    }

    /** Shares the heat of a fuel cell out to its outlets and exchanger groups; returns what has nowhere to go. */
    private double share(int r, int c, double q, double[][] load) {
        List<Cell> outlets = new ArrayList<>();
        List<Integer> groups = new ArrayList<>();
        for (int[] d : FluxMap.SIDES) {
            int nr = r + d[0], nc = c + d[1];
            Part.Kind k = zone.kind(nr, nc);
            if (k == null) continue;
            if (k.outlet()) outlets.add(new Cell(nr, nc));
            else if (k == Part.Kind.HEAT_EXCHANGER && !groups.contains(group[nr][nc])) groups.add(group[nr][nc]);
        }
        int ways = outlets.size() + groups.size();
        if (ways == 0) return q;
        double part = q / ways, lost = 0;
        for (Cell o : outlets) load[o.row()][o.col()] += part;
        for (int g : groups) {
            List<Cell> out = groupOutlets.get(g);
            if (out.isEmpty()) lost += part;
            else for (Cell o : out) load[o.row()][o.col()] += part / out.size();
        }
        return lost;
    }

    private void wear(double m, List<Burnt> burnt, List<Cell> products) {
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                Part part = zone.get(r, c);
                if (part == null) continue;
                if (part.isFuel() && (wear[r][c] += m) >= part.life()) {
                    burnt.add(new Burnt(new Cell(r, c), part));
                    zone.set(r, c, null);
                    wear[r][c] = 0;
                } else if (part.kind() == Part.Kind.CHANNEL && (channel[r][c] += map.irradiation(r, c) * m) >= part.work()) {
                    // the blank has become its product and leaves the cell, as a burnt rod does
                    products.add(new Cell(r, c));
                    zone.set(r, c, null);
                    channel[r][c] = 0;
                }
            }
        if (!burnt.isEmpty() || !products.isEmpty()) changed();
    }
}
