package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.cascade.CasingRole;
import com.jykito.voltexnuclear.cascade.NuclearCasingBlock;
import com.jykito.voltexnuclear.column.ColumnCoreBlock;
import com.jykito.voltexnuclear.column.ColumnCoreBlockEntity;
import com.jykito.voltexnuclear.column.ColumnStructure;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.column.HeavyWaterColumn;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.ToIntFunction;

/**
 * The hidden ports of a formed heavy water column (DESIGN step 18): the column stands still and is all in the block
 * models; where a Voltex port hides in the pillar this renderer draws the pillar's piece for that height and the port's
 * square on its three free sides, out, front and back, as the plant and the reactor draw theirs. The pillar's piece
 * follows the tiers (the coolant pipe enters the upper half), counted from the casing's roles the client already has.
 */
public class ColumnRenderer implements BlockEntityRenderer<ColumnCoreBlockEntity> {

    public ColumnRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ColumnCoreBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !state.getValue(ColumnCoreBlock.FORMED)) return;
        Direction facing = state.getValue(ColumnCoreBlock.FACING);
        BlockPos core = be.getBlockPos();
        int tiers = tiers(level, core, facing);
        for (int up = 0; up < ColumnStructure.PILLAR; up++) {
            BlockPos at = ColumnStructure.pillar(core, facing, up);
            BlockState s = level.getBlockState(at);
            if (!(s.getBlock() instanceof MultiblockPortBlock port) || !s.getValue(MultiblockPortBlock.CONCEALED)) continue;
            BlockPos rel = at.subtract(core);
            ps.pushPose();
            ps.translate(rel.getX(), rel.getY(), rel.getZ());
            renderPort(ps, buffers, facing, ColumnStructure.pillarPiece(up, tiers), port, side -> LevelRenderer.getLightColor(level, at.relative(side)), overlay);
            ps.popPose();
        }
    }

    /** The tiers of the formed column: its casing by the pillar, counted up from the ground while it holds the column's role. */
    private static int tiers(Level level, BlockPos core, Direction facing) {
        int t = 0;
        while (t < HeavyWaterColumn.MAX_TIERS) {
            BlockState s = level.getBlockState(ColumnStructure.cell(core, facing, t, 1, ColumnStructure.PILLAR_DEPTH));
            if (!s.is(NuclearBlocks.NUCLEAR_CASING.get()) || s.getValue(NuclearCasingBlock.ROLE) != CasingRole.HEAVY_COLUMN) break;
            t++;
        }
        return t;
    }

    /**
     * A hidden port of the pillar, the pose in its block: the pillar's piece, and the port's square out, in front and behind.
     * {@code light} gives the light beside a face: a hidden port is a solid block, dark inside.
     */
    private static void renderPort(PoseStack ps, MultiBufferSource buffers, Direction facing, int piece, MultiblockPortBlock port,
                                   ToIntFunction<Direction> light, int overlay) {
        Direction out = ColumnStructure.right(facing);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState()
                .setValue(NuclearCasingBlock.ROLE, CasingRole.HEAVY_COLUMN).setValue(NuclearCasingBlock.PIECE, piece)
                .setValue(NuclearCasingBlock.FACING, facing), ps, buffers, light.applyAsInt(out), overlay);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.COLUMN.texture));
        String mark = LiveModel.COLUMN.mark(port);
        for (Direction side : new Direction[]{out, facing, facing.getOpposite()})
            LiveModel.COLUMN.renderMark(mark, ps, vc, Axis.YP.rotationDegrees(LiveModel.turn(side)), light.applyAsInt(side), overlay);
    }

    /** NeoForge 1.21.1 culls block entity renderers by this box; on Forge 1.20.1 the block entity answers instead. */
    public AABB getRenderBoundingBox(ColumnCoreBlockEntity be) {
        return be.renderBox();
    }
}
