package com.jykito.voltexnuclear.radiochem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The thick glass of the hot cell's window (DESIGN, the table of machines: the plant is its core and this glass),
 * see-through as the window is in the game. {@link HalfTransparentBlock} is the glass base both versions share (1.20.1
 * calls the glass class AbstractGlassBlock, 1.21.1 TransparentBlock). In a formed plant it knows the plant's facing, and a
 * click opens the core. The right click is declared in both spellings, see
 * {@link com.jykito.voltexnuclear.cascade.NuclearCasingBlock}.
 */
public class HotCellGlassBlock extends HalfTransparentBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Which of the window's two panes it is in a formed plant, from the core outwards. */
    public static final IntegerProperty PANE = IntegerProperty.create("pane", 0, 1);

    public HotCellGlassBlock(Properties props) {
        super(props.noOcclusion().isViewBlocking(HotCellGlassBlock::never).isSuffocating(HotCellGlassBlock::never)
                .isRedstoneConductor(HotCellGlassBlock::never));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(FORMED, false).setValue(PANE, 0));
    }

    private static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, PANE);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return state.getValue(FORMED) ? RadiochemCoreBlock.openFrom(state, level, pos, player) : InteractionResult.PASS;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return state.getValue(FORMED) ? RadiochemCoreBlock.openFrom(state, level, pos, player) : InteractionResult.PASS;
    }
}
