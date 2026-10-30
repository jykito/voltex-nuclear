package com.jykito.voltexnuclear.core.press;

/**
 * Numbers of the pellet press (DESIGN step 2), with no Minecraft in them. One enriched uranium and one empty Voltex rod
 * make one fuel rod; the press runs while it has energy for the tick and stops, keeping its progress, when it has none.
 * Draft numbers: the balance pass (stage 14) tunes them.
 */
public final class PelletPress {

    private PelletPress() {}

    /** Ticks for one rod (10 seconds). */
    public static final int WORK_TICKS = 200;
    /** FE a tick while pressing. */
    public static final int FE_PER_TICK = 40;
    /** FE the press holds; it takes as much as fits in a tick, as the Voltex machines do (their buffer is 64k too). */
    public static final int BUFFER = 64_000;

    /** FE one rod costs. */
    public static int energyPerRod() {
        return WORK_TICKS * FE_PER_TICK;
    }
}
