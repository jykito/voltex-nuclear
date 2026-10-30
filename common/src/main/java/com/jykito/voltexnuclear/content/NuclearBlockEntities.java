package com.jykito.voltexnuclear.content;

import com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity;
import com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity;
import com.jykito.voltexnuclear.machine.EnergyOutputPortBlockEntity;
import com.jykito.voltexnuclear.platform.Platform;
import com.jykito.voltexnuclear.press.PelletPressBlockEntity;
import com.jykito.voltexnuclear.column.ColumnCoreBlockEntity;
import com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** Block entity types of the add-on. */
public final class NuclearBlockEntities {

    private NuclearBlockEntities() {}

    public static Supplier<BlockEntityType<CascadeCoreBlockEntity>> CASCADE_CORE;
    public static Supplier<BlockEntityType<PelletPressBlockEntity>> PELLET_PRESS;
    public static Supplier<BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER;
    public static Supplier<BlockEntityType<ReactorCoreBlockEntity>> REACTOR_CORE;
    public static Supplier<BlockEntityType<EnergyOutputPortBlockEntity>> ENERGY_OUTPUT_PORT;
    public static Supplier<BlockEntityType<RadiochemCoreBlockEntity>> RADIOCHEM_CORE;
    public static Supplier<BlockEntityType<ColumnCoreBlockEntity>> COLUMN_CORE;
    public static Supplier<BlockEntityType<TorusCoreBlockEntity>> TORUS_CORE;
    public static Supplier<BlockEntityType<StarForgeCoreBlockEntity>> STAR_FORGE_CORE;

    static void register(Platform p) {
        CASCADE_CORE = p.register(Registries.BLOCK_ENTITY_TYPE, "cascade_core",
                () -> BlockEntityType.Builder.of(CascadeCoreBlockEntity::new, NuclearBlocks.CASCADE_CORE.get()).build(null));
        PELLET_PRESS = p.register(Registries.BLOCK_ENTITY_TYPE, "pellet_press",
                () -> BlockEntityType.Builder.of(PelletPressBlockEntity::new, NuclearBlocks.PELLET_PRESS.get()).build(null));
        ELECTROLYZER = p.register(Registries.BLOCK_ENTITY_TYPE, "electrolyzer",
                () -> BlockEntityType.Builder.of(ElectrolyzerBlockEntity::new, NuclearBlocks.ELECTROLYZER.get()).build(null));
        REACTOR_CORE = p.register(Registries.BLOCK_ENTITY_TYPE, "reactor_core",
                () -> BlockEntityType.Builder.of(ReactorCoreBlockEntity::new, NuclearBlocks.REACTOR_CORE.get()).build(null));
        ENERGY_OUTPUT_PORT = p.register(Registries.BLOCK_ENTITY_TYPE, "energy_output_port",
                () -> BlockEntityType.Builder.of(EnergyOutputPortBlockEntity::new, NuclearBlocks.ENERGY_OUTPUT_PORT.get()).build(null));
        RADIOCHEM_CORE = p.register(Registries.BLOCK_ENTITY_TYPE, "radiochem_core",
                () -> BlockEntityType.Builder.of(RadiochemCoreBlockEntity::new, NuclearBlocks.RADIOCHEM_CORE.get()).build(null));
        COLUMN_CORE = p.register(Registries.BLOCK_ENTITY_TYPE, "heavy_water_column_core",
                () -> BlockEntityType.Builder.of(ColumnCoreBlockEntity::new, NuclearBlocks.COLUMN_CORE.get()).build(null));
        TORUS_CORE = p.register(Registries.BLOCK_ENTITY_TYPE, "torus_core",
                () -> BlockEntityType.Builder.of(TorusCoreBlockEntity::new, NuclearBlocks.TORUS_CORE.get()).build(null));
        STAR_FORGE_CORE = p.register(Registries.BLOCK_ENTITY_TYPE, "star_forge_core",
                () -> BlockEntityType.Builder.of(StarForgeCoreBlockEntity::new, NuclearBlocks.STAR_FORGE_CORE.get()).build(null));
    }
}
