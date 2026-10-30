package com.jykito.voltexnuclear.cascade;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.column.ColumnCoreBlock;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.fusion.TorusStructure;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlock;
import com.jykito.voltexnuclear.reactor.ReactorStructure;
import com.jykito.voltexnuclear.starforge.StarForgeStructure;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The casing of the add-on's machines (DESIGN step 20: one casing for the family). On its own a plain block; in a formed
 * cascade, radiochemical plant or heavy water column its model turns into the piece of the machine standing in it ({@link CasingRole}),
 * turned with the core, and a click on it opens the core. A formed fission reactor or fusion torus turns it into its frame instead
 * ({@link ReactorStructure}, {@link TorusStructure}).
 * <p>
 * The right click has a different method on each version: {@code use} on Forge 1.20.1, {@code useWithoutItem} on NeoForge
 * 1.21.1. The class declares both; each version overrides its own and never calls the other.
 */
public class NuclearCasingBlock extends Block {

    public static final EnumProperty<CasingRole> ROLE = EnumProperty.create("role", CasingRole.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Which piece of a formed radiochemical plant this block is: its cell, column * 9 + depth * 3 + level. */
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, 44);

    public NuclearCasingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROLE, CasingRole.NONE).setValue(FACING, Direction.SOUTH).setValue(LIT, false).setValue(PIECE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLE, FACING, LIT, PIECE);
    }

    /** A piece of a formed machine does not fill its block, so it must not hide the faces of its neighbours. */
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(ROLE) == CasingRole.NONE ? Shapes.block() : Shapes.empty();
    }

    /**
     * Placed casing may finish a fission reactor, a fusion torus or a star forge: it asks the cores it could belong to to
     * look (DESIGN step 16). Not when one of them turns its frame back into casing as it comes apart.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (old.is(this) || old.is(NuclearBlocks.REACTOR_FRAME.get()) || old.is(NuclearBlocks.TORUS_FRAME.get())
                || old.is(NuclearBlocks.STAR_FORGE_FRAME.get())) return;
        ReactorStructure.notifyNear(level, pos);
        TorusStructure.notifyNear(level, pos);
        StarForgeStructure.notifyNear(level, pos);
    }

    /** A Voltex port put next to casing may be the last block of a reactor's plinth or a stand of a torus or a forge; the port gives no word of its own. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide || !(level.getBlockState(from).getBlock() instanceof MultiblockPortBlock)) return;
        ReactorStructure.notifyNear(level, from);
        TorusStructure.notifyNear(level, from);
        StarForgeStructure.notifyNear(level, from);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(state, level, pos, player);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(state, level, pos, player);
    }

    /** A formed block opens the core of its machine; the role tells which machine that is. */
    private static InteractionResult open(BlockState state, Level level, BlockPos pos, Player player) {
        CasingRole role = state.getValue(ROLE);
        if (role == CasingRole.PLANT) return RadiochemCoreBlock.openFrom(state, level, pos, player);
        if (role == CasingRole.HEAVY_COLUMN) return ColumnCoreBlock.openFrom(state, level, pos, player);
        return CascadeCoreBlock.openFrom(state, level, pos, player, role != CasingRole.NONE);
    }
}
