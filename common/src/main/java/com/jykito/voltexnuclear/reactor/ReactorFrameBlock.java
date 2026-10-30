package com.jykito.voltexnuclear.reactor;

import com.jykito.voltexnuclear.machine.StructureFrameBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Nuclear casing inside a formed reactor (DESIGN step 26): a {@link StructureFrameBlock} that knows its place in
 * {@link ReactorStructure#PLACES}.
 */
public class ReactorFrameBlock extends StructureFrameBlock {

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, ReactorStructure.PLACES.size() - 1);

    public ReactorFrameBlock(Properties props) {
        super(props);
    }

    @Override
    protected IntegerProperty part() {
        return PART;
    }

    @Override
    protected VoxelShape shape(int part, Direction facing) {
        return ReactorShapes.place(part, facing);
    }

    @Nullable
    @Override
    protected BlockPos core(Level level, BlockPos pos, BlockState state) {
        ReactorCoreBlockEntity core = ReactorStructure.coreOfFrame(level, pos, state);
        return core != null ? core.getBlockPos() : null;
    }

    @Override
    protected void requestCheck(Level level, BlockPos core) {
        if (level.getBlockEntity(core) instanceof ReactorCoreBlockEntity be) be.requestCheck();
    }

    @Override
    protected InteractionResult open(Level level, BlockPos core, Player player) {
        return ReactorCoreBlock.open(level, core, player);
    }
}
