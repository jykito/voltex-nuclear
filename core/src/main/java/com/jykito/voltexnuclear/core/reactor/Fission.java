package com.jykito.voltexnuclear.core.reactor;

/**
 * Numbers of the fission reactor (DESIGN steps 4 to 8 and 23), with no Minecraft in them. They began as the numbers of
 * the reactor mock-up (tools/texture_viewer/nuclear_reactor.html in the Voltex workshop). The balance pass (stage 14)
 * searched the layouts on this model: era 4 (5 x 5, single rods) reaches about 240k FE/t, era 5 (7 x 7, assemblies) about
 * 2.6M in an ordered layout; the ready layouts are in {@link Scheme}.
 */
public final class Fission {

    private Fission() {}

    // ---- flux: what each neighbour of a fuel cell adds to its flux, as shares of a source (a rod times its fuel)

    /** Extra share of its own source per extra rod in a bundle: a dual rod counts 1.25 of its source, a quad 1.75. */
    public static final double BUNDLE = 0.25;
    /** A fuel neighbour gives this share of its source. */
    public static final double FUEL_NEIGHBOUR = 0.5;
    /** Graphite passes on this share of the other fuel around it ... */
    public static final double GRAPHITE_OTHERS = 0.8;
    /** ... and gives back this share of the cell's own source. */
    public static final double GRAPHITE_SELF = 0.4;
    /** Heavy water, the stronger moderator (step 22): the other fuel around it ... */
    public static final double HEAVY_WATER_OTHERS = 1.0;
    /** ... and the cell's own source. */
    public static final double HEAVY_WATER_SELF = 0.5;
    /** A reflector gives back this share of the cell's own source. */
    public static final double REFLECTOR = 0.6;
    /** An absorber, or a channel, next to fuel takes this share of its source. */
    public static final double ABSORBER = 0.25;
    /** Each control rod lets the power knob go this much lower; it does not damp its neighbours (step 17). */
    public static final double CONTROL_ROD_THROTTLE = 0.2;

    // ---- energy and heat of a fuel cell, per unit of flux at full power

    /** FE a tick per unit of flux (step 23: 3500, so the ceilings match 10 best solar panels of the era). */
    public static final double FE_PER_FLUX = 3500;
    /** Heat a tick per unit of flux, before the fuel's own heat factor. */
    public static final double HEAT_PER_FLUX = 6;

    // ---- cooling

    /** Heat a tick a heat sink takes away. */
    public static final double SINK_RATE = 80;
    /** Heat a tick a water cell takes away while it has water, which boils away: 1 mB per this much heat (step 25). */
    public static final double WATER_CELL_RATE = 150, HEAT_PER_WATER_MB = 20;
    /** Heat a tick a cryo cell takes away while it has Voltex coolant: 1 mB per this much heat (step 23). */
    public static final double CRYO_CELL_RATE = 600, HEAT_PER_COOLANT_MB = 600;
    /** Heat the hull takes before SCRAM, and the heat it loses a tick by itself. */
    public static final double HULL_CAPACITY = 20_000, HULL_COOLING = 20;

    // ---- wear and irradiation

    /** Ticks a single rod lasts at full power (one hour); at a lower power it lasts as much longer. */
    public static final double ROD_LIFE = 72_000;
    /**
     * Extra life per extra rod in a bundle, as a share of its fuel's life (stage 14): a dual assembly lasts 1.5 of it, a quad
     * 2.5. A rod in an assembly so gives more energy than a single one, and the assemblies are a step up in the fuel they
     * save as well as in power; without it a rod gave a little less in an assembly than alone.
     */
    public static final double BUNDLE_LIFE = 0.5;
    /**
     * Irradiation one product of a channel takes: the flux the channel sees times the power, summed over ticks. This is the
     * lithium blank's, about 30 seconds in a good channel.
     */
    public static final double CHANNEL_WORK = 6_000;
    /** The isotope blank's, the pellet of the third power suit (DESIGN step 14): 20 lithium blanks' worth, about 10 minutes in a good channel. */
    public static final double ISOTOPE_WORK = 20 * CHANNEL_WORK;
    /**
     * The blanks of the restructor's isotopes (DESIGN step 15): 40 lithium blanks' worth, about 20 minutes in a good channel.
     * A catalyst stays in the restructor and is never spent, so it is made once and may cost.
     */
    public static final double CATALYST_WORK = 40 * CHANNEL_WORK;
    /** Work for one core sample, the feed of the Watcher nanites (step 17): the power summed over ticks, so 6 000 ticks at full power. */
    public static final double SAMPLE_WORK = 6_000;
}
