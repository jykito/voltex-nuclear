package com.jykito.voltexnuclear.core.reactor;

/**
 * How an outlet of the zone takes heat away (DESIGN steps 7, 23 and 25): up to {@code rate} heat a tick. A cell does so
 * only while it has its fluid, 1 mB per {@code heatPerMb} heat, and gives as much of {@code output} back (the cryo cell's
 * hot coolant) or nothing (the water cell's water boils away); a heat sink drinks nothing. The add-on's three are the
 * constants, a pack adds its own through the add-on's API (PLAN stage 12, step 6). Fluids go by their ids: the core has
 * no Minecraft in it.
 */
public record Cooling(double rate, String fluid, double heatPerMb, String output) {

    public static final String WATER = "minecraft:water", COOLANT = "industrial_core:coolant", HOT_COOLANT = "industrial_core:hot_coolant";

    public static final Cooling SINK = new Cooling(Fission.SINK_RATE, null, 0, null),
            WATER_CELL = new Cooling(Fission.WATER_CELL_RATE, WATER, Fission.HEAT_PER_WATER_MB, null),
            CRYO_CELL = new Cooling(Fission.CRYO_CELL_RATE, COOLANT, Fission.HEAT_PER_COOLANT_MB, HOT_COOLANT);

    public Cooling {
        if (!(rate > 0)) throw new IllegalArgumentException("cooling rate > 0");
        if (fluid == null && (heatPerMb != 0 || output != null)) throw new IllegalArgumentException("a heat sink drinks nothing and gives nothing back");
        if (fluid != null && !(heatPerMb > 0)) throw new IllegalArgumentException("a cell takes heatPerMb > 0 of its fluid");
    }

    /** Whether it needs a fluid to work: a cell. */
    public boolean drinks() {
        return fluid != null;
    }

    /** The kind of part it makes: a heat sink, a cell whose fluid boils away (as water) or one that gives it back (as coolant). */
    public Part.Kind kind() {
        return fluid == null ? Part.Kind.HEAT_SINK : output == null ? Part.Kind.WATER_CELL : Part.Kind.CRYO_CELL;
    }
}
