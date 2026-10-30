package com.jykito.voltexnuclear.core.reactor;

import java.util.List;
import java.util.Map;

/**
 * A ready layout of the zone (DESIGN step 24): JEI shows it with what it gives, and lays it into a reactor through the
 * ghosts of the cells. Seven rows of seven letters as on the reactor page ('.' an empty cell), the chambers it needs as
 * the sides "LRTB" it opens, the power knob it is meant to run at. What it gives comes from the reactor model itself, so
 * JEI and the reactor never disagree.
 */
public record Scheme(String key, String chambers, double knob, String... rows) {

    /** The letters of the reactor page. */
    public static final Map<Character, Part> CODE = Map.ofEntries(
            Map.entry('L', Part.fuel(Fuel.LOW_ENRICHED, 1)), Map.entry('l', Part.fuel(Fuel.LOW_ENRICHED, 2)),
            Map.entry('Q', Part.fuel(Fuel.LOW_ENRICHED, 4)), Map.entry('h', Part.fuel(Fuel.HIGH_ENRICHED, 1)),
            Map.entry('H', Part.fuel(Fuel.HIGH_ENRICHED, 2)), Map.entry('U', Part.fuel(Fuel.HIGH_ENRICHED, 4)),
            Map.entry('M', Part.fuel(Fuel.MOX, 1)), Map.entry('m', Part.fuel(Fuel.MOX, 2)), Map.entry('Y', Part.fuel(Fuel.MOX, 4)),
            Map.entry('G', Part.GRAPHITE), Map.entry('D', Part.HEAVY_WATER),
            Map.entry('C', Part.CONTROL_ROD), Map.entry('c', Part.CHANNEL), Map.entry('S', Part.HEAT_SINK),
            Map.entry('X', Part.HEAT_EXCHANGER), Map.entry('W', Part.WATER_CELL), Map.entry('K', Part.CRYO_CELL),
            Map.entry('R', Part.REFLECTOR), Map.entry('A', Part.ABSORBER));

    /** Ticks in an hour, for what a scheme burns and turns in an hour. */
    public static final double TICKS_PER_HOUR = 72_000;

    // The ladder of stage 14 (the author: "two schemes are too few, there should be schemes for single, dual, quad, mixed,
    // for the different stages"). The layouts were searched on this model, mirror symmetric so that they look ordered, at
    // full power with no heat into the hull. Era 4 has the 5 x 5 zone and single low enriched rods; era 5 brings the
    // assemblies, then the chambers with the cryo cells and the channels, high enriched uranium and MOX; era 6 heavy water.

