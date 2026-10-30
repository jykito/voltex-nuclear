package com.jykito.voltexnuclear.content;

import com.jykito.industrialcore.api.VoltexUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets.Shared;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Everything the add-on puts into the game registries, registered once from the shared entry point. */
public final class NuclearContent {

    private NuclearContent() {}

    public static Supplier<CreativeModeTab> TAB;

    public static void register(Platform p) {
        NuclearBlocks.register(p);
        NuclearFluids.register(p);
        NuclearItems.register(p);
        NuclearBlockEntities.register(p);
        NuclearMenus.register(p);
        NuclearSounds.register(p);
        TAB = p.register(Registries.CREATIVE_MODE_TAB, "voltex_nuclear", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.voltex_nuclear"))
                .icon(() -> new ItemStack(NuclearItems.RAW_URANIUM.get()))
                .displayItems((params, output) -> tabOrder().forEach(item -> output.accept(item.get())))
                .build());
        // the press and the electrolyzer take the upgrade set of the Voltex rod fabricator; the upgrade tooltips then name them too
        for (Shared kind : List.of(Shared.ACCELERATOR, Shared.PARALLEL, Shared.ITEM_PUSH, Shared.ITEM_PULL)) {
            VoltexUpgrades.allow(kind, NuclearBlocks.PELLET_PRESS);
            VoltexUpgrades.allow(kind, NuclearBlocks.ELECTROLYZER);
        }
        NuclearNanites.register(p);
    }

    /** The creative tab follows the way of the uranium: the counter that finds it, the ores, the raw metal and its block,
     *  the dust, the yellowcake, the cascade that enriches it and what comes out, the press and the rods it makes, then
     *  the materials of the zone parts and the other components of the active zone, the parts of the reactor core and
     *  the reactor's blocks. */
    private static List<Supplier<Item>> tabOrder() {
        return List.of(NuclearItems.GEIGER_COUNTER, NuclearItems.URANIUM_ORE, NuclearItems.DEEPSLATE_URANIUM_ORE, NuclearItems.RAW_URANIUM,
                NuclearItems.RAW_URANIUM_BLOCK, NuclearItems.URANIUM_DUST, NuclearItems.YELLOWCAKE,
                NuclearItems.CASCADE_CORE, NuclearItems.CENTRIFUGE, NuclearItems.NUCLEAR_CASING,
                NuclearItems.LOW_ENRICHED_URANIUM, NuclearItems.HIGH_ENRICHED_URANIUM, NuclearItems.DEPLETED_URANIUM,
                NuclearItems.PELLET_PRESS,
                NuclearItems.LOW_ENRICHED_FUEL_ROD, NuclearItems.LOW_ENRICHED_FUEL_ROD_DUAL, NuclearItems.LOW_ENRICHED_FUEL_ROD_QUAD,
                NuclearItems.HIGH_ENRICHED_FUEL_ROD, NuclearItems.HIGH_ENRICHED_FUEL_ROD_DUAL, NuclearItems.HIGH_ENRICHED_FUEL_ROD_QUAD,
                NuclearItems.MOX_FUEL_ROD, NuclearItems.MOX_FUEL_ROD_DUAL, NuclearItems.MOX_FUEL_ROD_QUAD,
                NuclearItems.SPENT_FUEL_ROD, NuclearItems.SPENT_FUEL_ROD_DUAL, NuclearItems.SPENT_FUEL_ROD_QUAD, NuclearItems.CORE_SAMPLE,
                NuclearItems.PLUTONIUM, NuclearItems.PALLADIUM, NuclearItems.MOX_FUEL,
                NuclearItems.BORON, NuclearItems.BORON_CARBIDE, NuclearItems.BERYLLIUM_INGOT,
                NuclearItems.HEAVY_WATER_MODERATOR, NuclearItems.BERYLLIUM_REFLECTOR, NuclearItems.BORON_ABSORBER,
                NuclearItems.CONTROL_ROD, NuclearItems.CRYO_CELL,
                NuclearItems.EMPTY_CELL, NuclearItems.LITHIUM_BLANK, NuclearItems.TRITIUM_CELL,
                NuclearItems.ISOTOPE_BLANK, NuclearItems.ISOTOPE_PELLET,
                NuclearItems.GRAPHITE_BLANK, NuclearItems.CARBON_14, NuclearItems.GOLD_BLANK, NuclearItems.GOLD_198,
                NuclearItems.PLUTONIUM_BLANK, NuclearItems.AMERICIUM_241, NuclearItems.SODIUM_BLANK, NuclearItems.SODIUM_24,
                NuclearItems.DEUTERIUM_CELL, NuclearItems.HELIUM_CELL, NuclearItems.COLUMN_CORE, NuclearItems.ELECTROLYZER,
                NuclearItems.PRESSURE_VESSEL, NuclearItems.SCINTILLATOR, NuclearItems.NEUTRON_DETECTOR,
                NuclearItems.CONTROL_ROD_DRIVE, NuclearItems.REACTOR_CORE, NuclearItems.EXPANSION_CHAMBER, NuclearItems.ENERGY_OUTPUT_PORT,
                NuclearItems.RADIOCHEM_CORE, NuclearItems.HOT_CELL_GLASS, NuclearItems.TORUS_CORE, NuclearItems.TORUS_COIL,
                NuclearItems.STAR_FORGE_CORE, NuclearItems.STAR_ALLOY,
                NuclearItems.BORON_ALLOY, NuclearItems.REINFORCED_PLATE, NuclearItems.PALLADIUM_FOIL, NuclearItems.REFLECTOR_ELEMENT, NuclearItems.CRYO_WIRING, NuclearItems.STAR_PLATE,
                NuclearItems.STELLAR_HELMET, NuclearItems.STELLAR_CHESTPLATE, NuclearItems.STELLAR_LEGGINGS, NuclearItems.STELLAR_BOOTS);
    }
}
