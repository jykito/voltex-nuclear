package com.jykito.voltexnuclear.starforge;

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
 * The star forge in blocks (DESIGN step 28, Sh2 "Corner stands"): the dome of Z5 in four layers, two of the round
 * footprint (the 5 x 5 square with its corners cut: rows of 3, 5, 5, 5, 3, 21 places a layer), then 3 x 3 and the top
 * block under the crown; in each of the four empty corners of the square a stand of ports two blocks high, as the torus
 * has. Places are counted in the forge's own frame, as the torus's: x from -2 to 2 left to right for someone facing the
 * core, z from -2 at the back to 2 at the front where the core looks, y from 0 up. The core stands in the middle of the
 * front of the lowest layer; any block of a stand may be a Voltex port. The rest is nuclear casing.
 * <p>
 * A formed forge turns its casing into {@link StarForgeFrameBlock}s that know their place, so each finds the core by
 * itself (DESIGN step 16).
 */
public final class StarForgeStructure {

    private StarForgeStructure() {}

    public static final int LAYERS = 4, STAND = 2;
    /** Half the width of each row of the round footprint, z from -2 to 2. */
    private static final int[] HALF = {1, 2, 2, 2, 1};
    /** The corners the stands stand in, as signs of x and z, in the order of the places. */
    private static final int[][] STANDS = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

    /** What a place takes. */
    public enum Role { CASING, PORT_SOCKET, CORE }

    /** One block of the structure, its index the number a frame block keeps. */
    public record Place(int index, int x, int y, int z, Role role) {}

    public static final List<Place> PLACES;
    public static final Place CORE;

    static {
        List<Place> places = new ArrayList<>();
        for (int y = 0; y < 2; y++)
            for (int z = -2; z <= 2; z++)
                for (int x = -HALF[z + 2]; x <= HALF[z + 2]; x++)
                    places.add(new Place(places.size(), x, y, z, y == 0 && x == 0 && z == 2 ? Role.CORE : Role.CASING));
        for (int z = -1; z <= 1; z++)
            for (int x = -1; x <= 1; x++) places.add(new Place(places.size(), x, 2, z, Role.CASING));
        places.add(new Place(places.size(), 0, 3, 0, Role.CASING));
        for (int[] s : STANDS)
            for (int y = 0; y < STAND; y++) places.add(new Place(places.size(), 2 * s[0], y, 2 * s[1], Role.PORT_SOCKET));
        PLACES = List.copyOf(places);
        CORE = places.stream().filter(p -> p.role() == Role.CORE).findFirst().orElseThrow();
    }

    /** The forge's right for someone facing the core, local +x. */
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Where a place stands for a core at {@code core} looking {@code facing}. */
    public static BlockPos pos(BlockPos core, Direction facing, Place p) {
        return core.relative(right(facing), p.x()).relative(facing, p.z() - CORE.z()).above(p.y() - CORE.y());
    }

    /** The core of a block that stands in place {@code p} of a forge looking {@code facing}. */
    public static BlockPos coreOf(BlockPos pos, Direction facing, Place p) {
        return pos.relative(right(facing), -p.x()).relative(facing, CORE.z() - p.z()).above(CORE.y() - p.y());
    }

    /** The two sides of a stand's block that look out of the forge: its side along x, then its side along z. */
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

    /** A frame block that this very forge made: its place and its facing match, so a neighbour's frame never counts. */
    private static boolean isOwnFrame(BlockState state, Direction facing, Place p) {
        return state.is(NuclearBlocks.STAR_FORGE_FRAME.get()) && state.getValue(StarForgeFrameBlock.PART) == p.index()
                && state.getValue(StarForgeFrameBlock.FACING) == facing;
    }

    private static boolean fits(BlockState state, Direction facing, Place p) {
        return switch (p.role()) {
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

    /** The structure wand: casing into every empty place; the ports are the player's. */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wand) {
        for (Place p : PLACES) {
            if (p == CORE) continue;
            BlockPos at = pos(core, facing, p);
            if (!level.isLoaded(at) || fits(level.getBlockState(at), facing, p)) continue;
            wand.place(level, at, NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState());
        }
    }

    /**
     * Asks every forge core this block could belong to to look at its blocks again: a casing or a port that has just been
     * placed may have finished a forge (DESIGN step 16: checked when a block is placed, not on a timer).
     */
    public static void notifyNear(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Place p : PLACES) {
            if (p == CORE) continue;
            for (Direction f : Direction.Plane.HORIZONTAL) {
                BlockPos c = coreOf(pos, f, p);
                if (level.isLoaded(c) && level.getBlockState(c).is(NuclearBlocks.STAR_FORGE_CORE.get())
                        && level.getBlockState(c).getValue(StarForgeCoreBlock.FACING) == f
                        && level.getBlockEntity(c) instanceof StarForgeCoreBlockEntity core) core.requestCheck();
            }
        }
    }

    /** The core of a formed frame block, or null when it is gone. */
    @Nullable
    public static StarForgeCoreBlockEntity coreOfPart(Level level, BlockPos pos, Place p, Direction facing) {
        return level.getBlockEntity(coreOf(pos, facing, p)) instanceof StarForgeCoreBlockEntity core ? core : null;
    }

    /**
     * The formed core a block of a forge belongs to, found from the block states alone, so it works on the client too: the
     * core itself, a frame by its place, a hidden port by the sockets it may stand in. {@code state} is the block's own
     * state, which may already be gone from the world while it is being broken. Null when there is none.
     */
    @Nullable
    public static BlockPos formedCore(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.is(NuclearBlocks.STAR_FORGE_CORE.get())) return state.getValue(StarForgeCoreBlock.FORMED) ? pos : null;
        if (state.is(NuclearBlocks.STAR_FORGE_FRAME.get())) {
            Direction f = state.getValue(StarForgeFrameBlock.FACING);
            BlockPos c = coreOf(pos, f, PLACES.get(state.getValue(StarForgeFrameBlock.PART)));
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
        return s.is(NuclearBlocks.STAR_FORGE_CORE.get()) && s.getValue(StarForgeCoreBlock.FACING) == facing && s.getValue(StarForgeCoreBlock.FORMED);
    }

    /** The place of a hidden port standing at {@code pos} in the forge of a core at {@code core} looking {@code facing}. */
    @Nullable
    public static Place socketAt(BlockPos core, Direction facing, BlockPos pos) {
        for (Place p : PLACES) if (p.role() == Role.PORT_SOCKET && pos(core, facing, p).equals(pos)) return p;
        return null;
    }
}
