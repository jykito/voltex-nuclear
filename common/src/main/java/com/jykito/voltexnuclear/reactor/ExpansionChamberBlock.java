package com.jykito.voltexnuclear.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An expansion chamber (DESIGN step 5): in a socket in the middle of a side of the reactor it opens a row of the active
 * zone on that side; all four make the zone 7 x 7. Formed, it faces out of the reactor and draws the chamber module on the
 * wall. Placed, it asks a reactor near it to look at its blocks; taken out of a formed reactor, the reactor checks again.
 * The right click is declared in both spellings, see {@link com.jykito.voltexnuclear.cascade.NuclearCasingBlock}.
 */
public class ExpansionChamberBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ExpansionChamberBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    /** Formed, its piece of the reactor with the module on the wall (DESIGN step 16); before that a whole block. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FORMED) ? ReactorShapes.chamber(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) ReactorStructure.notifyNear(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !newState.is(this)) {
            BlockPos core = ReactorStructure.formedCore(level, pos, state);
            if (core != null && level.getBlockEntity(core) instanceof ReactorCoreBlockEntity be) be.requestCheck();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openCore(state, level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openCore(state, level, pos, player);
    }

    private static InteractionResult openCore(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(FORMED)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos core = ReactorStructure.formedCore(level, pos, state);
        return core != null ? ReactorCoreBlock.open(level, core, player) : InteractionResult.PASS;
    }
}