    /** Era 4, the first one: single low enriched rods round graphite, sinks and exchangers, one control rod. */
    public static final Scheme FIRST = new Scheme("first", "", 1,
            ".......", ".SXSXS.", ".LGLGL.", ".SXCXS.", ".LGLGL.", ".SXSXS.", ".......");
    /** Era 4 at its best without water: single rods in a cross of sinks, four control rods in the corners. */
    public static final Scheme SINGLE = new Scheme("single", "", 1,
            ".......", ".CLSLC.", ".LGLGL.", ".SLSLS.", ".LGLGL.", ".CLSLC.", ".......");
    /** Era 4 at its best: the same rods cooled by water cells, which boil about 400 mB a second. */
    public static final Scheme WATER = new Scheme("water", "", 1,
            ".......", ".CLWLC.", ".LGLGL.", ".WLGLW.", ".LGLGL.", ".CLWLC.", ".......");
    /** Era 5, the first step: dual low enriched assemblies in the 5 x 5 zone, before the chambers. */
    public static final Scheme DUAL_SMALL = new Scheme("dual_small", "", 1,
            ".......", ".SlWlS.", ".lGlGl.", ".WlClW.", ".lGlGl.", ".SlWlS.", ".......");
    /** Era 5, 7 x 7: single high enriched rods from the press, no rod fabricator needed. */
    public static final Scheme HIGH = new Scheme("high", "LRTB", 1,
            "hGhKhGh", "KhGhGhK", "hGhChGh", "WKX.XKW", "hGhChGh", "KhGhGhK", "hGhKhGh");
    /** Era 5, 7 x 7: dual low enriched assemblies, the thrifty way to the same power. */
    public static final Scheme DUAL = new Scheme("dual", "LRTB", 1,
            "KlGCGlK", "lGlGlGl", "GlKlKlG", "lWlRlWl", "GlKlKlG", "lGlGlGl", "KlGCGlK");
    /** Era 5, 7 x 7: dual high enriched assemblies round a cross of cryo cells. */
    public static final Scheme POWERFUL = new Scheme("powerful", "LRTB", 1,
            "XHHKHHX", "KHGHGHK", "HGHKHGH", "RKKCKKR", "HGHKHGH", "KHGHGHK", "XHHKHHX");
    /** Era 5, 7 x 7: quad low enriched assemblies. */
    public static final Scheme QUAD = new Scheme("quad", "LRTB", 1,
            "QGQ.QGQ", "KQKQKQK", "QGQCQGQ", "GQK.KQG", "QGQCQGQ", "KQKQKQK", "QGQ.QGQ");
    /** Era 5, 7 x 7: high enriched rods round four channels, for tritium and the isotope blanks. */
    public static final Scheme BREEDER = new Scheme("breeder", "LRTB", 1,
            "UGUKUGU", "KcUUUcK", "UUKXKUU", "GCUKUCG", "UUKXKUU", "KcUUUcK", "UGUKUGU");
    /**
     * Era 5 to 6, 7 x 7: high enriched quads with the plant's own fuels, MOX and low enriched, each under a quarter of the
     * rods: what the radiochemical plant gives back from the spent rods goes into the reactor again.
     */
    public static final Scheme CLOSED = new Scheme("closed", "LRTB", 1,
            "UKUCUKU", ".UGQGU.", "UXYKYXU", "KK.H.KK", "UXYKYXU", ".UGQGU.", "UKUCUKU");
    /** Era 5 at its peak: quad high enriched assemblies, cryo cells everywhere. */
    public static final Scheme PEAK = new Scheme("peak", "LRTB", 1,
            "KXUGUXK", "UGUKUGU", "UUKUKUU", "KUUCUUK", "UUKUKUU", "UGUKUGU", "KXUGUXK");
    /** Era 6: heavy water in the place of graphite, the peak's power on fewer rods. */
    public static final Scheme HEAVY = new Scheme("heavy", "LRTB", 1,
            "UDUUUDU", "KUKKKUK", "KUCUCUK", "UDDRDDU", "KUCUCUK", "KUKKKUK", "UDUUUDU");
    /** Era 6: four channels behind heavy water, more power and irradiation than the era 5 breeder on fewer rods. */
    public static final Scheme HEAVY_BREEDER = new Scheme("heavy_breeder", "LRTB", 1,
            "UKHKHKU", "DUUGUUD", "cUKHKUc", "DUKCKUD", "cUKHKUc", "DUUGUUD", "UKHKHKU");

    /** In the order JEI shows them: by era, then by power. */
    public static final List<Scheme> ALL = List.of(FIRST, SINGLE, WATER, DUAL_SMALL, HIGH, DUAL, POWERFUL, QUAD, BREEDER, CLOSED,
            PEAK, HEAVY, HEAVY_BREEDER);

    /** The part the scheme puts in a cell, or null for an empty one (rows or letters left out are empty). */
    public Part part(int row, int col) {
        if (row >= rows.length || col >= rows[row].length()) return null;
        char ch = rows[row].charAt(col);
        return ch == '.' ? null : CODE.get(ch);
    }

    public boolean needs(Zone.Side side) {
        return chambers.indexOf("LRTB".charAt(side.ordinal())) >= 0;
    }

    public Zone zone() {
        Zone zone = new Zone();
        for (Zone.Side s : Zone.Side.values()) zone.setChamber(s, needs(s));
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                Part p = part(r, c);
                if (p != null) zone.set(r, c, p);
            }
        return zone;
    }

    /** One tick at the scheme's knob with water and coolant to spare: what the scheme gives. */
    public Reactor.Tick run() {
        return running().tick(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    private Reactor running() {
        Reactor r = new Reactor(zone());
        r.setKnob(knob);
        r.start();
        return r;
    }

    /** Rods the scheme burns in an hour at its knob: every fuel cell's rods over its life, at the power it runs at. */
    public double rodsPerHour() {
        Reactor r = running();
        double rods = 0;
        for (int row = 0; row < Zone.SIZE; row++)
            for (int col = 0; col < Zone.SIZE; col++) {
                Part p = r.zone().get(row, col);
                if (p != null && p.isFuel()) rods += p.rods() * TICKS_PER_HOUR / p.life();
            }
        return rods * r.power();
    }

    /** Lithium blanks the scheme's channels turn in an hour at its knob, 0 without channels. */
    public double blanksPerHour() {
        Reactor r = running();
        double seen = 0;
        for (int row = 0; row < Zone.SIZE; row++)
            for (int col = 0; col < Zone.SIZE; col++) seen += r.flux().irradiation(row, col);
        return seen * r.power() * TICKS_PER_HOUR / Fission.CHANNEL_WORK;
    }
}
