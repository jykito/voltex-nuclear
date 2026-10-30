package com.jykito.voltexnuclear.core.starforge;

/**
 * The star forge (DESIGN step 28; the author's picks on :8781/nuclear_forge.html, 2026-10-29: K-2 "temperature", the
 * threshold 8 000 "right against the port"), with no Minecraft in it.
 * <p>
 * The forge melts the star alloy: two kronium alloys of Voltex, a palladium and a cell of helium make one ingot, the empty
 * cell comes back. Its heat is the Voltex heat level in its heat ports, as the Voltex late machines read it: the heat is not
 * spent, a port cools by itself without inflow. It melts from the star heat up, faster the hotter: at {@link #STAR_HEAT} an
 * ingot takes {@link #SLOW_TICKS}, at the port's top {@link #TOP_HEAT} {@link #FAST_TICKS}. A Voltex heat pipe holds no
 * more than 4 000, so only a heat port right against the heat port of the torus or the reactor gets that hot.
 * <p>
 * The inputs go in when a melt starts; a melt that loses its heat waits, and goes on where it stopped once the heat is back.
 * Draft numbers: the balance pass (stage 14) tunes them.
 */
public final class StarForge {

    private StarForge() {}

    /** The heat the forge melts from, and the most a Voltex heat port holds. */
    public static final int STAR_HEAT = 8_000, TOP_HEAT = 10_000;
    /** Ticks an ingot takes at the star heat and at the top. */
    public static final int SLOW_TICKS = 600, FAST_TICKS = 300;
    /** What one ingot takes: kronium alloys, palladium, cells of helium. */
    public static final int KRONIUM = 2, PALLADIUM = 1, HELIUM = 1;

    /** The share of an ingot melted in a tick at this heat: none below the star heat, then from 1/600 up to 1/300. */
    public static double rate(int heat) {
        if (heat < STAR_HEAT) return 0;
        double t = Math.min(1, (double) (heat - STAR_HEAT) / (TOP_HEAT - STAR_HEAT));
        return 1.0 / (SLOW_TICKS + (FAST_TICKS - SLOW_TICKS) * t);
    }

    /** One melt at a time: its share done, 0 to 1, and whether one is under way. */
    public static final class Melt {

        private double done;
        private boolean running;

        public boolean running() {
            return running;
        }

        public double done() {
            return done;
        }

        /** Starts a melt: the caller has taken its inputs. */
        public void start() {
            running = true;
            done = 0;
        }

        /**
         * A tick at this heat. Returns true when the melt finishes in it: the caller puts out the ingot and the empty cell,
         * then may start the next one.
         */
        public boolean tick(int heat) {
            double r = rate(heat);
            if (!running || r == 0) return false;
            done += r;
            if (done < 1 - 1e-9) return false;   // 300 shares of 1/300 add up to a hair under 1
            running = false;
            done = 0;
            return true;
        }

        /** Takes a saved melt back; a broken share starts the melt over, never past its end. */
        public void load(boolean running, double done) {
            this.running = running;
            this.done = running && Double.isFinite(done) ? Math.max(0, Math.min(done, 1)) : 0;
        }
    }
}
