package com.jykito.voltexnuclear.core.column;

/**
 * Numbers and rules of the heavy water column (DESIGN step 13, the Girdler process the author chose on 2026-10-27), with
 * no Minecraft in them.
 * <p>
 * Water grows heavy between a hot and a cold part. The column is built up in tiers, from 4 to 8, as the cascade grows in
 * columns: 4 tiers already work, at half the speed of 8. The lower half of the tiers is hot, the upper half cold; with an
 * odd number the spare tier is hot. The heat is as the Voltex late machines have it: the hot half runs while the heat in
 * the heat ports is at least {@link #HEAT_THRESHOLD}, and the heat is not spent (the author chose that over a heat cost,
 * as the Voltex heat port has no way to take heat out). The cold tiers take Voltex coolant and give as much hot coolant
 * back; short of coolant the column runs slower, nothing breaks. Water goes in 50 to 1.
 * Draft numbers: the balance pass (stage 14) tunes them.
 */
public final class HeavyWaterColumn {

    private HeavyWaterColumn() {}

    /** Tiers a column can have: it forms with the first number and grows to the second. */
    public static final int MIN_TIERS = 4, MAX_TIERS = 8;
    /** Heat the heat ports must hold for the hot tiers to work: the top of the Voltex liquefier's thresholds. */
    public static final int HEAT_THRESHOLD = 1_200;
    /** FE a tick a tier takes while the column works. */
    public static final int FE_PER_TIER = 20;
    /** Coolant a cold tier takes in a tick at full speed, in mB; the same amount comes back hot. */
    public static final double COOLANT_PER_COLD_TIER = 0.25;
    /** Water a full column takes in a tick at full speed, in mB, and how many mB of water make one of heavy water. */
    public static final double WATER_FULL = 25;
    public static final int WATER_PER_HEAVY = 50;
    /** FE the core holds and the most it takes from its energy ports in one tick, as the cascade and the plant. */
    public static final int BUFFER = 200_000, INTAKE = 2_000;
    /** Water the column keeps, in mB. */
    public static final int WATER_TANK = 16_000;

    /** Hot tiers in a column of this many: the lower half, the spare one of an odd number too. */
    public static int hot(int tiers) {
        return (tiers + 1) / 2;
    }

    /** Cold tiers in a column of this many: the upper half. */
    public static int cold(int tiers) {
        return tiers - hot(tiers);
    }

    /** FE a tick the column takes while it works. */
    public static int energyPerTick(int tiers) {
        return FE_PER_TIER * tiers;
    }

    /** Coolant the column needs in a tick to run at its full speed, in mB. */
    public static double coolantNeed(int tiers) {
        return COOLANT_PER_COLD_TIER * cold(tiers);
    }

    /** The column's speed against a full one of 8 tiers when nothing is short: 4 tiers run at half. */
    public static double fullSpeed(int tiers) {
        return (double) tiers / MAX_TIERS;
    }

    /**
     * How fast the column runs, against a full one of 8 tiers: nothing below the heat threshold, otherwise its own full
     * speed slowed by the share of the coolant it needs that it gets.
     *
     * @param heat    the heat in the heat ports
     * @param coolant the coolant it can get this tick, in mB
     */
    public static double speed(int tiers, int heat, double coolant) {
        if (tiers < MIN_TIERS || heat < HEAT_THRESHOLD) return 0;
        double need = coolantNeed(tiers);
        double share = need <= 0 ? 1 : Math.max(0, Math.min(1, coolant / need));
        return fullSpeed(tiers) * share;
    }

    /** Water taken in a tick at this speed, in mB. */
    public static double water(double speed) {
        return WATER_FULL * speed;
    }

    /** Heavy water made in a tick at this speed, in mB. */
    public static double heavyWater(double speed) {
        return WATER_FULL * speed / WATER_PER_HEAVY;
    }

    /** Coolant taken (and hot coolant given back) in a tick at this speed by a column of this many tiers, in mB. */
    public static double coolant(int tiers, double speed) {
        double full = fullSpeed(tiers);
        return full <= 0 ? 0 : coolantNeed(tiers) * speed / full;
    }

    /**
     * A fluid moved in fractions of a mB a tick, as the column takes and gives it: what is owed piles up, and whole mB are
     * moved when there are some. Its rest is saved with the column.
     */
    public static final class Flow {

        private double owed;

        /** Adds what this tick owes and returns the whole mB to move now; the fraction waits for the next ticks. */
        public int add(double amount) {
            owed += Math.max(0, amount);
            int whole = (int) owed;
            owed -= whole;
            return whole;
        }

        /** Gives back mB that could not be moved, to try again later. */
        public void giveBack(int amount) {
            owed += Math.max(0, amount);
        }

        public double owed() {
            return owed;
        }

        /** Takes a saved rest back; a missing or broken one starts from nothing. */
        public void load(double saved) {
            owed = Double.isFinite(saved) && saved >= 0 ? saved : 0;
        }
    }
}
