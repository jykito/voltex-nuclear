package com.jykito.voltexnuclear.fusion;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The fusion torus in blocks (DESIGN step 18, T2 "Corner stands"): three layers of the round footprint, the 7 x 7 square
 * with steps cut from its corners (rows of 3, 5, 7, 7, 7, 5, 3: 37 places a layer), and in each of the four empty corners
 * of the square a stand of ports two blocks high. Places are counted in the torus's own frame, as the reactor's: x from
 * -3 to 3 left to right for someone facing the core, z from -3 at the back to 3 at the front where the core looks, y from
 * 0 up. The core stands on the front segment of the ring in the middle layer; the torus coils in the middle layer where
 * the middles of the coils of the model stand; any block of a stand may be a Voltex port. The rest is nuclear casing.
 * <p>
 * A formed torus turns its casing into {@link TorusFrameBlock}s that know their place and tells its coils which of the
 * eight they are, so each draws its piece of the model and finds the core by itself (DESIGN step 16).
 */
public final class TorusStructure {

    private TorusStructure() {}

    public static final int LAYERS = 3, STAND = 2;
    /** Half the width of each row of the round footprint, z from -3 to 3. */
    private static final int[] HALF = {1, 2, 3, 3, 3, 2, 1};
    /** The corners the stands stand in, as signs of x and z, in the order of the places (and of the model's stands). */
    private static final int[][] STANDS = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

    /** What a place takes. */
    public enum Role { CASING, COIL, PORT_SOCKET, CORE }

    /** One block of the structure, its index the number a frame block keeps; {@code coil} the number of a coil, else -1. */
    public record Place(int index, int x, int y, int z, Role role, int coil) {}

    public static final List<Place> PLACES, COILS;
    public static final Place CORE;

    static {
        List<Place> places = new ArrayList<>(), coils = new ArrayList<>();
        for (int y = 0; y < LAYERS; y++)
            for (int z = -3; z <= 3; z++)
                for (int x = -HALF[z + 3]; x <= HALF[z + 3]; x++) {
                    Role role = role(x, y, z);
                    Place p = new Place(places.size(), x, y, z, role, role == Role.COIL ? coils.size() : -1);
                    places.add(p);
                    if (role == Role.COIL) coils.add(p);
                }
        for (int[] s : STANDS)
            for (int y = 0; y < STAND; y++) places.add(new Place(places.size(), 3 * s[0], y, 3 * s[1], Role.PORT_SOCKET, -1));
        PLACES = List.copyOf(places);
        COILS = List.copyOf(coils);
        CORE = places.stream().filter(p -> p.role() == Role.CORE).findFirst().orElseThrow();
    }

    private static Role role(int x, int y, int z) {
        if (y != 1) return Role.CASING;
        if (x == 0 && z == 3) return Role.CORE;
        int ax = Math.abs(x), az = Math.abs(z);
        return ax == 2 && az == 1 || ax == 1 && az == 2 ? Role.COIL : Role.CASING;
    }

    /** The torus's right for someone facing the core, local +x. */
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Where a place stands for a core at {@code core} looking {@code facing}. */
    public static BlockPos pos(BlockPos core, Direction facing, Place p) {
        return core.relative(right(facing), p.x()).relative(facing, p.z() - CORE.z()).above(p.y() - CORE.y());
    }

    /** The core of a block that stands in place {@code p} of a torus looking {@code facing}. */
    public static BlockPos coreOf(BlockPos pos, Direction facing, Place p) {
        return pos.relative(right(facing), -p.x()).relative(facing, CORE.z() - p.z()).above(CORE.y() - p.y());
    }

    /** The two sides of a stand's block that look out of the torus: its side along x, then its side along z. */
    public static Direction[] outward(Direction facing, Place p) {
        Direction x = p.x() > 0 ? right(facing) : right(facing).getOpposite(), z = p.z() > 0 ? facing : facing.getOpposite();
        return new Direction[]{x, z};
    }

    /** What a look at the blocks found: whether every place is right, and the ports in the stands. */
    public record Scan(boolean complete, List<BlockPos> ports) {

        static final Scan NONE = new Scan(false, List.of());
    }

    public static boolean isCasing(BlockState state) {
        return state.is(NuclearBlocks.NUCLEAR_CASING.get());
    }

    /** A frame block that this very torus made: its place and its facing match, so a neighbour's frame never counts. */
    private static boolean isOwnFrame(BlockState state, Direction facing, Place p) {
        return state.is(NuclearBlocks.TORUS_FRAME.get()) && state.getValue(TorusFrameBlock.PART) == p.index()
                && state.getValue(TorusFrameBlock.FACING) == facing;
    }

    /** A coil that is free or already this torus's own in this place. */
    private static boolean isCoil(BlockState state, Direction facing, Place p) {
        if (!state.is(NuclearBlocks.TORUS_COIL.get())) return false;
        return !state.getValue(TorusCoilBlock.FORMED)
                || state.getValue(TorusCoilBlock.COIL) == p.coil() && state.getValue(TorusCoilBlock.FACING) == facing;
    }

