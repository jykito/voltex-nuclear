package com.jykito.voltexnuclear.fusion;

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
 * Nuclear casing inside a formed fusion torus (DESIGN step 18): a {@link StructureFrameBlock} that knows its place in
 * {@link TorusStructure#PLACES}.
 */
public class TorusFrameBlock extends StructureFrameBlock {

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, TorusStructure.PLACES.size() - 1);

    public TorusFrameBlock(Properties props) {
        super(props);
    }

    @Override
    protected IntegerProperty part() {
        return PART;
    }

    @Override
    protected VoxelShape shape(int part, Direction facing) {
        return TorusShapes.place(part, facing);
    }

    @Nullable
    @Override
    protected BlockPos core(Level level, BlockPos pos, BlockState state) {
        TorusCoreBlockEntity core = TorusStructure.coreOfPart(level, pos, TorusStructure.PLACES.get(state.getValue(PART)), state.getValue(FACING));
        return core != null ? core.getBlockPos() : null;
    }

    @Override
    protected void requestCheck(Level level, BlockPos core) {
        if (level.getBlockEntity(core) instanceof TorusCoreBlockEntity be) be.requestCheck();
    }

    @Override
    protected InteractionResult open(Level level, BlockPos core, Player player) {
        return TorusCoreBlock.open(level, core, player);
    }
}
