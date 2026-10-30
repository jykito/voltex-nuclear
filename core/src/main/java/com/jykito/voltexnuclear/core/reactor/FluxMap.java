package com.jykito.voltexnuclear.core.reactor;

/**
 * The neutron flux of a zone (DESIGN step 4, numbers in {@link Fission}), worked out once for a layout and kept until
 * the layout changes. A fuel cell's flux is its own source, more for a bundle, plus what each of its four neighbours
 * adds: fuel half its source; a moderator a share of the other fuel around it and a share of the cell's own source; a
 * reflector a share of the cell's own source; an absorber or a channel takes a share away. A channel sees the sources
 * next to it and, through a moderator next to it, the fuel around that moderator. Control rods lower the floor of the
 * power knob only.
 */
public final class FluxMap {

    static final int[][] SIDES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final double[][] flux = new double[Zone.SIZE][Zone.SIZE], irradiation = new double[Zone.SIZE][Zone.SIZE];
    private final int controlRods;

    private FluxMap(Zone zone) {
        int controls = 0;
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                Part part = zone.get(r, c);
                if (part == null) continue;
                switch (part.kind()) {
                    case CONTROL_ROD -> controls++;
                    case CHANNEL -> irradiation[r][c] = channel(zone, r, c);
                    case FUEL -> flux[r][c] = fuel(zone, part, r, c);
                    default -> {}
                }
            }
        controlRods = controls;
    }

    public static FluxMap of(Zone zone) {
        return new FluxMap(zone);
    }

    private static double fuel(Zone zone, Part part, int r, int c) {
        double own = part.source(), f = own * (1 + Fission.BUNDLE * (part.rods() - 1));
        for (int[] d : SIDES) {
            Part n = zone.get(r + d[0], c + d[1]);
            if (n == null) continue;
            double[] mod = n.moderation();
            if (n.isFuel()) f += Fission.FUEL_NEIGHBOUR * n.source();
            else if (mod != null) f += mod[0] * around(zone, r + d[0], c + d[1], r, c) + mod[1] * own;
            else if (n.kind() == Part.Kind.REFLECTOR) f += Fission.REFLECTOR * own;
            else if (n.kind() == Part.Kind.ABSORBER || n.kind() == Part.Kind.CHANNEL) f -= Fission.ABSORBER * own;
        }
        return Math.max(0, f);
    }

    private static double channel(Zone zone, int r, int c) {
        double seen = 0;
        for (int[] d : SIDES) {
            int nr = r + d[0], nc = c + d[1];
            seen += zone.source(nr, nc);
            Part n = zone.get(nr, nc);
            double[] mod = n == null ? null : n.moderation();
            if (mod != null) seen += mod[0] * around(zone, nr, nc, r, c);
        }
        return seen;
    }

    /** The sources around cell (r, c), leaving out the cell (skipR, skipC). */
    private static double around(Zone zone, int r, int c, int skipR, int skipC) {
        double sum = 0;
        for (int[] d : SIDES) {
            int nr = r + d[0], nc = c + d[1];
            if (nr != skipR || nc != skipC) sum += zone.source(nr, nc);
        }
        return sum;
    }

    /** Flux of a fuel cell, 0 for any other. */
    public double flux(int row, int col) {
        return flux[row][col];
    }

    /** What a channel sees each tick at full power, 0 for any other cell. */
    public double irradiation(int row, int col) {
        return irradiation[row][col];
    }

    public int controlRods() {
        return controlRods;
    }

    /** The lowest the power knob goes: 1 without control rods, 20 % lower for each. */
    public double minPower() {
        return Math.max(0, 1 - Fission.CONTROL_ROD_THROTTLE * controlRods);
    }
}
