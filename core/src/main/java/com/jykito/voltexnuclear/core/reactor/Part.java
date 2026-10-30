package com.jykito.voltexnuclear.core.reactor;

/**
 * What can stand in a cell of the active zone (DESIGN step 7): a fuel rod of some fuel as a single, dual or quad
 * assembly, a channel blank with the irradiation it takes to turn ({@code work}), an outlet with the way it cools, or one
 * of the other components. The other parts are constants; a fuel part comes from {@link #fuel}, a channel from
 * {@link #channel}, an outlet from {@link #outlet}.
 */
public record Part(Kind kind, Fuel fuel, int rods, double work, Cooling cooling) {

    public enum Kind {
        FUEL, GRAPHITE, HEAVY_WATER, REFLECTOR, ABSORBER, CONTROL_ROD, CHANNEL, HEAT_SINK, HEAT_EXCHANGER, WATER_CELL, CRYO_CELL;

        /** Takes heat out of the cells next to it: a heat sink or a cooling cell. */
        public boolean outlet() {
            return this == HEAT_SINK || this == WATER_CELL || this == CRYO_CELL;
        }
    }

    public static final Part GRAPHITE = of(Kind.GRAPHITE), HEAVY_WATER = of(Kind.HEAVY_WATER), REFLECTOR = of(Kind.REFLECTOR),
            ABSORBER = of(Kind.ABSORBER), CONTROL_ROD = of(Kind.CONTROL_ROD), CHANNEL = channel(Fission.CHANNEL_WORK),
            HEAT_SINK = outlet(Cooling.SINK), HEAT_EXCHANGER = of(Kind.HEAT_EXCHANGER), WATER_CELL = outlet(Cooling.WATER_CELL),
            CRYO_CELL = outlet(Cooling.CRYO_CELL);

    public Part {
        if ((kind == Kind.FUEL) != (fuel != null)) throw new IllegalArgumentException("only a fuel part has a fuel");
        if (kind == Kind.FUEL && rods != 1 && rods != 2 && rods != 4) throw new IllegalArgumentException("rods: 1, 2 or 4");
        if ((kind == Kind.CHANNEL) != (work > 0)) throw new IllegalArgumentException("only a channel has work, and it is above 0");
        if (kind.outlet() != (cooling != null) || cooling != null && cooling.kind() != kind)
            throw new IllegalArgumentException("an outlet, and only an outlet, has a cooling of its own kind");
    }

    private static Part of(Kind kind) {
        return new Part(kind, null, 0, 0, null);
    }

    /** A fuel rod: 1 for a single rod, 2 for a dual assembly, 4 for a quad one. */
    public static Part fuel(Fuel fuel, int rods) {
        return new Part(Kind.FUEL, fuel, rods, 0, null);
    }

    /** A channel blank that turns into its product once it has gathered this much irradiation (the lithium blank: {@link #CHANNEL}). */
    public static Part channel(double work) {
        return new Part(Kind.CHANNEL, null, 0, work, null);
    }

    /** An outlet that cools this way: a heat sink, a water cell or a cryo cell by what it drinks and gives back. */
    public static Part outlet(Cooling cooling) {
        return new Part(cooling.kind(), null, 0, 0, cooling);
    }

    public boolean isFuel() {
        return kind == Kind.FUEL;
    }

    /** Ticks a fuel part lasts at full power: its fuel's life, longer for a bundle ({@link Fission#BUNDLE_LIFE}); 0 for the other parts. */
    public double life() {
        return isFuel() ? fuel.life * (1 + Fission.BUNDLE_LIFE * (rods - 1)) : 0;
    }

    /** The neutron source of the cell: its rods times the strength of its fuel; nothing for the other parts. */
    public double source() {
        return isFuel() ? rods * fuel.strength : 0;
    }

    /** A moderator's shares {other fuel around it, the cell's own source}, or null for a part that is not one. */
    double[] moderation() {
        return switch (kind) {
            case GRAPHITE -> new double[]{Fission.GRAPHITE_OTHERS, Fission.GRAPHITE_SELF};
            case HEAVY_WATER -> new double[]{Fission.HEAVY_WATER_OTHERS, Fission.HEAVY_WATER_SELF};
            default -> null;
        };
    }
}
