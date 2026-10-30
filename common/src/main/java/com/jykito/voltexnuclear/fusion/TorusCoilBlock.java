package com.jykito.voltexnuclear.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A torus coil (DESIGN steps 18 and 20): the special block of the fusion torus, in the middle layer where the middle of a
 * coil of the model stands. Formed, it knows which of the eight coils it is ({@link TorusStructure#COILS}) and the
 * torus's facing, so it draws its piece of the model and finds the core; it stays a coil to the player and drops itself.
 * Placed, it asks a torus near it to look at its blocks; taken out of a formed torus, the torus checks again. The right
 * click is declared in both spellings, see {@link com.jykito.voltexnuclear.cascade.NuclearCasingBlock}.
 */
public class TorusCoilBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty COIL = IntegerProperty.create("coil", 0, 7);

    public TorusCoilBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(FORMED, false).setValue(COIL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, COIL);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    /** Formed, its piece of the torus (DESIGN step 16); before that a whole block. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FORMED) ? TorusShapes.place(TorusStructure.COILS.get(state.getValue(COIL)).index(), state.getValue(FACING)) : Shapes.block();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) TorusStructure.notifyNear(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !newState.is(this)) {
            BlockPos core = TorusStructure.formedCore(level, pos, state);
            if (core != null && level.getBlockEntity(core) instanceof TorusCoreBlockEntity be) be.requestCheck();
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
        BlockPos core = TorusStructure.formedCore(level, pos, state);
        return core != null ? TorusCoreBlock.open(level, core, player) : InteractionResult.PASS;
    }
}
