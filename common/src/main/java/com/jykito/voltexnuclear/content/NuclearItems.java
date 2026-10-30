package com.jykito.voltexnuclear.content;

import com.jykito.industrialcore.item.custom.CatalystItem;
import com.jykito.voltexnuclear.platform.Platform;
import com.jykito.voltexnuclear.reactor.ChannelBlankItem;
import com.jykito.voltexnuclear.reactor.FuelRodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** Items of the add-on, the block items included. */
public final class NuclearItems {

    private NuclearItems() {}

    public static Supplier<Item> URANIUM_ORE;
    public static Supplier<Item> DEEPSLATE_URANIUM_ORE;
    public static Supplier<Item> RAW_URANIUM_BLOCK;

    /** Clicks faster the closer uranium ore is, a scale over the hotbar; no energy (DESIGN step 1). The work is on the client. */
    public static Supplier<Item> GEIGER_COUNTER;

    /** Ore -> raw uranium -> the Voltex crusher (x2) -> uranium dust -> the Voltex blast furnace -> yellowcake (DESIGN step 2). */
    public static Supplier<Item> RAW_URANIUM;
    public static Supplier<Item> URANIUM_DUST;
    public static Supplier<Item> YELLOWCAKE;

    /** Yellowcake -> the centrifuge cascade -> one enriched uranium, the rest depleted (DESIGN step 3); granules. */
    public static Supplier<Item> LOW_ENRICHED_URANIUM;
    public static Supplier<Item> HIGH_ENRICHED_URANIUM;
    public static Supplier<Item> DEPLETED_URANIUM;
    /**
     * What the radiochemical plant gives besides empty rods and uranium (DESIGN steps 13 and 21): plutonium for MOX fuel,
     * palladium for the star alloy; MOX fuel is plutonium and depleted uranium sintered in the Voltex blast furnace.
     */
    public static Supplier<Item> PLUTONIUM;
    public static Supplier<Item> PALLADIUM;
    public static Supplier<Item> MOX_FUEL;

    public static Supplier<Item> NUCLEAR_CASING;
    public static Supplier<Item> CENTRIFUGE;
    public static Supplier<Item> CASCADE_CORE;
    public static Supplier<Item> PELLET_PRESS;
    public static Supplier<Item> ELECTROLYZER;
    /** The fission reactor's core and its expansion chamber; the frame has no item, it is the casing inside the reactor. */
    public static Supplier<Item> REACTOR_CORE;
    public static Supplier<Item> EXPANSION_CHAMBER;
    public static Supplier<Item> ENERGY_OUTPUT_PORT;
    public static Supplier<Item> RADIOCHEM_CORE;
    public static Supplier<Item> HOT_CELL_GLASS;
    public static Supplier<Item> COLUMN_CORE;
    public static Supplier<Item> TORUS_CORE;
    public static Supplier<Item> TORUS_COIL;
    public static Supplier<Item> STAR_FORGE_CORE;
    /**
     * The parts of the reactor core (DESIGN step 22, the author's variant C): the pressure vessel forged in the Voltex
     * blast furnace, the lithium scintillator grown in the Voltex crystallizer, the neutron detector built round it.
     */
    public static Supplier<Item> PRESSURE_VESSEL;
    public static Supplier<Item> SCINTILLATOR;
    public static Supplier<Item> NEUTRON_DETECTOR;
    /** The part of the expansion chamber (DESIGN step 22, the author's variant B), made in the Voltex rod fabricator. */
    public static Supplier<Item> CONTROL_ROD_DRIVE;

    /**
     * Fuel rods (DESIGN step 7): the single rod from the pellet press, the dual and quad assemblies from the Voltex rod
     * fabricator. Their wear lives in the item, with a bar under it ({@link FuelRodItem}).
     */
    public static Supplier<Item> LOW_ENRICHED_FUEL_ROD;
    public static Supplier<Item> LOW_ENRICHED_FUEL_ROD_DUAL;
    public static Supplier<Item> LOW_ENRICHED_FUEL_ROD_QUAD;
    public static Supplier<Item> HIGH_ENRICHED_FUEL_ROD;
    public static Supplier<Item> HIGH_ENRICHED_FUEL_ROD_DUAL;
    public static Supplier<Item> HIGH_ENRICHED_FUEL_ROD_QUAD;
    /**
     * MOX fuel rods (DESIGN step 13): plutonium and depleted uranium sintered in the Voltex blast furnace, pressed into
     * the empty rod and assembled as the uranium ones.
     */
    public static Supplier<Item> MOX_FUEL_ROD;
    public static Supplier<Item> MOX_FUEL_ROD_DUAL;
    public static Supplier<Item> MOX_FUEL_ROD_QUAD;

