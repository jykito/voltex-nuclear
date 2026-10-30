package com.jykito.voltexnuclear.starforge;

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
 * A block of a formed star forge: the casing in its place, knowing its index in {@link StarForgeStructure#PLACES} and the
 * forge's facing; it draws its piece of the model and takes its shape.
 */
public class StarForgeFrameBlock extends StructureFrameBlock {

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, StarForgeStructure.PLACES.size() - 1);

    public StarForgeFrameBlock(Properties props) {
        super(props);
    }

    @Override
    protected IntegerProperty part() {
        return PART;
    }

    @Override
    protected VoxelShape shape(int part, Direction facing) {
        return StarForgeShapes.place(part, facing);
    }

    @Nullable
    @Override
    protected BlockPos core(Level level, BlockPos pos, BlockState state) {
        StarForgeCoreBlockEntity core = StarForgeStructure.coreOfPart(level, pos, StarForgeStructure.PLACES.get(state.getValue(PART)), state.getValue(FACING));
        return core != null ? core.getBlockPos() : null;
    }

    @Override
    protected void requestCheck(Level level, BlockPos core) {
        if (level.getBlockEntity(core) instanceof StarForgeCoreBlockEntity be) be.requestCheck();
    }

    @Override
    protected InteractionResult open(Level level, BlockPos core, Player player) {
        return StarForgeCoreBlock.open(level, core, player);
    }
}
