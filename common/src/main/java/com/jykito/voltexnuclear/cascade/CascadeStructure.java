package com.jykito.voltexnuclear.cascade;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.cascade.Cascade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the centrifuge cascade "On a plinth" (DESIGN steps 3 and 18; the author reworked the K1 rail on 10-25 so
 * the ports have blocks of their own): a row of 2 to 12 columns runs to the right of someone facing the core, all on a
 * plinth of whole blocks. A column is four blocks: the plinth, casing, a centrifuge (its ring), casing (the collector).
 * The cabinet at the left end is the plinth, two blocks of casing and the core on top.
 * <p>
 * A Voltex port can stand in for any block of the plinth and of the cabinet, and only there: pipes and cables reach it
 * from the front, the back or below and never the turning columns. It is hidden while the cascade stands and wears the
 * machine's casing with its own square as a mark.
 * <p>
 * Cells are counted from the core: column 0 is the cabinet, row 0 the plinth, row 3 the level of the core.
 */
public final class CascadeStructure {

    private CascadeStructure() {}

    public static final int ROWS = 4;

    /** One block of the structure. */
    public record Cell(BlockPos pos, int column, int row) {

        /** What a casing block in this cell becomes. */
        public CasingRole role() {
            if (row == 0) return CasingRole.PLINTH;
            if (column == 0) return row == 1 ? CasingRole.CABINET_LOW : CasingRole.CABINET_MID;
            return row == 1 ? CasingRole.COLUMN : CasingRole.COLLECTOR;
        }
    }

    /**
     * A look at the row: the whole columns behind a whole cabinet, counted until the first broken one. Only a formed
     * cascade (two columns and more) claims its cells and sockets.
     */
    public record Scan(int columns, List<Cell> cells, List<BlockPos> sockets) {

        public boolean formed() {
            return columns >= Cascade.MIN_COLUMNS;
        }
    }

    /** The way the row runs from the core. */
    public static Direction row(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cell(BlockPos core, Direction facing, int column, int row) {
        return core.relative(row(facing), column).below(ROWS - 1 - row);
    }

    /** Whether a port may stand in this cell: the plinth and the cabinet. */
    public static boolean socket(int column, int row) {
        return row == 0 || column == 0;
    }

    public static boolean isCasing(BlockState state) {
        return state.is(NuclearBlocks.NUCLEAR_CASING.get());
    }

    /** Casing, or a Voltex port in its place. */
    public static boolean isSocket(BlockState state) {
        return isCasing(state) || state.getBlock() instanceof MultiblockPortBlock;
    }

    public static boolean isCentrifuge(BlockState state) {
        return state.is(NuclearBlocks.CENTRIFUGE.get());
    }

    /** Whether a block fits a cell: a centrifuge in the ring row of a column, a socket in a socket, casing elsewhere. */
    private static boolean fits(BlockState state, int column, int row) {
        if (column > 0 && row == 2) return isCentrifuge(state);
        return socket(column, row) ? isSocket(state) : isCasing(state);
    }

    public static Scan scan(Level level, BlockPos core, Direction facing) {
        List<Cell> cells = new ArrayList<>();
        for (int r = 0; r < ROWS - 1; r++) {
            BlockPos p = cell(core, facing, 0, r);
            if (!level.isLoaded(p) || !fits(level.getBlockState(p), 0, r)) return new Scan(0, List.of(), List.of());
            cells.add(new Cell(p, 0, r));
        }
        int columns = 0;
        columns:
        for (int c = 1; c <= Cascade.MAX_COLUMNS; c++) {
            List<Cell> column = new ArrayList<>(ROWS);
            for (int r = 0; r < ROWS; r++) {
                BlockPos p = cell(core, facing, c, r);
                if (!level.isLoaded(p) || !fits(level.getBlockState(p), c, r)) break columns;
                column.add(new Cell(p, c, r));
            }
            cells.addAll(column);
            columns = c;
        }
        if (columns < Cascade.MIN_COLUMNS) return new Scan(columns, List.of(), List.of());
        List<BlockPos> sockets = new ArrayList<>();
        for (Cell c : cells) if (level.getBlockState(c.pos()).getBlock() instanceof MultiblockPortBlock) sockets.add(c.pos());
        return new Scan(columns, cells, sockets);
    }

    /**
     * The structure wand: the cabinet and every column up to the farthest one the player has started (a casing, a centrifuge
     * or a port in its cells), at least the shortest row. Cells already right are skipped; a port counts as casing.
     */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wand) {
        int length = Cascade.MIN_COLUMNS;
        for (int c = 1; c <= Cascade.MAX_COLUMNS; c++)
            for (int r = 0; r < ROWS; r++) {
                BlockState s = level.getBlockState(cell(core, facing, c, r));
                if (isSocket(s) || isCentrifuge(s)) length = Math.max(length, c);
            }
        for (int r = 0; r < ROWS - 1; r++) place(level, core, facing, 0, r, wand);
        for (int c = 1; c <= length; c++)
            for (int r = 0; r < ROWS; r++) place(level, core, facing, c, r, wand);
    }

    private static void place(Level level, BlockPos core, Direction facing, int c, int r, WandBuild wand) {
        BlockPos p = cell(core, facing, c, r);
        if (!level.isLoaded(p) || fits(level.getBlockState(p), c, r)) return;
        wand.place(level, p, (c > 0 && r == 2 ? NuclearBlocks.CENTRIFUGE : NuclearBlocks.NUCLEAR_CASING).get().defaultBlockState());
    }

    /**
     * The core a formed casing or centrifuge belongs to: it stands at the left end of the row, on the core level, and its
     * scan holds this block. Null when there is none.
     */
    public static BlockPos findCore(Level level, BlockPos part, Direction facing) {
        Direction back = row(facing).getOpposite();
        for (int c = 0; c <= Cascade.MAX_COLUMNS; c++)
            for (int up = 0; up < ROWS; up++) {
                BlockPos p = part.relative(back, c).above(up);
                if (level.getBlockEntity(p) instanceof CascadeCoreBlockEntity core && core.claims(part)) return p;
            }
        return null;
    }
}
