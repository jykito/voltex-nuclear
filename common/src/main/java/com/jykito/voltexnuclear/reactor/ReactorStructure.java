package com.jykito.voltexnuclear.reactor;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.reactor.Zone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The fission reactor in blocks (DESIGN step 26, "Ribs on a plinth, tall"): four layers of 21 places, a 5 x 5 square
 * without its corners, under a lid that stands 0.2 above the last layer. Places are counted in the reactor's own frame:
 * x from -2 to 2 left to right for someone facing the core, z from -2 at the back to 2 at the front where the core looks,
 * y from 0 (the plinth) to 3. The core stands at the front of the top layer, the sockets of the expansion chambers in the
 * middle of each side of layer 1, and the 12 edge blocks of the plinth may hold Voltex ports. The rest is nuclear casing,
 * the one casing of the add-on; the core is what the reactor is gated by.
 * <p>
 * A formed reactor turns its casing into {@link ReactorFrameBlock}s that know their place, so each draws its piece of the
 * model and finds the core by itself (DESIGN step 16: no timers, the blocks report).
 */
public final class ReactorStructure {

    private ReactorStructure() {}

    public static final int LAYERS = 4, PLACES_PER_LAYER = 21;

    /** What a place takes. */
    public enum Role { CASING, PORT_SOCKET, CHAMBER_SOCKET, CORE }

    /** One block of the structure, its index the number a frame block keeps. */
    public record Place(int index, int x, int y, int z, Role role) {}

    public static final List<Place> PLACES;
    public static final Place CORE;

    static {
        List<Place> places = new ArrayList<>();
        Place core = null;
        for (int y = 0; y < LAYERS; y++)
            for (int z = -2; z <= 2; z++)
                for (int x = -2; x <= 2; x++) {
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;   // no corners: the plinth steps round the body
                    Place p = new Place(places.size(), x, y, z, role(x, y, z));
                    places.add(p);
                    if (p.role() == Role.CORE) core = p;
                }
        PLACES = List.copyOf(places);
        CORE = core;
    }

    private static Role role(int x, int y, int z) {
        if (x == 0 && y == LAYERS - 1 && z == 2) return Role.CORE;
        if (y == 1 && (x == 0 && Math.abs(z) == 2 || z == 0 && Math.abs(x) == 2)) return Role.CHAMBER_SOCKET;
        if (y == 0 && (Math.abs(x) == 2 || Math.abs(z) == 2)) return Role.PORT_SOCKET;
        return Role.CASING;
    }

    /** The reactor's right for someone facing the core, local +x. */
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Whether (x, y, z) of the reactor's own frame is one of its places (the corners of the 5 x 5 are not). */
    public static boolean isPlace(int x, int y, int z) {
        return y >= 0 && y < LAYERS && Math.abs(x) <= 2 && Math.abs(z) <= 2 && !(Math.abs(x) == 2 && Math.abs(z) == 2);
    }

    /** A step along the reactor's own frame (dx or dz of 1 or -1) as a direction of the world. */
    public static Direction direction(Direction facing, int dx, int dz) {
        return dz > 0 ? facing : dz < 0 ? facing.getOpposite() : dx > 0 ? right(facing) : right(facing).getOpposite();
    }

    /** Where a place stands for a core at {@code core} looking {@code facing}. */
    public static BlockPos pos(BlockPos core, Direction facing, Place p) {
        return at(core, facing, p.x(), p.y(), p.z());
    }

    /** Where the block at (x, y, z) of the reactor's own frame stands, a place or not. */
    public static BlockPos at(BlockPos core, Direction facing, int x, int y, int z) {
        return core.relative(right(facing), x).relative(facing, z - CORE.z()).above(y - CORE.y());
    }

    /** The core of a block that stands in place {@code p} of a reactor looking {@code facing}. */
    public static BlockPos coreOf(BlockPos pos, Direction facing, Place p) {
        return pos.relative(right(facing), -p.x()).relative(facing, CORE.z() - p.z()).above(CORE.y() - p.y());
    }

    /** The way a chamber socket looks out of the reactor. */
    public static Direction outward(Direction facing, Place p) {
        if (p.z() == 2) return facing;
        if (p.z() == -2) return facing.getOpposite();
        return p.x() > 0 ? right(facing) : right(facing).getOpposite();
    }

    /** The side of the active zone a chamber socket opens (DESIGN step 5): the grid's rows run from the back to the front. */
    public static Zone.Side side(Place p) {
        if (p.z() == 2) return Zone.Side.BOTTOM;
        if (p.z() == -2) return Zone.Side.TOP;
        return p.x() > 0 ? Zone.Side.RIGHT : Zone.Side.LEFT;
    }

    /** What a look at the blocks found: whether every place is right, the ports in the plinth and the chambers present. */
    public record Scan(boolean complete, List<BlockPos> ports, Set<Zone.Side> chambers) {

        static final Scan NONE = new Scan(false, List.of(), Set.of());
    }

    public static boolean isCasing(BlockState state) {
        return state.is(NuclearBlocks.NUCLEAR_CASING.get());
    }

    /** A frame block that this very reactor made: its place and its facing match, so a neighbour's frame never counts. */
    private static boolean isOwnFrame(BlockState state, Direction facing, Place p) {
        return state.is(NuclearBlocks.REACTOR_FRAME.get()) && state.getValue(ReactorFrameBlock.PART) == p.index()
                && state.getValue(ReactorFrameBlock.FACING) == facing;
    }

