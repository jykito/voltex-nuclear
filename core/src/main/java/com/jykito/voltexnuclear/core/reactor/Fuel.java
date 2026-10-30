package com.jykito.voltexnuclear.core.reactor;

/**
 * The fuel of a rod (DESIGN step 7): how strong a source one rod of it is, how hot it runs for its flux and how long it
 * lasts. The add-on's three fuels are the constants; a pack adds its own through the add-on's API (PLAN stage 12, step 6).
 * Two fuels are the same only when they are the same object, so a pack's fuel never stands in for the add-on's.
 */
public final class Fuel {

    public static final Fuel LOW_ENRICHED = new Fuel("low_enriched", 1, 1, Fission.ROD_LIFE),
            HIGH_ENRICHED = new Fuel("high_enriched", 2, 1.4, Fission.ROD_LIFE),
            MOX = new Fuel("mox", 1.5, 1.2, Fission.ROD_LIFE);

    /** A name for logs and tests. */
    public final String name;
    /** Source of one rod. */
    public final double strength;
    /** Heat factor: the heat of a cell is its flux times {@link Fission#HEAT_PER_FLUX} times this. */
    public final double heat;
    /** Ticks a rod of it lasts at full power; at a lower power it lasts as much longer. */
    public final double life;

    public Fuel(String name, double strength, double heat, double life) {
        if (!(strength > 0) || !(heat >= 0) || !(life >= 1)) throw new IllegalArgumentException("fuel " + name + ": strength > 0, heat >= 0, life >= 1");
        this.name = name;
        this.strength = strength;
        this.heat = heat;
        this.life = life;
    }

    @Override
    public String toString() {
        return name;
    }
}
