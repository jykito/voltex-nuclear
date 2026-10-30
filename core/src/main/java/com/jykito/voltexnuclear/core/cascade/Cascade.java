package com.jykito.voltexnuclear.core.cascade;

import java.util.List;

/**
 * Numbers and rules of the centrifuge cascade (DESIGN step 3), with no Minecraft in them.
 * <p>
 * A row of 2 to 12 columns enriches yellowcake. Each stage needs a number of columns for full speed; extra columns run
 * it faster, a shorter row slower: the speed is {@code columns / need}. The work of one uranium is counted in column
 * ticks ({@code need * ticks}), and every tick each column adds one, so the speed comes out exact in whole numbers.
 * Each column takes the same energy per tick, so the energy for one uranium does not depend on the length of the row.
 */
public final class Cascade {

    private Cascade() {}

    public static final int MIN_COLUMNS = 2, MAX_COLUMNS = 12;
    /** FE a tick each column takes while the cascade runs (the Voltex fluid enricher takes 50). */
    public static final int FE_PER_COLUMN = 40;
    /** FE the core holds, and the most it takes from its energy ports in one tick. */
    public static final int BUFFER = 200_000, INTAKE = 2_000;

    /** What the cascade makes; the stage is picked in the core window and counts from the next cycle. */
    public enum Stage {
        /** Low enriched uranium, the fuel of the first reactor: full speed from 4 columns, a row of 2-3 runs slower. */
        LOW(4, 4, 400),
        /** High enriched uranium, straight from yellowcake: opens at 10 columns. */
        HIGH(10, 12, 800);

        /** Columns for full speed. */
        public final int need;
        /** Yellowcake per uranium; all but one of it leaves as depleted uranium. */
        public final int feed;
        /** Ticks per uranium at full speed. */
        public final int ticks;

        Stage(int need, int feed, int ticks) {
            this.need = need;
            this.feed = feed;
            this.ticks = ticks;
        }

        /** Whether a row of this length makes this stage at all. */
        public boolean open(int columns) {
            return columns >= minColumns() && columns <= MAX_COLUMNS;
        }

        /** The shortest row that makes this stage: the low one runs slower from two columns, the high one opens at full speed. */
        public int minColumns() {
            return this == LOW ? MIN_COLUMNS : need;
        }

        /** Column ticks one uranium takes. */
        public int work() {
            return need * ticks;
        }

        /** FE one uranium takes, the same for any length of the row: every column tick costs the same. */
        public int energy() {
            return FE_PER_COLUMN * work();
        }

        /** Depleted uranium a cycle gives besides the one enriched. */
        public int depleted() {
            return feed - 1;
        }

        /** Ticks one uranium takes on a row of this length (0 when the stage is shut). */
        public int ticksAt(int columns) {
            return open(columns) ? (work() + columns - 1) / columns : 0;
        }

        /** Speed against full speed, for the window: 0.5 on two columns of the low stage, 3 on twelve. */
        public double speed(int columns) {
            return open(columns) ? (double) columns / need : 0;
        }

        public static Stage byIndex(int i) {
            Stage[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    /**
     * One cycle the cascade runs: a stage on yellowcake, or the low stage on tails (stage 14, the author chose A on
     * 2026-10-29). With no yellowcake in the input the low stage re-enriches depleted uranium: eight of it and twice as long
     * for one low enriched uranium, with nothing left over, so the depleted uranium of the reactor fuel has a way back.
     */
    public record Run(Stage stage, boolean tails) {

        public static final Run LOW = new Run(Stage.LOW, false), HIGH = new Run(Stage.HIGH, false), TAILS = new Run(Stage.LOW, true);
        /** In the order of their saved index; the first two keep the index of their stage. */
        public static final List<Run> ALL = List.of(LOW, HIGH, TAILS);
        /** Depleted uranium per uranium on tails. */
        public static final int TAILS_FEED = 8;

        /** The run of a stage on yellowcake. */
        public static Run of(Stage stage) {
            return stage == Stage.LOW ? LOW : HIGH;
        }

        public static Run byIndex(int i) {
            return ALL.get(Math.floorMod(i, ALL.size()));
        }

        public int index() {
            return ALL.indexOf(this);
        }

        /** Yellowcake, or depleted uranium on tails, for one uranium. */
        public int feed() {
            return tails ? TAILS_FEED : stage.feed;
        }

        /** Depleted uranium a cycle leaves besides the one enriched. */
        public int depleted() {
            return tails ? 0 : stage.depleted();
        }

        /** Column ticks one uranium takes: twice the low stage's on tails. */
        public int work() {
            return tails ? 2 * stage.work() : stage.work();
        }

        /** FE one uranium takes, the same for any length of the row. */
        public int energy() {
            return FE_PER_COLUMN * work();
        }

        /** Ticks one uranium takes on a row of this length (0 when the stage is shut). */
        public int ticksAt(int columns) {
            return stage.open(columns) ? (work() + columns - 1) / columns : 0;
        }
    }

    /** FE a tick the row takes while it runs. */
    public static int energyPerTick(int columns) {
        return FE_PER_COLUMN * columns;
    }
}