    private static boolean fits(BlockState state, Direction facing, Place p) {
        if (isCasing(state) || isOwnFrame(state, facing, p)) return p.role() != Role.CORE;
        return switch (p.role()) {
            case PORT_SOCKET -> state.getBlock() instanceof MultiblockPortBlock;
            case CHAMBER_SOCKET -> state.getBlock() instanceof ExpansionChamberBlock;
            default -> false;
        };
    }

    public static Scan scan(Level level, BlockPos core, Direction facing) {
        List<BlockPos> ports = new ArrayList<>();
        Set<Zone.Side> chambers = EnumSet.noneOf(Zone.Side.class);
        for (Place p : PLACES) {
            if (p == CORE) continue;
            BlockPos at = pos(core, facing, p);
            if (!level.isLoaded(at)) return Scan.NONE;
            BlockState s = level.getBlockState(at);
            if (!fits(s, facing, p)) return Scan.NONE;
            if (s.getBlock() instanceof MultiblockPortBlock) ports.add(at);
            if (s.getBlock() instanceof ExpansionChamberBlock) chambers.add(side(p));
        }
        return new Scan(true, ports, chambers);
    }

    /** The structure wand: casing into every empty place; the sockets get casing too, the ports and chambers are the player's. */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wand) {
        for (Place p : PLACES) {
            if (p == CORE) continue;
            BlockPos at = pos(core, facing, p);
            if (level.isLoaded(at) && !fits(level.getBlockState(at), facing, p))
                wand.place(level, at, NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState());
        }
    }

    /**
     * Asks every reactor core this block could belong to to look at its blocks again: a casing, a chamber or a port that
     * has just been placed may have finished a reactor (DESIGN step 16: an unformed reactor is checked when a block of it
     * is placed, not on a timer).
     */
    public static void notifyNear(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Place p : PLACES) {
            if (p == CORE) continue;
            for (Direction f : Direction.Plane.HORIZONTAL) {
                BlockPos c = coreOf(pos, f, p);
                if (level.isLoaded(c) && level.getBlockState(c).is(NuclearBlocks.REACTOR_CORE.get())
                        && level.getBlockState(c).getValue(ReactorCoreBlock.FACING) == f
                        && level.getBlockEntity(c) instanceof ReactorCoreBlockEntity core) core.requestCheck();
            }
        }
    }

    /** The core of a formed frame block, or null when it is gone. */
    @Nullable
    public static ReactorCoreBlockEntity coreOfFrame(Level level, BlockPos pos, BlockState frame) {
        Place p = PLACES.get(frame.getValue(ReactorFrameBlock.PART));
        return level.getBlockEntity(coreOf(pos, frame.getValue(ReactorFrameBlock.FACING), p)) instanceof ReactorCoreBlockEntity core ? core : null;
    }

    /**
     * The formed core a block of a reactor belongs to, found from the block states alone, so it works on the client too:
     * the core itself, a frame by its place, a formed chamber or a hidden port by the sockets it may stand in (a chamber
     * also by the way it looks out). {@code state} is the block's own state, which may already be gone from the world
     * while it is being broken. Null when there is none.
     */
    @Nullable
    public static BlockPos formedCore(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.is(NuclearBlocks.REACTOR_CORE.get())) return state.getValue(ReactorCoreBlock.FORMED) ? pos : null;
        if (state.is(NuclearBlocks.REACTOR_FRAME.get())) {
            Direction f = state.getValue(ReactorFrameBlock.FACING);
            BlockPos c = coreOf(pos, f, PLACES.get(state.getValue(ReactorFrameBlock.PART)));
            return isFormedCore(level, c, f) ? c : null;
        }
        boolean chamber = state.getBlock() instanceof ExpansionChamberBlock && state.getValue(ExpansionChamberBlock.FORMED);
        boolean port = state.getBlock() instanceof MultiblockPortBlock && state.getValue(MultiblockPortBlock.CONCEALED);
        if (!chamber && !port) return null;
        for (Place p : PLACES) {
            if (p.role() != (chamber ? Role.CHAMBER_SOCKET : Role.PORT_SOCKET)) continue;
            for (Direction f : Direction.Plane.HORIZONTAL) {
                if (chamber && outward(f, p) != state.getValue(ExpansionChamberBlock.FACING)) continue;
                BlockPos c = coreOf(pos, f, p);
                if (isFormedCore(level, c, f)) return c;
            }
        }
        return null;
    }

    private static boolean isFormedCore(BlockGetter level, BlockPos c, Direction facing) {
        BlockState s = level.getBlockState(c);
        return s.is(NuclearBlocks.REACTOR_CORE.get()) && s.getValue(ReactorCoreBlock.FACING) == facing && s.getValue(ReactorCoreBlock.FORMED);
    }

    /** The chambers a formed reactor has, from the block states (for the outline, on the client): the sides of the zone. */
    public static Set<Zone.Side> chambersOf(BlockGetter level, BlockPos core, Direction facing) {
        Set<Zone.Side> out = EnumSet.noneOf(Zone.Side.class);
        for (Place p : PLACES) {
            if (p.role() != Role.CHAMBER_SOCKET) continue;
            BlockState s = level.getBlockState(pos(core, facing, p));
            if (s.getBlock() instanceof ExpansionChamberBlock && s.getValue(ExpansionChamberBlock.FORMED)) out.add(side(p));
        }
        return out;
    }
}
