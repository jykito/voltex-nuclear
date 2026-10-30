package com.jykito.voltexnuclear.core.fusion;

import com.jykito.voltexnuclear.core.reactor.Watcher;

import java.util.function.DoubleSupplier;

/**
 * The fusion torus at work, tick by tick (DESIGN step 19, numbers in {@link Fusion}), with no Minecraft in it; the torus
 * block (stage 10) puts the cells and the condensate in, takes the helium cells and the energy out and shares the output
 * into heat. It follows the torus mock-up (tools/texture_viewer/nuclear_torus.html in the Voltex workshop).
 * <p>
 * While started, the torus ignites by itself when it has a cell of each fuel, condensate for the whole ignition, the tritium share
 * in the window and a full reserve: {@link Fusion#IGNITION} FE over {@link Fusion#IGNITION_TICKS} ticks from the reserve,
 * with condensate and no fuel. Burning, it takes {@link Fusion#BURN} units of fuel a tick in the knob's mix; short of one
 * fuel the mix in the plasma moves by itself. Out of the window or out of condensate the plasma cools for
 * {@link Fusion#GRACE} ticks: back in time and it burns on, otherwise it goes out without harm and waits to ignite again.
 * <p>
 * The buffer keeps {@link Fusion#RESERVE} for the next ignition: the energy ports take only what is above it, the torus
 * charges it from its own output and an energy port charges it at up to {@link Fusion#INTAKE} a tick. Helium fills the
 * empty cells the fuel leaves; a full helium tank vents the rest and the torus burns on.
 * <p>
 * The Watcher (the reactor's nanites) widens the window and adds to the output as it knows more; an unstable strain now
 * and then knocks the mix off by {@link Fusion#JITTER} for {@link Fusion#JITTER_TICKS} ticks, which near the edge of the
 * window puts the torus out.
 */
public final class Torus {

    public enum Phase { OFF, WAIT, HEAT, LIT, COOL }

    /** What keeps the torus from igniting, or what made it cool down. */
    public enum Why { NONE, NO_FUEL, NO_TRITIUM, NO_DEUTERIUM, NO_CONDENSATE, OUT_OF_WINDOW, RESERVE }

    /**
     * What one tick did: the phase it ended in, the energy the plasma gave (with the Watcher's addition), the tritium
     * share in the plasma (NaN while it does not burn) and the share of its best it gave, the deuterium and tritium burnt,
     * the condensate the coils took, the helium made and the helium vented.
     */
    public record Tick(Phase phase, double energy, double mix, double yield, double deuterium, double tritium,
                       double condensate, double helium, double vented) {}

    /** Fuel each tank must hold for an ignition: a cell, so the torus does not light on the last scraps of a stopped supply. */
    public static final double IGNITION_FUEL = Fusion.CELL;

    private double mix = Fusion.BEST_MIX;
    private boolean running;
    private Phase phase = Phase.OFF;
    private Why why = Why.NONE;
    private int heating, cooling, jitterLeft;
    private double jitter;
    private double deuterium, tritium, condensate, helium, buffer;

    // ---- the controls of the window

    /** The tritium share knob, 0 to 1. */
    public void setMix(double mix) {
        this.mix = Double.isNaN(mix) ? Fusion.BEST_MIX : Fusion.clamp(mix);
    }

    public double mix() {
        return mix;
    }

    /** «Start»: the torus ignites by itself whenever it has all it needs. */
    public void start() {
        running = true;
    }

    /** «Stop»: the plasma goes out at once; an ignition under way is lost. */
    public void stop() {
        running = false;
        phase = Phase.OFF;
        heating = 0;
        cooling = 0;
    }

    public boolean running() {
        return running;
    }

    /** The torus has come apart: the plasma goes out at once, «Start» stays on and it ignites again once it stands. */
    public void quench() {
        phase = running ? Phase.WAIT : Phase.OFF;
        heating = 0;
        cooling = 0;
    }

    // ---- what the ports put in and take out

    /** Empties a deuterium cell into its tank if the cell fits; the empty cell is the block's to send out. */
    public boolean fillDeuterium() {
        if (deuterium > Fusion.FUEL_TANK - Fusion.CELL) return false;
        deuterium += Fusion.CELL;
        return true;
    }

