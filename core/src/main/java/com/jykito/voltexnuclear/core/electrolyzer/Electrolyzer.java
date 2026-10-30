package com.jykito.voltexnuclear.core.electrolyzer;

/**
 * Numbers of the electrolyzer (DESIGN step 13), with no Minecraft in them. Heavy water and an empty cell make a deuterium
 * cell: the electrolyzer splits the heavy water and fills the cell with the deuterium. It takes heavy water only, so the
 * heavy water column is a must for fusion. It runs while it has energy for the tick and stops, keeping its progress,
 * when it has none, as the pellet press. Draft numbers: the balance pass (stage 14) tunes them.
 */
public final class Electrolyzer {

    private Electrolyzer() {}

    /** Heavy water one deuterium cell takes, in mB. */
    public static final int HEAVY_WATER_PER_CELL = 250;
    /** Ticks for one cell (10 seconds). */
    public static final int WORK_TICKS = 200;
    /** FE a tick while it works. */
    public static final int FE_PER_TICK = 800;
    /** FE it holds: two cells' worth. */
    public static final int BUFFER = 400_000;
    /** Heavy water it holds, in mB: 32 cells. */
    public static final int TANK = 8_000;

    /** FE one cell costs. */
    public static int energyPerCell() {
        return WORK_TICKS * FE_PER_TICK;
    }
}
