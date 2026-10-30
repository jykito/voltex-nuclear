package com.jykito.voltexnuclear.starforge;

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
 * The core of the star forge (DESIGN step 28): it stands in the middle of the front of the dome's lowest layer and looks
 * out. Formed, a click opens the window; the structure wand of Voltex lays the casing round it.
 */
public class StarForgeCoreBlock extends Block implements EntityBlock, WandAssemblable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Melting: the face is lit and the star in the crown burns. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public StarForgeCoreBlock(Properties props) {
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

    /** A piece of the formed forge does not fill its block, so it must not hide the faces of its neighbours. */
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    /** Formed, the piece of the model in its place (DESIGN step 16); before that a whole block. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FORMED) ? StarForgeShapes.place(StarForgeStructure.CORE.index(), state.getValue(FACING)) : Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StarForgeCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != NuclearBlockEntities.STAR_FORGE_CORE.get()) return null;
        return (l, p, s, be) -> ((StarForgeCoreBlockEntity) be).serverTick();
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    /**
     * A click on the forge: it looks at its blocks at once (a port put into a stand gives no word of its own); formed, it
     * opens the window, else it says it does not stand.
     */
    static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof StarForgeCoreBlockEntity core)) return InteractionResult.PASS;
        core.check();
        if (core.formed()) player.openMenu(core);
        else player.displayClientMessage(Component.translatable("message.voltex_nuclear.star_forge.not_formed"), true);
        return InteractionResult.CONSUME;
    }

    /** Broken or replaced: what the core holds drops, the forge comes apart, its casing and ports show again. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StarForgeCoreBlockEntity core) {
            core.dropAll();
            core.dismantle();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void buildWithWand(Level level, BlockPos core, BlockState coreState, WandBuild build) {
        StarForgeStructure.build(level, core, coreState.getValue(FACING), build);
        if (level.getBlockEntity(core) instanceof StarForgeCoreBlockEntity be) be.requestCheck();
    }
}