    private static boolean fits(BlockState state, Direction facing, Place p) {
        return switch (p.role()) {
            case COIL -> isCoil(state, facing, p);
            case PORT_SOCKET -> isCasing(state) || isOwnFrame(state, facing, p) || state.getBlock() instanceof MultiblockPortBlock;
            case CASING -> isCasing(state) || isOwnFrame(state, facing, p);
            case CORE -> false;
        };
    }

    public static Scan scan(Level level, BlockPos core, Direction facing) {
        List<BlockPos> ports = new ArrayList<>();
        for (Place p : PLACES) {
            if (p == CORE) continue;
            BlockPos at = pos(core, facing, p);
            if (!level.isLoaded(at)) return Scan.NONE;
            BlockState s = level.getBlockState(at);
            if (!fits(s, facing, p)) return Scan.NONE;
            if (s.getBlock() instanceof MultiblockPortBlock) ports.add(at);
        }
        return new Scan(true, ports);
    }

    /** The structure wand: casing into every empty place, the coils into theirs; the ports are the player's. */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wand) {
        for (Place p : PLACES) {
            if (p == CORE) continue;
            BlockPos at = pos(core, facing, p);
            if (!level.isLoaded(at) || fits(level.getBlockState(at), facing, p)) continue;
            wand.place(level, at, (p.role() == Role.COIL ? NuclearBlocks.TORUS_COIL : NuclearBlocks.NUCLEAR_CASING).get().defaultBlockState());
        }
    }

    /**
     * Asks every torus core this block could belong to to look at its blocks again: a casing, a coil or a port that has
     * just been placed may have finished a torus (DESIGN step 16: checked when a block is placed, not on a timer).
     */
    public static void notifyNear(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Place p : PLACES) {
            if (p == CORE) continue;
            for (Direction f : Direction.Plane.HORIZONTAL) {
                BlockPos c = coreOf(pos, f, p);
                if (level.isLoaded(c) && level.getBlockState(c).is(NuclearBlocks.TORUS_CORE.get())
                        && level.getBlockState(c).getValue(TorusCoreBlock.FACING) == f
                        && level.getBlockEntity(c) instanceof TorusCoreBlockEntity core) core.requestCheck();
            }
        }
    }

    /** The core of a formed frame block or coil, or null when it is gone. */
    @Nullable
    public static TorusCoreBlockEntity coreOfPart(Level level, BlockPos pos, Place p, Direction facing) {
        return level.getBlockEntity(coreOf(pos, facing, p)) instanceof TorusCoreBlockEntity core ? core : null;
    }

    /**
     * The formed core a block of a torus belongs to, found from the block states alone, so it works on the client too: the
     * core itself, a frame or a formed coil by its place, a hidden port by the sockets it may stand in. {@code state} is
     * the block's own state, which may already be gone from the world while it is being broken. Null when there is none.
     */
    @Nullable
    public static BlockPos formedCore(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.is(NuclearBlocks.TORUS_CORE.get())) return state.getValue(TorusCoreBlock.FORMED) ? pos : null;
        if (state.is(NuclearBlocks.TORUS_FRAME.get())) {
            Direction f = state.getValue(TorusFrameBlock.FACING);
            BlockPos c = coreOf(pos, f, PLACES.get(state.getValue(TorusFrameBlock.PART)));
            return isFormedCore(level, c, f) ? c : null;
        }
        if (state.is(NuclearBlocks.TORUS_COIL.get())) {
            if (!state.getValue(TorusCoilBlock.FORMED)) return null;
            Direction f = state.getValue(TorusCoilBlock.FACING);
            BlockPos c = coreOf(pos, f, COILS.get(state.getValue(TorusCoilBlock.COIL)));
            return isFormedCore(level, c, f) ? c : null;
        }
        if (!(state.getBlock() instanceof MultiblockPortBlock) || !state.getValue(MultiblockPortBlock.CONCEALED)) return null;
        for (Place p : PLACES) {
            if (p.role() != Role.PORT_SOCKET) continue;
            for (Direction f : Direction.Plane.HORIZONTAL) {
                BlockPos c = coreOf(pos, f, p);
                if (isFormedCore(level, c, f)) return c;
            }
        }
        return null;
    }

    private static boolean isFormedCore(BlockGetter level, BlockPos c, Direction facing) {
        BlockState s = level.getBlockState(c);
        return s.is(NuclearBlocks.TORUS_CORE.get()) && s.getValue(TorusCoreBlock.FACING) == facing && s.getValue(TorusCoreBlock.FORMED);
    }

    /** The place of a hidden port standing at {@code pos} in the torus of a core at {@code core} looking {@code facing}. */
    @Nullable
    public static Place socketAt(BlockPos core, Direction facing, BlockPos pos) {
        for (Place p : PLACES) if (p.role() == Role.PORT_SOCKET && pos(core, facing, p).equals(pos)) return p;
        return null;
    }
}
