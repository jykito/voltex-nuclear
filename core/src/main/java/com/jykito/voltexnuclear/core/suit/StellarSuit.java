package com.jykito.voltexnuclear.core.suit;

/**
 * The Stellar power suit, the third over Voltex's Nexite and Plasma (DESIGN step 12, the author's picks on
 * :8781/nuclear_suit3.html: the set "Stellar", the abilities U-A "Star within", the pellet T-1). Voltex's suits spend
 * energy only on the damage they take, so this one has abilities that burn energy by themselves, and an isotope pellet
 * inside feeds them.
 * <p>
 * Each of the four pieces holds its own energy. What comes in is shared over the pieces by their room, what goes out is
 * taken from them evenly, as far as each has. The numbers are drafts until the balance stage.
 */
public final class StellarSuit {

    private StellarSuit() {}

    public static final int PIECES = 4;
    /** Energy a piece holds: four times Plasma's. */
    public static final int CAPACITY = 4_000_000;
    /** Damage turned away costs this much a health point, half of Plasma's; each charged piece takes a quarter of it. */
    public static final int FE_PER_HEALTH = 50;
    /** The Halo: half a heart every 2 seconds while the wearer is hurt, at 20 000 FE a health point. */
    public static final int HEAL_EVERY = 40, FE_PER_HEAL = 20_000;
    public static final float HEAL = 1f;
    /** The star's heat: fire and lava do not burn; in lava the suit spends this a tick. */
    public static final int LAVA_PER_TICK = 1_000;
    /** Sprinting in flight the suit flies twice as fast and spends this a tick. */
    public static final int RUSH_PER_TICK = 2_000;
    public static final float RUSH_SPEED = 2f;
    /** In flight with no key held the suit stops at once (the author: "fast flight and inertia damping"); it spends this a tick of flight. */
    public static final int DAMPING_PER_TICK = 500;
    /** The isotope pellet: an hour at 2 000 FE a tick over the four pieces, taken when the suit is under a quarter. */
    public static final int PELLET_TICKS = 72_000, PELLET_PER_TICK = 2_000;
    public static final double TAKE_BELOW = 0.25;

    /** Whether the chestplate takes the next pellet: none burning and the whole suit under a quarter. */
    public static boolean wantsPellet(long stored, long capacity, int burnLeft) {
        return burnLeft <= 0 && capacity > 0 && stored < capacity * TAKE_BELOW;
    }

    /** The share of damage the suit turns away: a quarter for each piece that holds enough to pay for its part. */
    public static float absorbed(int chargedPieces) {
        return Math.max(0, Math.min(PIECES, chargedPieces)) / (float) PIECES;
    }

    /** What the damage the suit turns away costs, the whole of it over the pieces. */
    public static int damageCost(float damage) {
        return (int) Math.ceil(Math.max(0, damage) * FE_PER_HEALTH);
    }

    /**
     * Shares {@code amount} over pieces with {@code room} left in each: evenly first, then what a full piece could not
     * take goes to those still with room. Returns what each piece gets; it sums to the amount or to all the room.
     */
    public static int[] share(int amount, int[] room) {
        int[] got = new int[room.length];
        int left = Math.max(0, amount);
        while (left > 0) {
            int open = 0;
            for (int i = 0; i < room.length; i++) if (room[i] - got[i] > 0) open++;
            if (open == 0) break;
            int each = Math.max(1, left / open);
            for (int i = 0; i < room.length && left > 0; i++) {
                int give = Math.min(Math.min(each, left), room[i] - got[i]);
                if (give <= 0) continue;
                got[i] += give;
                left -= give;
            }
        }
        return got;
    }

    /**
     * Takes {@code amount} from pieces holding {@code stored} each: evenly first, then what an empty piece could not give
     * from those that still hold some. Returns what each piece gives; it sums to the amount or to all there is.
     */
    public static int[] drain(int amount, int[] stored) {
        return share(amount, stored);
    }

    /** Whether the suit can pay {@code amount} from all its pieces together. */
    public static boolean canPay(int amount, int[] stored) {
        long sum = 0;
        for (int s : stored) sum += s;
        return sum >= amount;
    }
}
