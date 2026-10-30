package com.jykito.voltexnuclear.core.radiochem;

/**
 * Numbers and rules of the radiochemical plant (DESIGN step 13, options 1B, 2B and 3B chosen by the author on 2026-10-27), with no
 * Minecraft in them.
 * <p>
 * The plant takes spent rods apart in water. Every rod gives its empty rod back; the rest comes in even shares, not by
 * chance: the plant keeps what it has gathered towards each product and hands one out whole when its share is full. An
 * assembly x2 or x4 counts as 2 or 4 rods, takes as long and as much water as they would, and gives back 2 or 4 empty
 * rods.
 */
public final class Radiochem {

    private Radiochem() {}

    /** Ticks one rod takes, the FE a tick the plant takes while it works, and the water one rod takes. */
    public static final int TICKS_PER_ROD = 200, FE_PER_TICK = 200, WATER_PER_ROD = 1_000;
    /** FE the core holds and the most it takes from its energy ports in one tick, as the cascade. */
    public static final int BUFFER = 200_000, INTAKE = 2_000;
    /** Water the plant keeps, in mB: enough for an assembly x4 several times over. */
    public static final int WATER_TANK = 16_000;

    /** What the shares give, and how many rods one of it takes. */
    public enum Product {
        LOW_ENRICHED(4), PLUTONIUM(4), DEPLETED(2), PALLADIUM(16);

        /** Rods per one of this product. */
        public final int per;

        Product(int per) {
            this.per = per;
        }
    }

    /** Ticks a part of this many rods takes. */
    public static int ticks(int rods) {
        return TICKS_PER_ROD * rods;
    }

    /** Water a part of this many rods takes, in mB. */
    public static int water(int rods) {
        return WATER_PER_ROD * rods;
    }

    /** FE one rod takes. */
    public static int energyPerRod() {
        return TICKS_PER_ROD * FE_PER_TICK;
    }

    /** What the plant has gathered towards each product, in rods; a share never reaches its whole, that has come out. */
    public static final class Shares {

        private final int[] have = new int[Product.values().length];

        /** Adds the rods of one part and returns how many of each product came out whole, by {@link Product#ordinal()}. */
        public int[] add(int rods) {
            int[] out = new int[have.length];
            for (Product p : Product.values()) {
                int sum = have[p.ordinal()] + rods;
                out[p.ordinal()] = sum / p.per;
                have[p.ordinal()] = sum % p.per;
            }
            return out;
        }

        public int get(Product p) {
            return have[p.ordinal()];
        }

        /** The shares to save, by {@link Product#ordinal()}. */
        public int[] save() {
            return have.clone();
        }

        /** Takes saved shares back; a missing or out of range one starts from nothing. */
        public void load(int[] saved) {
            for (Product p : Product.values()) {
                int v = saved != null && p.ordinal() < saved.length ? saved[p.ordinal()] : 0;
                have[p.ordinal()] = v >= 0 && v < p.per ? v : 0;
            }
        }
    }
}
