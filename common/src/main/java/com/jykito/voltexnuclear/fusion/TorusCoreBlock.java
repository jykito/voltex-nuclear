package com.jykito.voltexnuclear.fusion;

import com.jykito.industrialcore.api.WandAssemblable;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The core of the fusion torus: the block with the face on the front segment of the ring, in the middle layer (DESIGN
 * step 18), the block the torus is gated by. The face looks at the one who places it. A click looks at the blocks and
 * opens the torus; a click with the structure wand while sneaking lays the casing and the coils (the wand's own rule, as
 * for Voltex cores). The right click is declared in both spellings, see
 * {@link com.jykito.voltexnuclear.cascade.NuclearCasingBlock}.
 */
public class TorusCoreBlock extends Block implements EntityBlock, WandAssemblable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Burning or igniting: the face is lit. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public TorusCoreBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    /** Formed, the piece of the model in its place (DESIGN step 16); before that a whole block. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FORMED) ? TorusShapes.place(TorusStructure.CORE.index(), state.getValue(FACING)) : Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TorusCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != NuclearBlockEntities.TORUS_CORE.get()) return null;
        return (l, p, s, be) -> ((TorusCoreBlockEntity) be).serverTick();
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    /**
     * A click on the torus: it looks at its blocks at once (a port put into a stand gives no word of its own); formed, it
     * opens the window, else it says it does not stand.
     */
    static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TorusCoreBlockEntity core)) return InteractionResult.PASS;
        core.check();
        if (core.formed()) player.openMenu(core);
        else player.displayClientMessage(Component.translatable("message.voltex_nuclear.torus.not_formed"), true);
        return InteractionResult.CONSUME;
    }

    /** Broken or replaced: what the core holds drops, the torus comes apart, its casing, coils and ports show again. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TorusCoreBlockEntity core) {
            core.dropAll();
            core.dismantle();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void buildWithWand(Level level, BlockPos core, BlockState coreState, WandBuild build) {
        TorusStructure.build(level, core, coreState.getValue(FACING), build);
        if (level.getBlockEntity(core) instanceof TorusCoreBlockEntity be) be.requestCheck();
    }
}
