package com.jykito.voltexnuclear.radiochem;

import com.jykito.industrialcore.api.WandAssemblable;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
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
 * The core of the radiochemical plant, in the left pillar of the hot cell. Its state carries formed and running for the
 * client; a click opens the window, a click with the structure wand while sneaking builds the rest. The right click is
 * declared in both spellings, see {@link com.jykito.voltexnuclear.cascade.NuclearCasingBlock}.
 */
public class RadiochemCoreBlock extends Block implements EntityBlock, WandAssemblable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** The water in the plant in eighths, for the level lamp on its tanks; changed only when it changes. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);

    public RadiochemCoreBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(FORMED, false).setValue(LIT, false).setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT, LEVEL);
    }

    /** The face looks at the one who places it; the plant then runs to their right. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /** A formed core is its piece of the plant, which does not fill the block. */
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiochemCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != NuclearBlockEntities.RADIOCHEM_CORE.get()) return null;
        return (l, p, s, be) -> ((RadiochemCoreBlockEntity) be).serverTick();
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    private static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof RadiochemCoreBlockEntity core) player.openMenu(core);
        return InteractionResult.CONSUME;
    }

    /** A click on a formed casing or glass of the plant opens its core. */
    public static InteractionResult openFrom(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos core = RadiochemStructure.findCore(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        if (core != null) return open(level, core, player);
        return InteractionResult.PASS;
    }

    /** Broken or replaced: the plant comes apart, its blocks show again. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RadiochemCoreBlockEntity core) core.dismantle();
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void buildWithWand(Level level, BlockPos core, BlockState coreState, WandBuild build) {
        RadiochemStructure.build(level, core, coreState.getValue(FACING), build);
    }
}