    /** A rod that burnt out in the reactor (DESIGN step 7); one set for every fuel, as the fuel is gone. */
    public static Supplier<Item> SPENT_FUEL_ROD;
    public static Supplier<Item> SPENT_FUEL_ROD_DUAL;
    public static Supplier<Item> SPENT_FUEL_ROD_QUAD;
    /** What a working reactor gives off, the feed the Watcher nanites are bred on (DESIGN step 10). */
    public static Supplier<Item> CORE_SAMPLE;

    /**
     * Materials of the zone parts (DESIGN steps 6 and 22, the author's variant C): boron from Voltex mineral dust in the
     * ore washer, its carbide from the blast furnace, beryllium from Voltex emerald dust in the blast furnace.
     */
    public static Supplier<Item> BORON;
    public static Supplier<Item> BORON_CARBIDE;
    public static Supplier<Item> BERYLLIUM_INGOT;

    /**
     * Components of the active zone (DESIGN step 7) the add-on brings; graphite and the heat parts are Voltex items. What
     * each one is in the reactor grid: {@link com.jykito.voltexnuclear.reactor.ReactorParts}.
     */
    public static Supplier<Item> BERYLLIUM_REFLECTOR;
    public static Supplier<Item> BORON_ABSORBER;
    public static Supplier<Item> CONTROL_ROD;
    public static Supplier<Item> CRYO_CELL;
    public static Supplier<Item> HEAVY_WATER_MODERATOR;

    /**
     * The cells (DESIGN step 20): gases stay items and go through no pipes. The lithium blank is a channel blank, it
     * stands in a cell of the zone and the flux turns it into a tritium cell for fusion (step 17, the author's variant B).
     */
    public static Supplier<Item> EMPTY_CELL;
    public static Supplier<Item> LITHIUM_BLANK;
    public static Supplier<Item> TRITIUM_CELL;
    /** Heavy water split in the electrolyzer fills an empty cell with deuterium, the other fuel of fusion (DESIGN step 13). */
    public static Supplier<Item> DEUTERIUM_CELL;
    /** A cell of helium, from the fusion torus, for the star forge (DESIGN step 19). */
    public static Supplier<Item> HELIUM_CELL;

    /**
     * The isotope pellet, the energy source inside the third power suit (DESIGN steps 12 and 14): depleted uranium pressed
     * into an empty cell is the isotope blank, the irradiation channel turns it into the pellet in 20 lithium blanks' time.
     */
    public static Supplier<Item> ISOTOPE_BLANK;
    public static Supplier<Item> ISOTOPE_PELLET;

    /**
     * The restructor's isotopes, reward B of the branch (DESIGN step 15, set I-A on 2026-10-29): each from its own blank in
     * the irradiation channel, each a Voltex catalyst for its own family of rare things made from common ones: carbon-14 the
     * ancient, gold-198 the Nether, americium-241 the End, sodium-24 the sea. A catalyst is never spent.
     */
    public static Supplier<Item> GRAPHITE_BLANK, GOLD_BLANK, PLUTONIUM_BLANK, SODIUM_BLANK;
    public static Supplier<Item> CARBON_14, GOLD_198, AMERICIUM_241, SODIUM_24;

    /** What the star forge melts (DESIGN step 28): the metal of the third power suit. */
    public static Supplier<Item> STAR_ALLOY;

    /**
     * The add-on's own parts (stage 11.5, :8781/nuclear_parts.html; the author: "not enough consumables, wiring, a
     * reinforced plate, a reflecting element"), each in a Voltex machine with little work, each out of the add-on's branches:
     * the boron alloy (depleted uranium and boron, blast furnace) rolled into the reinforced plate, palladium rolled into
     * foil, the reflector element (beryllium in photonic melt, crystallizer), the cryogenic wiring (a superconducting strand
     * in the cryo chamber), the star plate (the reinforced plate in star melt, crystallizer).
     */
    public static Supplier<Item> BORON_ALLOY, REINFORCED_PLATE, PALLADIUM_FOIL, REFLECTOR_ELEMENT, CRYO_WIRING, STAR_PLATE;