    /** Empties a tritium cell into its tank if the cell fits. */
    public boolean fillTritium() {
        if (tritium > Fusion.FUEL_TANK - Fusion.CELL) return false;
        tritium += Fusion.CELL;
        return true;
    }

    /** Takes what fits of {@code mb} of condensate; returns what it took. */
    public double addCondensate(double mb) {
        double took = Math.min(positive(mb), Fusion.CONDENSATE_TANK - condensate);
        condensate += took;
        return took;
    }

    /** Fills an empty cell with helium if the tank holds a cell's worth; the block gives the cell and sends it out. */
    public boolean takeHelium() {
        if (helium < Fusion.CELL) return false;
        helium -= Fusion.CELL;
        return true;
    }

    /** An energy port charging the reserve: takes up to {@link Fusion#INTAKE} while the buffer is below the reserve. */
    public double charge(double offered) {
        double took = Math.max(0, Math.min(Math.min(positive(offered), Fusion.INTAKE), Fusion.RESERVE - buffer));
        buffer += took;
        return took;
    }

    /** Puts the torus's own energy into the buffer; returns what a full buffer could not take, which is lost. */
    public double store(double fe) {
        buffer += positive(fe);
        double lost = Math.max(0, buffer - Fusion.BUFFER);
        buffer -= lost;
        return lost;
    }

    /** The energy ports taking up to {@code max}: only what is above the reserve; returns what they got. */
    public double drain(double max) {
        double given = Math.min(Math.max(0, buffer - Fusion.RESERVE), positive(max));
        buffer -= given;
        return given;
    }

    // ---- the tick

    /**
     * One tick. {@code knowledge} and {@code stability} are those of the Watcher strain in the slot, 0 to 1 (no strain:
     * 0 and 1); {@code random} gives numbers in [0, 1) for its slips.
     */
    public Tick tick(double knowledge, double stability, DoubleSupplier random) {
        double k = Fusion.clamp(knowledge), s = Double.isNaN(stability) ? 1 : Fusion.clamp(stability);
        if (jitterLeft > 0 && --jitterLeft == 0) jitter = 0;
        if (phase == Phase.LIT && jitterLeft == 0 && random.getAsDouble() < (1 - s) * Fusion.JITTER_CHANCE) {
            jitter = (random.getAsDouble() < 0.5 ? -1 : 1) * Fusion.JITTER;
            jitterLeft = Fusion.JITTER_TICKS;
        }
        double want = fed();

        if (!running) phase = Phase.OFF;
        else if (phase == Phase.OFF) phase = Phase.WAIT;

        double condensateUsed = 0;
        if (phase == Phase.WAIT) {
            why = lack(want, k, Fusion.CONDENSATE * Fusion.IGNITION_TICKS);
            if (why == Why.NONE && buffer < Fusion.IGNITION) why = Why.RESERVE;
            if (why == Why.NONE) {
                phase = Phase.HEAT;
                heating = 0;
            }
        }
        if (phase == Phase.HEAT) {
            // fuel or cold gone, or the knob out of the window: the ignition stops and what it took is lost
            Why r = lack(want, k, Fusion.CONDENSATE);
            if (r != Why.NONE) {
                phase = Phase.WAIT;
                why = r;
            } else {
                buffer -= Fusion.IGNITION / Fusion.IGNITION_TICKS;
                condensate -= Fusion.CONDENSATE;
                condensateUsed += Fusion.CONDENSATE;
                if (++heating >= Fusion.IGNITION_TICKS) {
                    phase = Phase.LIT;
                    heating = 0;
                }
            }
        }

        double energy = 0, x = Double.NaN, f = 0, gotD = 0, gotT = 0, made = 0, vented = 0;
        if (phase == Phase.LIT || phase == Phase.COOL) {
            double needD = Fusion.BURN * (1 - want), needT = Fusion.BURN * want;
            double d = Math.min(needD, deuterium), t = Math.min(needT, tritium), got = d + t;
            boolean cold = condensate >= Fusion.CONDENSATE;
            x = got > 0 ? t / got : Double.NaN;   // short of one fuel, the mix in the plasma moves by itself
            f = got > 0 && cold ? Fusion.yieldAt(x, k) : 0;
            if (f > 0) {
                phase = Phase.LIT;
                why = Why.NONE;
                cooling = 0;
                deuterium -= d;
                tritium -= t;
                gotD = d;
                gotT = t;
                condensate -= Fusion.CONDENSATE;
                condensateUsed += Fusion.CONDENSATE;
                made = got * Fusion.HELIUM_PER_FUEL;
                energy = Fusion.FE_BEST * f * (got / Fusion.BURN) * Watcher.bonus(k);
            } else {
                if (phase == Phase.LIT) {
                    phase = Phase.COOL;
                    cooling = 0;
                    why = !cold ? Why.NO_CONDENSATE : got <= 0 ? Why.NO_FUEL : t < needT ? Why.NO_TRITIUM
                            : d < needD ? Why.NO_DEUTERIUM : Why.OUT_OF_WINDOW;
                }
                double c = Math.min(condensate, Fusion.CONDENSATE);
                condensate -= c;
                condensateUsed += c;
                if (++cooling >= Fusion.GRACE) {
                    phase = Phase.WAIT;
                    cooling = 0;
                }
                x = Double.NaN;
                f = 0;
            }
        }

        helium += made;
        vented = Math.max(0, helium - Fusion.HELIUM_TANK);
        helium -= vented;
        return new Tick(phase, energy, x, f, gotD, gotT, condensateUsed, made, vented);
    }

