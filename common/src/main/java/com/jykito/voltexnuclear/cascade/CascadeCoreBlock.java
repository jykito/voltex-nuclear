package com.jykito.voltexnuclear.cascade;

import com.jykito.industrialcore.api.WandAssemblable;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.core.cascade.Cascade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The core of the centrifuge cascade: the top of the cabinet with the face. Its state carries what the client draws from:
 * formed, running, the number of columns; the server changes it only when that changes (DESIGN step 18). A click opens
 * the window, a click with the structure wand while sneaking builds the rest (the wand's own rule, as for Voltex cores).
 * The right click is declared in both spellings, see {@link NuclearCasingBlock}.
 */
public class CascadeCoreBlock extends Block implements EntityBlock, WandAssemblable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty COLUMNS = IntegerProperty.create("columns", 0, Cascade.MAX_COLUMNS);

    public CascadeCoreBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(FORMED, false).setValue(LIT, false).setValue(COLUMNS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT, COLUMNS);
    }

    /** The face looks at the one who places it; the row then runs to their right. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CascadeCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != NuclearBlockEntities.CASCADE_CORE.get()) return null;
        return (l, p, s, be) -> ((CascadeCoreBlockEntity) be).serverTick();
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    private static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof CascadeCoreBlockEntity core) player.openMenu(core);
        return InteractionResult.CONSUME;
    }

    /** A click on a formed casing or centrifuge opens the core it belongs to; an unformed one does nothing. */
    static InteractionResult openFrom(BlockState state, Level level, BlockPos pos, Player player, boolean formed) {
        if (!formed) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos core = CascadeStructure.findCore(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        if (core != null) return open(level, core, player);
        return InteractionResult.PASS;
    }

    /** Broken or replaced: the cascade comes apart, its blocks and ports show again. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CascadeCoreBlockEntity core) core.dismantle();
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void buildWithWand(Level level, BlockPos core, BlockState coreState, WandBuild build) {
        CascadeStructure.build(level, core, coreState.getValue(FACING), build);
    }
}
