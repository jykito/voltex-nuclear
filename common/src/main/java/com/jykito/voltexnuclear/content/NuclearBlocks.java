package com.jykito.voltexnuclear.content;

import com.jykito.voltexnuclear.cascade.CascadeCoreBlock;
import com.jykito.voltexnuclear.cascade.CentrifugeBlock;
import com.jykito.voltexnuclear.cascade.NuclearCasingBlock;
import com.jykito.voltexnuclear.machine.EnergyOutputPortBlock;
import com.jykito.voltexnuclear.platform.Platform;
import com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity;
import com.jykito.voltexnuclear.machine.MachineBlock;
import com.jykito.voltexnuclear.press.PelletPressBlockEntity;
import com.jykito.voltexnuclear.radiochem.HotCellGlassBlock;
import com.jykito.voltexnuclear.column.ColumnCoreBlock;
import com.jykito.voltexnuclear.fusion.TorusCoilBlock;
import com.jykito.voltexnuclear.fusion.TorusCoreBlock;
import com.jykito.voltexnuclear.fusion.TorusFrameBlock;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlock;
import com.jykito.voltexnuclear.reactor.ExpansionChamberBlock;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlock;
import com.jykito.voltexnuclear.reactor.ReactorFrameBlock;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlock;
import com.jykito.voltexnuclear.starforge.StarForgeFrameBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

/** Blocks of the add-on. The tool tags and loot tables live in data-src (a block without them breaks survival quietly). */
public final class NuclearBlocks {

    private NuclearBlocks() {}

    /** Uranium ore in stone and in deepslate: drops raw uranium, takes an iron pickaxe (DESIGN step 1). */
    public static Supplier<Block> URANIUM_ORE;
    public static Supplier<Block> DEEPSLATE_URANIUM_ORE;
    /** Nine raw uranium, like the raw metal blocks of Voltex (DESIGN step 20). */
    public static Supplier<Block> RAW_URANIUM_BLOCK;

    /** The casing of the add-on's machines, the cascade's rail, collector and cabinet (DESIGN steps 3 and 18). */
    public static Supplier<Block> NUCLEAR_CASING;
    /** The middle block of a cascade column. */
    public static Supplier<Block> CENTRIFUGE;
    /** The core of the centrifuge cascade. */
    public static Supplier<Block> CASCADE_CORE;
    /** Enriched uranium and an empty Voltex rod into a fuel rod, one block, epoch 4 (DESIGN step 2). */
    public static Supplier<Block> PELLET_PRESS;
    /** Heavy water and an empty cell into a deuterium cell, one block, epoch 6 (DESIGN step 13). */
    public static Supplier<Block> ELECTROLYZER;
    /** The fission reactor (DESIGN step 26): its core, the casing inside it, an expansion chamber for a socket. */
    public static Supplier<Block> REACTOR_CORE;
    public static Supplier<Block> REACTOR_FRAME;
    public static Supplier<Block> EXPANSION_CHAMBER;
    /** The energy output connector: a Voltex port that gives the energy of an add-on multiblock out to cables. */
    public static Supplier<Block> ENERGY_OUTPUT_PORT;
    /** The radiochemical plant (DESIGN step 13): its core and the thick glass of its hot cell. */
    public static Supplier<Block> RADIOCHEM_CORE;
    public static Supplier<Block> HOT_CELL_GLASS;
    /** The core of the heavy water column (DESIGN step 13). */
    public static Supplier<Block> COLUMN_CORE;
    /** The fusion torus (DESIGN step 18): its core, the casing inside it, a torus coil. */
    public static Supplier<Block> TORUS_CORE;
    public static Supplier<Block> TORUS_FRAME;
    public static Supplier<Block> TORUS_COIL;
    /** The star forge (DESIGN step 28): its core and the casing of its dome. */
    public static Supplier<Block> STAR_FORGE_CORE;
    public static Supplier<Block> STAR_FORGE_FRAME;

    static void register(Platform p) {
        // the strength of the vanilla iron ores and the raw iron block
        URANIUM_ORE = p.register(Registries.BLOCK, "uranium_ore", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(3.0F, 3.0F)));
        DEEPSLATE_URANIUM_ORE = p.register(Registries.BLOCK, "deepslate_uranium_ore", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)));
        RAW_URANIUM_BLOCK = p.register(Registries.BLOCK, "raw_uranium_block", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

        NUCLEAR_CASING = p.register(Registries.BLOCK, "nuclear_casing", () -> new NuclearCasingBlock(machine()));
        CENTRIFUGE = p.register(Registries.BLOCK, "centrifuge", () -> new CentrifugeBlock(machine()));
        CASCADE_CORE = p.register(Registries.BLOCK, "cascade_core", () -> new CascadeCoreBlock(machine()));
        PELLET_PRESS = p.register(Registries.BLOCK, "pellet_press", () -> new MachineBlock(machine(), () -> NuclearBlockEntities.PELLET_PRESS.get(), PelletPressBlockEntity::new));
        ELECTROLYZER = p.register(Registries.BLOCK, "electrolyzer", () -> new MachineBlock(machine(), () -> NuclearBlockEntities.ELECTROLYZER.get(), ElectrolyzerBlockEntity::new));
        REACTOR_CORE = p.register(Registries.BLOCK, "reactor_core", () -> new ReactorCoreBlock(machine()));
        // pistons must not move a block of the reactor: it would leave its place without breaking (DESIGN step 16)
        REACTOR_FRAME = p.register(Registries.BLOCK, "reactor_frame", () -> new ReactorFrameBlock(machine().pushReaction(PushReaction.BLOCK)));
        EXPANSION_CHAMBER = p.register(Registries.BLOCK, "expansion_chamber", () -> new ExpansionChamberBlock(machine().pushReaction(PushReaction.BLOCK)));
        ENERGY_OUTPUT_PORT = p.register(Registries.BLOCK, "energy_output_port", () -> new EnergyOutputPortBlock(machine()));
        RADIOCHEM_CORE = p.register(Registries.BLOCK, "radiochem_core", () -> new RadiochemCoreBlock(machine()));
        HOT_CELL_GLASS = p.register(Registries.BLOCK, "hot_cell_glass", () -> new HotCellGlassBlock(machine().sound(SoundType.GLASS)));
        COLUMN_CORE = p.register(Registries.BLOCK, "heavy_water_column_core", () -> new ColumnCoreBlock(machine()));
        TORUS_CORE = p.register(Registries.BLOCK, "torus_core", () -> new TorusCoreBlock(machine()));
        TORUS_FRAME = p.register(Registries.BLOCK, "torus_frame", () -> new TorusFrameBlock(machine().pushReaction(PushReaction.BLOCK)));
        TORUS_COIL = p.register(Registries.BLOCK, "torus_coil", () -> new TorusCoilBlock(machine().pushReaction(PushReaction.BLOCK)));
        STAR_FORGE_CORE = p.register(Registries.BLOCK, "star_forge_core", () -> new StarForgeCoreBlock(machine()));
        STAR_FORGE_FRAME = p.register(Registries.BLOCK, "star_forge_frame", () -> new StarForgeFrameBlock(machine().pushReaction(PushReaction.BLOCK)));
    }

    /** Machine blocks: the strength of the Voltex machines, a pickaxe to take them. */
    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL);
    }
}
