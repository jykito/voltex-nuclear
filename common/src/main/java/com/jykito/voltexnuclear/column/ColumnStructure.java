package com.jykito.voltexnuclear.column;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.column.HeavyWaterColumn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the heavy water column (DESIGN steps 13 and 17, "Tiers" in blocks with the pillar C1 "Frame", the author's
 * pick on :8781/nuclear_column.html, 2026-10-28): a round tower of 4 to 8 tiers standing on the ground, a tier a layer of
 * 3 x 3 add-on casing; the core in the middle of the front side of the lowest tier. At the middle of the tower's right
 * side, as one faces the core, stands the pillar of the ports, 1 x 1 and 6 high from the ground: each of its blocks a
 * Voltex port or casing. The tiers are counted from the bottom while a layer is whole; the column forms with 4.
 * <p>
 * Cells are counted from the core: tier 0 the lowest, going up; across -1 left to 1 right; depth 0 the front (the
 * core's row) to 2 the back. The pillar stands across 2, depth 1.
 */
public final class ColumnStructure {

    private ColumnStructure() {}

    /** Blocks in the pillar of the ports. */
    public static final int PILLAR = 6;
    /** Where the pillar stands. */
    public static final int PILLAR_ACROSS = 2, PILLAR_DEPTH = 1;

    public enum Kind { TIER, PILLAR }

    /** One block of the column besides the core: its tier (the pillar's: its height from the ground) and its place in it. */
    public record Cell(BlockPos pos, Kind kind, int tier, int across, int depth) {}

    /** A look at the column: the tiers it has, whether the pillar is whole; when formed, its cells and the ports in the pillar. */
    public record Scan(int tiers, boolean pillar, List<Cell> cells, List<BlockPos> sockets) {

        static final Scan NONE = new Scan(0, false, List.of(), List.of());

        public boolean formed() {
            return pillar && tiers >= HeavyWaterColumn.MIN_TIERS;
        }
    }

    /**
     * The pieces of the column's model, as export_nuclear_column.js numbers them: a tower cell by the kind of its tier
     * (0 the lowest, 1 a middle one, 2 the top) and its place, kind * 9 + (across + 1) * 3 + depth, the core's own cell
     * being 3; the pillar's foot, a block with a pipe into the tower, a plain block, its top and its top with a pipe.
     */
    public static final int CORE_PIECE = 3, PILLAR_FOOT = 27, PILLAR_PIPE = 28, PILLAR_PLAIN = 29, PILLAR_TOP = 30, PILLAR_TOP_PIPE = 31;

    /** The piece a block of a column of this many tiers shows. */
    public static int piece(Cell c, int tiers) {
        if (c.kind() == Kind.PILLAR) return pillarPiece(c.tier(), tiers);
        int kind = c.tier() == 0 ? 0 : c.tier() == tiers - 1 ? 2 : 1;
        return kind * 9 + (c.across() + 1) * 3 + c.depth();
    }

    /** The pillar's piece at this height: the hot pipe enters the lowest tier but one, the coolant's the upper half. */
    public static int pillarPiece(int up, int tiers) {
        int cold = Math.min(tiers, PILLAR) - 1;
        if (up == 0) return PILLAR_FOOT;
        if (up == PILLAR - 1) return up == cold ? PILLAR_TOP_PIPE : PILLAR_TOP;
        return up == 1 || up == cold ? PILLAR_PIPE : PILLAR_PLAIN;
    }

    /** The way across runs: to the right of someone facing the core. */
    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cell(BlockPos core, Direction facing, int tier, int across, int depth) {
        return core.relative(right(facing), across).relative(facing.getOpposite(), depth).above(tier);
    }

    public static BlockPos pillar(BlockPos core, Direction facing, int up) {
        return cell(core, facing, up, PILLAR_ACROSS, PILLAR_DEPTH);
    }

    public static boolean isCasing(BlockState state) {
        return state.is(NuclearBlocks.NUCLEAR_CASING.get());
    }

    private static boolean isSocket(BlockState state) {
        return isCasing(state) || state.getBlock() instanceof MultiblockPortBlock;
    }

    private static boolean isCore(int tier, int across, int depth) {
        return tier == 0 && across == 0 && depth == 0;
    }

    /** The cells of a whole tier, or null when a block of it is missing or wrong (or not loaded). */
    private static List<Cell> tier(Level level, BlockPos core, Direction facing, int t) {
        List<Cell> cells = new ArrayList<>(9);
        for (int a = -1; a <= 1; a++)
            for (int d = 0; d < 3; d++) {
                if (isCore(t, a, d)) continue;
                BlockPos p = cell(core, facing, t, a, d);
                if (!level.isLoaded(p) || !isCasing(level.getBlockState(p))) return null;
                cells.add(new Cell(p, Kind.TIER, t, a, d));
            }
        return cells;
    }

    public static Scan scan(Level level, BlockPos core, Direction facing) {
        List<Cell> tower = new ArrayList<>();
        int tiers = 0;
        for (int t = 0; t < HeavyWaterColumn.MAX_TIERS; t++) {
            List<Cell> layer = tier(level, core, facing, t);
            if (layer == null) break;
            tower.addAll(layer);
            tiers++;
        }
        List<Cell> pillar = new ArrayList<>(PILLAR);
        List<BlockPos> sockets = new ArrayList<>();
        for (int up = 0; up < PILLAR; up++) {
            BlockPos p = pillar(core, facing, up);
            if (!level.isLoaded(p)) return Scan.NONE;
            BlockState s = level.getBlockState(p);
            if (!isSocket(s)) return new Scan(tiers, false, List.of(), List.of());
            pillar.add(new Cell(p, Kind.PILLAR, up, PILLAR_ACROSS, PILLAR_DEPTH));
            if (s.getBlock() instanceof MultiblockPortBlock) sockets.add(p);
        }
        if (tiers < HeavyWaterColumn.MIN_TIERS) return new Scan(tiers, true, List.of(), List.of());
        tower.addAll(pillar);
        return new Scan(tiers, true, tower, sockets);
    }

    /**
     * The structure wand: the pillar and every tier up to the highest the player has started (a casing in its layer), at
     * least the lowest column. Cells already right are skipped; a port counts as casing in the pillar.
     */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wand) {
        int tiers = HeavyWaterColumn.MIN_TIERS;
        for (int t = 1; t < HeavyWaterColumn.MAX_TIERS; t++)
            for (int a = -1; a <= 1; a++)
                for (int d = 0; d < 3; d++)
                    if (isCasing(level.getBlockState(cell(core, facing, t, a, d)))) tiers = Math.max(tiers, t + 1);
        BlockState casing = NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState();
        for (int t = 0; t < tiers; t++)
            for (int a = -1; a <= 1; a++)
                for (int d = 0; d < 3; d++) {
                    BlockPos p = cell(core, facing, t, a, d);
                    if (!isCore(t, a, d) && level.isLoaded(p) && !isCasing(level.getBlockState(p))) wand.place(level, p, casing);
                }
        for (int up = 0; up < PILLAR; up++) {
            BlockPos p = pillar(core, facing, up);
            if (level.isLoaded(p) && !isSocket(level.getBlockState(p))) wand.place(level, p, casing);
        }
    }

    /** The core a formed block of the column belongs to: one whose cells hold this block. Null when there is none. */
    public static BlockPos findCore(Level level, BlockPos part, Direction facing) {
        for (int t = 0; t < HeavyWaterColumn.MAX_TIERS; t++) {
            for (int a = -1; a <= 1; a++)
                for (int d = 0; d < 3; d++)
                    if (claimedBy(level, part, from(part, facing, t, a, d))) return from(part, facing, t, a, d);
            if (t < PILLAR && claimedBy(level, part, from(part, facing, t, PILLAR_ACROSS, PILLAR_DEPTH)))
                return from(part, facing, t, PILLAR_ACROSS, PILLAR_DEPTH);
        }
        return null;
    }

    /** Where the core is if this block stands in the cell (tier, across, depth). */
    private static BlockPos from(BlockPos part, Direction facing, int tier, int across, int depth) {
        return part.relative(right(facing), -across).relative(facing.getOpposite(), -depth).below(tier);
    }

    private static boolean claimedBy(Level level, BlockPos part, BlockPos core) {
        return level.getBlockEntity(core) instanceof ColumnCoreBlockEntity be && be.claims(part);
    }
}
