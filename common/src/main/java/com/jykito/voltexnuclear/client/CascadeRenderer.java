package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.cascade.CascadeCoreBlock;
import com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity;
import com.jykito.voltexnuclear.cascade.CascadeStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;


/**
 * The moving part of a formed cascade (DESIGN step 18): every column's rotor turns about its axis with a glowing slit that
 * shows the turning, the columns with different offsets. Started, the rotors run up and the slits light; stopped, they
 * run down and the slits go dark in steps. The rest of the cascade is in the block models; only where a hidden Voltex
 * port stands does this renderer draw the piece of the plinth or the cabinet in its block, with the port's own square
 * as its mark on the front and the back. Everything here comes from the block state and the game time: the server sends
 * nothing per frame.
 */
public class CascadeRenderer implements BlockEntityRenderer<CascadeCoreBlockEntity> {

    /** Turn of a rotor at full speed, radians a tick; the run up and down takes about a second and a half. */
    static final float SPEED = 0.4f;
    private static final float EASE = 0.035f;

    public CascadeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CascadeCoreBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !state.getValue(CascadeCoreBlock.FORMED)) return;
        Direction facing = state.getValue(CascadeCoreBlock.FACING);
        int columns = state.getValue(CascadeCoreBlock.COLUMNS);
        BlockPos core = be.getBlockPos();

        double time = level.getGameTime() + partialTick;
        float dt = be.clientTime < 0 ? 0 : (float) Math.max(0, Math.min(10, time - be.clientTime));
        be.clientTime = time;
        be.clientSpin += ((state.getValue(CascadeCoreBlock.LIT) ? 1 : 0) - be.clientSpin) * Math.min(1f, dt * EASE);
        be.clientAngle = (be.clientAngle + be.clientSpin * dt * SPEED) % (float) (Math.PI * 2);
        float spin = be.clientSpin, glow = spin > 0.66f ? 1f : spin > 0.33f ? 0.82f : spin > 0.05f ? 0.62f : 0f;
        renderLive(ps, buffers, facing, columns, be.clientAngle, glow,
                (c, r) -> level.getBlockState(CascadeStructure.cell(core, facing, c, r)).getBlock() instanceof MultiblockPortBlock port ? port : null,
                (c, r, side) -> LevelRenderer.getLightColor(level, CascadeStructure.cell(core, facing, c, r).relative(side)), overlay);
    }

    /** The port standing in a cell of the row, or null. */
    @FunctionalInterface
    interface CellPort {
        @Nullable MultiblockPortBlock at(int column, int row);
    }

    /** The light beside a face of a cell of the row: a hidden port is a solid block, the light inside it is dark. */
    @FunctionalInterface
    interface CellLight {
        int at(int column, int row, Direction side);
    }

    /**
     * The rotors with their slits, {@code glow} the slits' own light (0 dark), and the hidden ports with their marks; the
     * pose in the core block.
     */
    static void renderLive(PoseStack ps, MultiBufferSource buffers, Direction facing, int columns, float angle, float glow,
                           CellPort ports, CellLight light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 1 - CascadeStructure.ROWS, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(switch (facing) { case EAST -> 90f; case NORTH -> 180f; case WEST -> 270f; default -> 0f; }));

        // one buffer at a time: taking a buffer of another kind closes the one before
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.CASCADE.texture));
        for (int c = 1; c <= columns; c++) {
            int lt = light.at(c, 2, facing);
            rotor(ps, c, angle);
            LiveModel.CASCADE.render("rotor", ps.last(), body, lt, overlay, 1f);
            if (glow == 0) LiveModel.CASCADE.render("slit", ps.last(), body, lt, overlay, 0.3f);
            ps.popPose();
        }
        Direction row = CascadeStructure.row(facing);
        for (int c = 0; c <= columns; c++)
            for (int r = 0; r < CascadeStructure.ROWS - 1; r++) {
                if (!CascadeStructure.socket(c, r)) continue;
                MultiblockPortBlock port = ports.at(c, r);
                if (port == null) continue;
                int lt = light.at(c, r, facing);
                ps.pushPose();
                ps.translate(c - 0.5, r, 0);
                LiveModel.CASCADE.render(r == 0 ? "plinth" : r == 1 ? "cab_low" : "cab_mid", ps.last(), body, lt, overlay, 1f);
                // the mark on every face a pipe can come to: the front and the back, below the plinth, the ends of the row
                String mark = LiveModel.CASCADE.mark(port);
                mark(ps, body, mark, Axis.YP.rotationDegrees(0), lt, overlay);
                mark(ps, body, mark, Axis.YP.rotationDegrees(180), light.at(c, r, facing.getOpposite()), overlay);
                if (r == 0) mark(ps, body, mark, Axis.XP.rotationDegrees(90), light.at(c, r, Direction.DOWN), overlay);
                if (c == 0) mark(ps, body, mark, Axis.YP.rotationDegrees(270), light.at(c, r, row.getOpposite()), overlay);
                if (c == columns && r == 0) mark(ps, body, mark, Axis.YP.rotationDegrees(90), light.at(c, r, row), overlay);
                ps.popPose();
            }
        if (glow > 0) {
            VertexConsumer eyes = buffers.getBuffer(RenderType.eyes(LiveModel.CASCADE.texture));
            for (int c = 1; c <= columns; c++) {
                rotor(ps, c, angle);
                LiveModel.CASCADE.render("slit", ps.last(), eyes, LightTexture.FULL_BRIGHT, overlay, glow);
                ps.popPose();
            }
        }
        ps.popPose();
    }

    /** Pushes the pose of column c's rotor: the columns turn together, each a little behind the one before. */
    private static void rotor(PoseStack ps, int c, float angle) {
        ps.pushPose();
        ps.translate(c, 0, 0);
        ps.mulPose(Axis.YP.rotation(angle + 0.9f * c));
    }

    /**
     * A port's mark, made for the front face of a cell of the row (x 0..1, z -0.5..0.5), turned about the cell's centre
     * onto another face; only its outer front face is drawn (the model holds a back plate too, see LiveModel.renderSouth).
     */
    private static void mark(PoseStack ps, VertexConsumer vc, String mark, Quaternionf turn, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0);
        ps.mulPose(turn);
        ps.translate(-0.5, -0.5, 0);
        LiveModel.CASCADE.renderSouth(mark, ps.last(), vc, light, overlay, 1f);
        ps.popPose();
    }

    /** NeoForge 1.21.1 culls block entity renderers by this box; on Forge 1.20.1 the block entity answers instead. */
    public AABB getRenderBoundingBox(CascadeCoreBlockEntity be) {
        return be.renderBox();
    }
}
