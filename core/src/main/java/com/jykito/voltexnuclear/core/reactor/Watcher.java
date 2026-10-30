package com.jykito.voltexnuclear.core.reactor;

import java.util.function.DoubleSupplier;

/**
 * The Watcher nanites in the reactor's slot (DESIGN step 10): they ease the power knob off before the hull SCRAMs the
 * reactor, the earlier and the quicker the more they know of it, and they add to the output. An unstable strain now and
 * then slips: a false alarm (it eases off a cold reactor) or a blind spell (it misses the heat). The numbers only; the
 * block gives it the strain's knowledge and stability, the hull and a random source, and caps the knob with the answer.
 */
public final class Watcher {

    /** The share of the hull's capacity the Watcher starts easing off at: knowing nothing, and knowing the reactor fully. */
    public static final double START_NONE = 0.9, START_FULL = 0.6;
    /** How far the cap moves a tick, knowing nothing and knowing fully: 4 % and 20 % a second. */
    public static final double RATE_NONE = 0.002, RATE_FULL = 0.01;
    /** The output added at full knowledge (draft, step 10). */
    public static final double BONUS_FULL = 0.15;
    /** Ticks between the looks at the strain's stability; the chance of a slip a look at no stability; how long a slip lasts. */
    public static final int CHECK_EVERY = 1200, SLIP_TICKS = 600;
    public static final double SLIP_CHANCE = 0.10;
    /** What a false alarm caps the knob at. */
    public static final double FALSE_ALARM_CAP = 0.5;

    public enum Slip { NONE, FALSE_ALARM, BLIND }

    private double cap = 1;
    private int clock, slipLeft;
    private Slip slip = Slip.NONE;

    /**
     * One tick. {@code knowledge} and {@code stability} are the strain's, 0 to 1; {@code hull} the hull's heat as a share
     * of its capacity; {@code random} gives numbers in [0, 1) for the slips. Returns the cap on the power knob, 0 to 1.
     */
    public double tick(double knowledge, double stability, double hull, DoubleSupplier random) {
        double k = clamp(knowledge), s = clamp(stability);
        if (++clock >= CHECK_EVERY) {
            clock = 0;
            if (slip == Slip.NONE && random.getAsDouble() < (1 - s) * SLIP_CHANCE) {
                slip = random.getAsDouble() < 0.5 ? Slip.FALSE_ALARM : Slip.BLIND;
                slipLeft = SLIP_TICKS;
            }
        }
        double target = 1;
        if (slip != Slip.BLIND) {
            double start = START_NONE - (START_NONE - START_FULL) * k;
            if (hull > start) target = clamp(1 - (hull - start) / (1 - start));
            if (slip == Slip.FALSE_ALARM) target = Math.min(target, FALSE_ALARM_CAP);
        }
        double rate = RATE_NONE + (RATE_FULL - RATE_NONE) * k;
        cap += Math.max(-rate, Math.min(rate, target - cap));
        if (slip != Slip.NONE && --slipLeft <= 0) slip = Slip.NONE;
        return cap;
    }

    /** The output with the Watcher's addition: 1 knowing nothing, up to 1 + {@link #BONUS_FULL}. */
    public static double bonus(double knowledge) {
        return 1 + BONUS_FULL * clamp(knowledge);
    }

    /** No strain in the slot: the cap lets go at once and the clock starts over. */
    public void reset() {
        cap = 1;
        clock = 0;
        slip = Slip.NONE;
        slipLeft = 0;
    }

    public double cap() {
        return cap;
    }

    public Slip slip() {
        return slip;
    }

    public int clock() {
        return clock;
    }

    public int slipLeft() {
        return slipLeft;
    }

    /** Puts back what the block saved. */
    public void restore(double cap, int clock, Slip slip, int slipLeft) {
        this.cap = Double.isNaN(cap) ? 1 : clamp(cap);
        this.clock = Math.max(0, Math.min(clock, CHECK_EVERY - 1));
        this.slip = slip == null ? Slip.NONE : slip;
        this.slipLeft = this.slip == Slip.NONE ? 0 : Math.max(1, Math.min(slipLeft, SLIP_TICKS));
    }

    private static double clamp(double v) {
        return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
    }
}
