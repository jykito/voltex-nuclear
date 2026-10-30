package com.jykito.voltexnuclear.machine;

import com.jykito.voltexnuclear.content.NuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Nuclear casing inside a formed multiblock of places (DESIGN steps 16 and 26): the fission reactor's frame, the fusion
 * torus's. The core turns its casing into these when the machine forms and back when it comes apart; the player never
 * holds one. It keeps its place in the structure and the machine's facing: the place gives its piece of the model and the
 * way to the core. To the player it is the casing: its name, the casing from a middle click, the casing dropped when
 * broken.
 * <p>
 * Broken, or a neighbour changed (a port pulled out of its socket gives no word of its own), it asks the core to look at
 * its blocks again. The right click and the middle click are declared in both spellings, see
 * {@link com.jykito.voltexnuclear.cascade.NuclearCasingBlock}.
 */
public abstract class StructureFrameBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected StructureFrameBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(part(), 0).setValue(FACING, Direction.SOUTH));
    }

    /** The number of the place in the structure; a static of the subclass, so it is there when the state is built. */
    protected abstract IntegerProperty part();

    /** The shape of the block in place {@code part} of a machine looking {@code facing}. */
    protected abstract VoxelShape shape(int part, Direction facing);

    /** Where the core of a frame block stands, if it is still there. */
    @Nullable
    protected abstract BlockPos core(Level level, BlockPos pos, BlockState state);

    /** Asks the core at {@code core} to look at its blocks on its next tick. */
    protected abstract void requestCheck(Level level, BlockPos core);

    /** Opens the window of the core at {@code core}. */
    protected abstract InteractionResult open(Level level, BlockPos core, Player player);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(part(), FACING);
    }

    /** Its piece of the model cut to its cell (DESIGN step 16): the aim and the feet go by the model, not the block. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(state.getValue(part()), state.getValue(FACING));
    }

    /** A piece does not fill its block, so it must not hide the faces of its neighbours. */
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    /** The casing's name, so tooltips and Jade show what the player put there. */
    @Override
    public String getDescriptionId() {
        return NuclearBlocks.NUCLEAR_CASING.get().getDescriptionId();
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(NuclearBlocks.NUCLEAR_CASING.get());
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(NuclearBlocks.NUCLEAR_CASING.get());
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openCore(state, level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openCore(state, level, pos, player);
    }

    private InteractionResult openCore(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos core = core(level, pos, state);
        return core != null ? open(level, core, player) : InteractionResult.PASS;
    }

    /** Taken out by anything but the core turning it back into casing: the machine comes apart. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !newState.is(this) && !newState.is(NuclearBlocks.NUCLEAR_CASING.get())) {
            BlockPos core = core(level, pos, state);
            if (core != null) requestCheck(level, core);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide) {
            BlockPos core = core(level, pos, state);
            if (core != null) requestCheck(level, core);
        }
        super.neighborChanged(state, level, pos, block, from, moving);
    }
}