    /** The Stellar power suit, the third over Voltex's Nexite and Plasma (DESIGN step 12): each version's own armour items. */
    public static Supplier<Item> STELLAR_HELMET, STELLAR_CHESTPLATE, STELLAR_LEGGINGS, STELLAR_BOOTS;

    static void register(Platform p) {
        URANIUM_ORE = blockItem(p, "uranium_ore", NuclearBlocks.URANIUM_ORE);
        DEEPSLATE_URANIUM_ORE = blockItem(p, "deepslate_uranium_ore", NuclearBlocks.DEEPSLATE_URANIUM_ORE);
        RAW_URANIUM_BLOCK = blockItem(p, "raw_uranium_block", NuclearBlocks.RAW_URANIUM_BLOCK);
        NUCLEAR_CASING = blockItem(p, "nuclear_casing", NuclearBlocks.NUCLEAR_CASING);
        CENTRIFUGE = blockItem(p, "centrifuge", NuclearBlocks.CENTRIFUGE);
        CASCADE_CORE = blockItem(p, "cascade_core", NuclearBlocks.CASCADE_CORE);
        PELLET_PRESS = blockItem(p, "pellet_press", NuclearBlocks.PELLET_PRESS);
        ELECTROLYZER = blockItem(p, "electrolyzer", NuclearBlocks.ELECTROLYZER);
        REACTOR_CORE = blockItem(p, "reactor_core", NuclearBlocks.REACTOR_CORE);
        EXPANSION_CHAMBER = blockItem(p, "expansion_chamber", NuclearBlocks.EXPANSION_CHAMBER);
        ENERGY_OUTPUT_PORT = blockItem(p, "energy_output_port", NuclearBlocks.ENERGY_OUTPUT_PORT);
        RADIOCHEM_CORE = blockItem(p, "radiochem_core", NuclearBlocks.RADIOCHEM_CORE);
        HOT_CELL_GLASS = blockItem(p, "hot_cell_glass", NuclearBlocks.HOT_CELL_GLASS);
        COLUMN_CORE = blockItem(p, "heavy_water_column_core", NuclearBlocks.COLUMN_CORE);
        TORUS_CORE = blockItem(p, "torus_core", NuclearBlocks.TORUS_CORE);
        TORUS_COIL = blockItem(p, "torus_coil", NuclearBlocks.TORUS_COIL);
        STAR_FORGE_CORE = blockItem(p, "star_forge_core", NuclearBlocks.STAR_FORGE_CORE);
        PRESSURE_VESSEL = item(p, "pressure_vessel");
        SCINTILLATOR = item(p, "scintillator");
        NEUTRON_DETECTOR = item(p, "neutron_detector");
        CONTROL_ROD_DRIVE = item(p, "control_rod_drive");
        LOW_ENRICHED_FUEL_ROD = rod(p, "low_enriched_fuel_rod");
        LOW_ENRICHED_FUEL_ROD_DUAL = rod(p, "low_enriched_fuel_rod_dual");
        LOW_ENRICHED_FUEL_ROD_QUAD = rod(p, "low_enriched_fuel_rod_quad");
        HIGH_ENRICHED_FUEL_ROD = rod(p, "high_enriched_fuel_rod");
        HIGH_ENRICHED_FUEL_ROD_DUAL = rod(p, "high_enriched_fuel_rod_dual");
        HIGH_ENRICHED_FUEL_ROD_QUAD = rod(p, "high_enriched_fuel_rod_quad");
        MOX_FUEL_ROD = rod(p, "mox_fuel_rod");
        MOX_FUEL_ROD_DUAL = rod(p, "mox_fuel_rod_dual");
        MOX_FUEL_ROD_QUAD = rod(p, "mox_fuel_rod_quad");
        SPENT_FUEL_ROD = item(p, "spent_fuel_rod");
        SPENT_FUEL_ROD_DUAL = item(p, "spent_fuel_rod_dual");
        SPENT_FUEL_ROD_QUAD = item(p, "spent_fuel_rod_quad");
        CORE_SAMPLE = item(p, "core_sample");
        BORON = item(p, "boron");
        BORON_CARBIDE = item(p, "boron_carbide");
        BERYLLIUM_INGOT = item(p, "beryllium_ingot");
        BERYLLIUM_REFLECTOR = item(p, "beryllium_reflector");
        BORON_ABSORBER = item(p, "boron_absorber");
        CONTROL_ROD = item(p, "control_rod");
        CRYO_CELL = item(p, "cryo_cell");
        HEAVY_WATER_MODERATOR = item(p, "heavy_water_moderator");
        EMPTY_CELL = item(p, "empty_cell");
        LITHIUM_BLANK = p.register(Registries.ITEM, "lithium_blank", () -> new ChannelBlankItem(new Item.Properties(), ChannelBlankItem.TRITIUM));
        TRITIUM_CELL = item(p, "tritium_cell");
        DEUTERIUM_CELL = item(p, "deuterium_cell");
        HELIUM_CELL = item(p, "helium_cell");
        ISOTOPE_BLANK = p.register(Registries.ITEM, "isotope_blank", () -> new ChannelBlankItem(new Item.Properties(), ChannelBlankItem.ISOTOPE));
        ISOTOPE_PELLET = item(p, "isotope_pellet");
        GRAPHITE_BLANK = blank(p, "graphite_blank", ChannelBlankItem.GRAPHITE);
        GOLD_BLANK = blank(p, "gold_blank", ChannelBlankItem.GOLD);
        PLUTONIUM_BLANK = blank(p, "plutonium_blank", ChannelBlankItem.PLUTONIUM);
        SODIUM_BLANK = blank(p, "sodium_blank", ChannelBlankItem.SODIUM);
        CARBON_14 = isotope(p, "carbon_14");
        GOLD_198 = isotope(p, "gold_198");
        AMERICIUM_241 = isotope(p, "americium_241");
        SODIUM_24 = isotope(p, "sodium_24");
        STAR_ALLOY = item(p, "star_alloy");
        BORON_ALLOY = item(p, "boron_alloy");
        REINFORCED_PLATE = item(p, "reinforced_plate");
        PALLADIUM_FOIL = item(p, "palladium_foil");
        REFLECTOR_ELEMENT = item(p, "reflector_element");
        CRYO_WIRING = item(p, "cryo_wiring");
        STAR_PLATE = item(p, "star_plate");
        STELLAR_HELMET = p.register(Registries.ITEM, "stellar_helmet", () -> p.stellarArmor(ArmorItem.Type.HELMET));
        STELLAR_CHESTPLATE = p.register(Registries.ITEM, "stellar_chestplate", () -> p.stellarArmor(ArmorItem.Type.CHESTPLATE));
        STELLAR_LEGGINGS = p.register(Registries.ITEM, "stellar_leggings", () -> p.stellarArmor(ArmorItem.Type.LEGGINGS));
        STELLAR_BOOTS = p.register(Registries.ITEM, "stellar_boots", () -> p.stellarArmor(ArmorItem.Type.BOOTS));
        LOW_ENRICHED_URANIUM = item(p, "low_enriched_uranium");
        HIGH_ENRICHED_URANIUM = item(p, "high_enriched_uranium");
        DEPLETED_URANIUM = item(p, "depleted_uranium");
        PLUTONIUM = item(p, "plutonium");
        PALLADIUM = item(p, "palladium");
        MOX_FUEL = item(p, "mox_fuel");

        GEIGER_COUNTER = p.register(Registries.ITEM, "geiger_counter", () -> new Item(new Item.Properties().stacksTo(1)));
        RAW_URANIUM = item(p, "raw_uranium");
        URANIUM_DUST = item(p, "uranium_dust");
        YELLOWCAKE = item(p, "yellowcake");
    }

    /** The four pieces of the Stellar suit, for the versions' capability and client hooks. */
    public static Item[] stellarPieces() {
        return new Item[]{STELLAR_HELMET.get(), STELLAR_CHESTPLATE.get(), STELLAR_LEGGINGS.get(), STELLAR_BOOTS.get()};
    }

    private static Supplier<Item> item(Platform p, String name) {
        return p.register(Registries.ITEM, name, () -> new Item(new Item.Properties()));
    }

    private static Supplier<Item> blank(Platform p, String name, int bar) {
        return p.register(Registries.ITEM, name, () -> new ChannelBlankItem(new Item.Properties(), bar));
    }

    /** A Voltex restructor catalyst; all four share one tooltip line, what each makes is JEI's to show. */
    private static Supplier<Item> isotope(Platform p, String name) {
        return p.register(Registries.ITEM, name, () -> new CatalystItem("tooltip.voltex_nuclear.isotope_catalyst"));
    }

    private static Supplier<Item> rod(Platform p, String name) {
        return p.register(Registries.ITEM, name, () -> new FuelRodItem(new Item.Properties()));
    }

    private static Supplier<Item> blockItem(Platform p, String name, Supplier<Block> block) {
        return p.register(Registries.ITEM, name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
