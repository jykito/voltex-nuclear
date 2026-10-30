package com.jykito.voltexnuclear.machine;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The energy output connector: the Voltex energy connector the other way round, for the add-on multiblocks that make
 * energy (the fission reactor). It is a Voltex port of the energy kind, so a socket that takes Voltex ports takes it, the
 * core hides it in the plinth, and the plinth wears its own mark ({@code mark_energy_output}). Its block entity is its
 * own: the Voltex cores look for their energy connectors by theirs, so they never take this one for an input. No window
 * and no tick; the work is in {@link EnergyOutputPortBlockEntity} and the core.
 * <p>
 * {@code use} is the Voltex port's: Forge 1.20.1 calls it as the block's, NeoForge 1.21.1 through the port's
 * {@code useItemOn}.
 */
public class EnergyOutputPortBlock extends MultiblockPortBlock {

    public EnergyOutputPortBlock(Properties props) {
        super(Kind.ENERGY, props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyOutputPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
