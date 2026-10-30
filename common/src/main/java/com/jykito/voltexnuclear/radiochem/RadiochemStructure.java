package com.jykito.voltexnuclear.radiochem;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the radiochemical plant, R3 "Cell and tanks" (DESIGN steps 13 and 17): 5 blocks wide, 3 deep, 3 high. The
 * hot cell takes the three columns from the core's, the two solvent tanks the two to its right as one faces the core.
 * The core stands in the left pillar of the cell, on the middle level at the front; the thick glass of the cell's window
 * fills the two front blocks next to it. Everything else is the add-on casing.
 * <p>
 * A Voltex port can stand in for any block of the bottom level, the plinth, and only there, as on the cascade: pipes and
 * cables reach it from the front, the back, the sides or below.
 * <p>
 * Cells are counted from the core: column 0 is the core's, going right; depth 0 the front, going back; level 0 the
 * bottom, the core on level 1.
 */
public final class RadiochemStructure {

    private RadiochemStructure() {}

    public static final int COLUMNS = 5, DEPTH = 3, LEVELS = 3;
    /** Where the core stands. */
    public static final int CORE_COLUMN = 0, CORE_DEPTH = 0, CORE_LEVEL = 1;

    public enum Kind { CORE, GLASS, SOCKET, CASING }

    /** One block of the structure besides the core, and where it stands. */
    public record Cell(BlockPos pos, Kind kind, int column, int depth, int level) {

        /** The piece of the plant's model this cell holds, as export_nuclear_radiochem.js numbers them. */
        public int index() {
            return RadiochemStructure.index(column, depth, level);
        }
    }

    public static int index(int column, int depth, int level) {
        return column * DEPTH * LEVELS + depth * LEVELS + level;
    }

    /** A look at the plant: formed when every block is in place; then its cells and the ports in its plinth. */
    public record Scan(boolean formed, List<Cell> cells, List<BlockPos> sockets) {

        static final Scan NONE = new Scan(false, List.of(), List.of());
    }

    /** The way the columns run from the core: to the right of someone facing it. */
    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cell(BlockPos core, Direction facing, int column, int depth, int level) {
        return core.relative(right(facing), column).relative(facing.getOpposite(), depth).above(level - CORE_LEVEL);
    }

    public static Kind kind(int column, int depth, int level) {
        if (column == CORE_COLUMN && depth == CORE_DEPTH && level == CORE_LEVEL) return Kind.CORE;
        if (depth == 0 && level == CORE_LEVEL && (column == 1 || column == 2)) return Kind.GLASS;
        return level == 0 ? Kind.SOCKET : Kind.CASING;
    }

    public static boolean isCasing(BlockState state) {
        return state.is(NuclearBlocks.NUCLEAR_CASING.get());
    }

    public static boolean isGlass(BlockState state) {
        return state.is(NuclearBlocks.HOT_CELL_GLASS.get());
    }

    private static boolean fits(BlockState state, Kind kind) {
        return switch (kind) {
            case CORE -> true;
            case GLASS -> isGlass(state);
            case SOCKET -> isCasing(state) || state.getBlock() instanceof MultiblockPortBlock;
            case CASING -> isCasing(state);
        };
    }

    public static Scan scan(Level level, BlockPos core, Direction facing) {
        List<Cell> cells = new ArrayList<>();
        List<BlockPos> sockets = new ArrayList<>();
        for (int c = 0; c < COLUMNS; c++)
            for (int d = 0; d < DEPTH; d++)
                for (int l = 0; l < LEVELS; l++) {
                    Kind kind = kind(c, d, l);
                    if (kind == Kind.CORE) continue;
                    BlockPos p = cell(core, facing, c, d, l);
                    if (!level.isLoaded(p)) return Scan.NONE;
                    BlockState s = level.getBlockState(p);
                    if (!fits(s, kind)) return Scan.NONE;
                    cells.add(new Cell(p, kind, c, d, l));
                    if (s.getBlock() instanceof MultiblockPortBlock) sockets.add(p);
                }
        return new Scan(true, cells, sockets);
    }

    /** The structure wand: casing and the glass wherever a block is missing or wrong; a port counts as casing. */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wand) {
        for (int c = 0; c < COLUMNS; c++)
            for (int d = 0; d < DEPTH; d++)
                for (int l = 0; l < LEVELS; l++) {
                    Kind kind = kind(c, d, l);
                    BlockPos p = cell(core, facing, c, d, l);
                    if (kind == Kind.CORE || !level.isLoaded(p) || fits(level.getBlockState(p), kind)) continue;
                    wand.place(level, p, (kind == Kind.GLASS ? NuclearBlocks.HOT_CELL_GLASS : NuclearBlocks.NUCLEAR_CASING).get().defaultBlockState());
                }
    }

    /** The core a formed block of the plant belongs to: one whose cells hold this block. Null when there is none. */
    public static BlockPos findCore(Level level, BlockPos part, Direction facing) {
        for (int c = 0; c < COLUMNS; c++)
            for (int d = 0; d < DEPTH; d++)
                for (int l = 0; l < LEVELS; l++) {
                    // the core is where this block would be if it stood in cell (c, d, l)
                    BlockPos core = part.relative(right(facing), -c).relative(facing.getOpposite(), -d).above(CORE_LEVEL - l);
                    if (level.getBlockEntity(core) instanceof RadiochemCoreBlockEntity be && be.claims(part)) return core;
                }
        return null;
    }
}
