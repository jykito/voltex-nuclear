package com.jykito.voltexnuclear.machine;

import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The energy output connector. It keeps no energy of its own: its energy is a window onto the buffer of the multiblock
 * it stands in, open for taking only. The multiblock pushes energy into the cables next to it every tick, the way the
 * Voltex generators do; cables of other mods that draw energy themselves take it here. Each loader exposes this as its
 * own energy capability.
 */
public class EnergyOutputPortBlockEntity extends BlockEntity {

    /** The core of the multiblock it stands in; set by the core on every tick, not saved. */
    @Nullable private BlockPos source;

    public EnergyOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.ENERGY_OUTPUT_PORT.get(), pos, state);
    }

    public void link(@Nullable BlockPos source) {
        this.source = source;
    }

    @Nullable
    private EnergySource source() {
        return source != null && level != null && level.getBlockEntity(source) instanceof EnergySource s ? s : null;
    }

    public int extract(int max, boolean simulate) {
        EnergySource s = source();
        return s == null || max <= 0 ? 0 : s.giveEnergy(max, simulate);
    }

    public int stored() {
        EnergySource s = source();
        return s == null ? 0 : s.energyStored();
    }

    public int capacity() {
        EnergySource s = source();
        return s == null ? 0 : s.energyCapacity();
    }
}