    /** What keeps the torus from igniting or heating, with the condensate it needs for that. */
    private Why lack(double want, double knowledge, double cold) {
        boolean d = deuterium >= IGNITION_FUEL, t = tritium >= IGNITION_FUEL;
        if (!d && !t) return Why.NO_FUEL;
        if (!t) return Why.NO_TRITIUM;
        if (!d) return Why.NO_DEUTERIUM;
        if (condensate < cold) return Why.NO_CONDENSATE;
        if (Fusion.yieldAt(want, knowledge) <= 0) return Why.OUT_OF_WINDOW;
        return Why.NONE;
    }

    // ---- what the window shows and the block saves

    public Phase phase() {
        return phase;
    }

    public Why why() {
        return why;
    }

    /** The tritium share the torus is fed now: the knob, knocked off by an unstable Watcher. */
    public double fed() {
        return Fusion.clamp(mix + jitter);
    }

    /** Ticks of the ignition under way. */
    public int heating() {
        return heating;
    }

    /** Ticks the plasma has been cooling. */
    public int cooling() {
        return cooling;
    }

    public double jitter() {
        return jitter;
    }

    public int jitterLeft() {
        return jitterLeft;
    }

    public double deuterium() {
        return deuterium;
    }

    public double tritium() {
        return tritium;
    }

    public double condensate() {
        return condensate;
    }

    public double helium() {
        return helium;
    }

    public double buffer() {
        return buffer;
    }

    /** Puts back the tanks and the buffer the block saved; a value out of range is clamped. */
    public void restoreTanks(double deuterium, double tritium, double condensate, double helium, double buffer) {
        this.deuterium = within(deuterium, Fusion.FUEL_TANK);
        this.tritium = within(tritium, Fusion.FUEL_TANK);
        this.condensate = within(condensate, Fusion.CONDENSATE_TANK);
        this.helium = within(helium, Fusion.HELIUM_TANK);
        this.buffer = within(buffer, Fusion.BUFFER);
    }

    /** Puts back the run the block saved: whether it runs, the phase, why it waits or cools, the ticks and the Watcher's knock. */
    public void restoreRun(boolean running, Phase phase, Why why, int heating, int cooling, double jitter, int jitterLeft) {
        this.running = running;
        this.phase = !running || phase == null ? Phase.OFF : phase;
        this.why = why == null ? Why.NONE : why;
        this.heating = this.phase == Phase.HEAT ? Math.max(0, Math.min(heating, Fusion.IGNITION_TICKS - 1)) : 0;
        this.cooling = this.phase == Phase.COOL ? Math.max(0, Math.min(cooling, Fusion.GRACE - 1)) : 0;
        this.jitterLeft = Math.max(0, Math.min(jitterLeft, Fusion.JITTER_TICKS));
        this.jitter = this.jitterLeft > 0 && !Double.isNaN(jitter) ? Math.max(-Fusion.JITTER, Math.min(Fusion.JITTER, jitter)) : 0;
    }

    private static double within(double v, double max) {
        return Double.isNaN(v) ? 0 : Math.max(0, Math.min(v, max));
    }

    private static double positive(double v) {
        return Double.isNaN(v) ? 0 : Math.max(0, v);
    }
}
