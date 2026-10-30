package com.jykito.voltexnuclear.core.fusion;

/**
 * Numbers and formulas of the fusion torus (DESIGN steps 11, 19 and 23), with no Minecraft in them. They are the numbers
 * of the torus mock-up (tools/texture_viewer/nuclear_torus.html in the Voltex workshop).
 * <p>
 * The working window of the plasma is the share of tritium in the mix: the torus burns best at {@link #BEST_MIX}, gives
 * {@link #EDGE_YIELD} of its best at the edge of the window and nothing beyond it. The Watcher's knowledge widens the
 * window. Draft numbers: the balance pass (stage 14) tunes them.
 */
public final class Fusion {

    private Fusion() {}

    // ---- output and the window of the plasma

    /** FE a tick at the best mix with no Watcher; a knowing one brings it to about 300M, the fusion ceiling of step 8. */
    public static final double FE_BEST = 260_000_000;
    /** The tritium share the torus burns best at. */
    public static final double BEST_MIX = 0.5;
    /** Half the width of the window around the best mix, and what a fully knowing Watcher adds to it on each side. */
    public static final double HALF_WINDOW = 0.25, WATCHER_HALF = 0.10;
    /** At the edge of the window the torus gives this share of its best. */
    public static final double EDGE_YIELD = 0.5;

    // ---- fuel, condensate and helium

    /** Fuel units burnt a tick, deuterium and tritium together. */
    public static final double BURN = 4;
    /** Fuel units in one cell, and the units each fuel tank holds: eight cells. */
    public static final int CELL = 1_000, FUEL_TANK = 8_000;
    /** Ticks between the cells an input port puts into the tanks, and the helium cells filled from the tank. */
    public static final int CELL_EVERY = 20;
    /** Superfluid condensate the coils take a tick, mB (5 mB a second), and the condensate the torus holds. */
    public static final double CONDENSATE = 0.25;
    public static final int CONDENSATE_TANK = 16_000;
    /** Helium a fuel unit burnt gives: one helium from every deuterium and tritium pair; the helium the torus holds. */
    public static final double HELIUM_PER_FUEL = 0.5;
    public static final int HELIUM_TANK = 4_000;

    // ---- ignition and cooling

    /** FE one ignition takes, and its ticks (10 s, so 5M FE a tick while it heats). */
    public static final double IGNITION = 1_000_000_000;
    public static final int IGNITION_TICKS = 200;
    /** Ticks the plasma takes to cool down (5 s): back in the window in time and it burns on, otherwise it goes out. */
    public static final int GRACE = 100;

    // ---- the buffer

    /** FE the torus keeps for the next ignition; the energy ports take only what is above it. */
    public static final double RESERVE = IGNITION;
    /** The whole buffer, under the Forge energy limit; what a full buffer cannot take is lost. */
    public static final double BUFFER = 2_000_000_000;
    /** The most an energy port charges the reserve a tick: as fast as a Voltex T3 energy bank gives. */
    public static final double INTAKE = 100_000_000;

    // ---- the Watcher (step 19: the same nanites as in the reactor)

    /** How far an unstable strain knocks the mix off, for how many ticks, and its chance a burning tick at no stability. */
    public static final double JITTER = 0.15;
    public static final int JITTER_TICKS = 120;
    public static final double JITTER_CHANCE = 0.0004;

    /** Half the width of the window with a Watcher of this knowledge, 0 to 1. */
    public static double halfWindow(double knowledge) {
        return HALF_WINDOW + WATCHER_HALF * clamp(knowledge);
    }

    /** The lower edge of the window: 25 % with no Watcher, 15 % with a knowing one. */
    public static double low(double knowledge) {
        return BEST_MIX - halfWindow(knowledge);
    }

    /** The upper edge of the window: 75 % with no Watcher, 85 % with a knowing one. */
    public static double high(double knowledge) {
        return BEST_MIX + halfWindow(knowledge);
    }

    /** The share of its best the torus gives at a tritium share {@code mix}: 1 at the best mix, falling to the edge, 0 beyond. */
    public static double yieldAt(double mix, double knowledge) {
        if (Double.isNaN(mix)) return 0;
        double d = Math.abs(mix - BEST_MIX) / halfWindow(knowledge);
        return d > 1 ? 0 : 1 - EDGE_YIELD * d * d;
    }

    /** Cells a minute that a burn of this many units a tick takes. */
    public static double cellsPerMinute(double unitsPerTick) {
        return unitsPerTick * 1_200 / CELL;
    }

    static double clamp(double v) {
        return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
    }
}
