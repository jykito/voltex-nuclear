package com.jykito.voltexnuclear.content;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fluids of the add-on, as Voltex has its own: a texture in a tint, no bucket, carried in Voltex capsules. Water-like
 * fluids take the water textures; the melts take Voltex's molten lead in grey (nuclear_textures/fluids.js), so the tint
 * colours them in the world, the tanks and the capsules alike. Each one is kept here with its tint and textures, for the
 * loader that sets them up on the client apart from the fluid type.
 */
public final class NuclearFluids {

    private NuclearFluids() {}

    /** A fluid of the add-on, its tint (ARGB) and its still and flowing textures. */
    public record Tinted(Supplier<Fluid> source, int tint, ResourceLocation still, ResourceLocation flow) {}

    private static final List<Tinted> ALL = new ArrayList<>();

    /** Water grown heavy in the heavy water column (DESIGN step 13), for the electrolyzer and the moderator; a darker blue than water. */
    public static Supplier<Fluid> HEAVY_WATER;
    /** Darker than water, so it tells from water in a tank at a glance (the author: "a tint just darker than water"). */
    public static final int HEAVY_WATER_TINT = 0xFF1F3E8C;
    /**
     * The add-on's own fluids for its parts (stage 11.5, :8781/nuclear_parts.html): liquid helium out of the torus's helium
     * cells, for the star forge core and the Stellar suit; the star alloy melted, for the star plates.
     */
    public static Supplier<Fluid> LIQUID_HELIUM, STAR_MELT;
    public static final int LIQUID_HELIUM_TINT = 0xFFCFEEFF, STAR_MELT_TINT = 0xFFFFC24A;

    static void register(Platform p) {
        HEAVY_WATER = water(p, "heavy_water", HEAVY_WATER_TINT);
        LIQUID_HELIUM = melt(p, "liquid_helium", LIQUID_HELIUM_TINT);
        STAR_MELT = melt(p, "star_melt", STAR_MELT_TINT);
    }

    /** The add-on's fluids with their tints and textures. */
    public static List<Tinted> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static Supplier<Fluid> water(Platform p, String name, int tint) {
        return fluid(p, name, tint, p.id("minecraft", "block/water_still"), p.id("minecraft", "block/water_flow"));
    }

    private static Supplier<Fluid> melt(Platform p, String name, int tint) {
        return fluid(p, name, tint, VoltexNuclear.id("block/" + name + "_still"), VoltexNuclear.id("block/" + name + "_flow"));
    }

    private static Supplier<Fluid> fluid(Platform p, String name, int tint, ResourceLocation still, ResourceLocation flow) {
        Supplier<Fluid> source = p.fluid(name, tint, still, flow);
        ALL.add(new Tinted(source, tint, still, flow));
        return source;
    }
}
