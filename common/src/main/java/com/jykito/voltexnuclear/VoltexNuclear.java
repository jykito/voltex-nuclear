package com.jykito.voltexnuclear;

import com.jykito.industrialcore.api.VoltexApi;
import com.jykito.voltexnuclear.content.NuclearContent;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.platform.Platform;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

import java.util.Objects;

/** Shared entry point: each loader module builds its {@link Platform} and calls {@link #init} from the mod constructor. */
public final class VoltexNuclear {

    public static final Logger LOGGER = LogUtils.getLogger();

    private static Platform platform;

    private VoltexNuclear() {}

    public static void init(Platform loader) {
        platform = loader;
        LOGGER.info("{} starting on {}, built for Voltex API {}", ModInfo.NAME, loader.describe(), VoltexApi.VERSION);
        NuclearContent.register(loader);
    }

    public static Platform platform() {
        if (platform == null) throw new IllegalStateException(ModInfo.NAME + " is not initialised yet");
        return platform;
    }

    /**
     * A resource location in the add-on namespace. Built without the platform ({@code tryParse} is spelled the same on
     * both versions), so it works before {@link #init}: JEI asks for the plugin id from its own mod constructor.
     */
    public static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse(ModInfo.MOD_ID + ":" + path), path);
    }

    /** A Voltex item the add-on takes by its id (the empty rod, lithium, graphite); once the registries are filled. */
    public static Item voltexItem(String path) {
        Item item = BuiltInRegistries.ITEM.get(platform().id("industrial_core", path));
        if (item == Items.AIR) throw new IllegalStateException("Voltex has no item industrial_core:" + path);
        return item;
    }

    /** A Voltex fluid the add-on takes by its id (the coolant and the hot coolant); once the registries are filled. */
    public static Fluid voltexFluid(String path) {
        Fluid fluid = BuiltInRegistries.FLUID.get(platform().id("industrial_core", path));
        if (fluid == Fluids.EMPTY) throw new IllegalStateException("Voltex has no fluid industrial_core:" + path);
        return fluid;
    }
}
