package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.fusion.Fusion;
import com.jykito.voltexnuclear.core.fusion.Torus.Phase;
import com.jykito.voltexnuclear.fusion.TorusCoreBlock;
import com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity;
import com.jykito.voltexnuclear.fusion.TorusFrameBlock;
import com.jykito.voltexnuclear.fusion.TorusStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * The live parts of a formed fusion torus (DESIGN steps 18 and 19, as the torus page animates them): the 16 windows of
 * plasma, 8 boxes 45 degrees apart; a glowing band on the outer face of each of the 8 coils; the solenoid's band; and where
 * a hidden Voltex port stands in a stand, the stand's piece in its block (the very model of the frame block in that place)
 * with the port's own square on the stand's two outer sides. The rest of the torus is in the block models, the lit face
 * of a burning core too.
 * <p>
 * The light comes in steps, with no smooth change. Igniting, the windows light round one by one; burning, a clot of plasma
 * runs round with a tail of three steps and the coils it passes flash; cooling, the windows blink; out, all smoulder. The
 * phase and the start of the ignition come from the core's update packets, sent only when the phase changes; the clot
 * and the blinking come from the game's time.
 */
public class TorusRenderer implements BlockEntityRenderer<TorusCoreBlockEntity> {

    /** The lowest step of the glow: a window that barely smoulders. */
    private static final float SMOULDER = 0.22f;
    /** The clot goes round 6 windows of 8 a second; the blinking changes 4 times a second; in ticks of the game. */
    private static final float CLOT_PER_TICK = 0.3f;
    private static final long BLINK_STEP = 5;

    private final BlockRenderDispatcher blocks;

    public TorusRenderer(BlockEntityRendererProvider.Context ctx) {
        blocks = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(TorusCoreBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !state.getValue(TorusCoreBlock.FORMED)) return;
        Direction facing = state.getValue(TorusCoreBlock.FACING);
        BlockPos core = be.getBlockPos();

        for (TorusStructure.Place p : TorusStructure.PLACES) {
            if (p.role() != TorusStructure.Role.PORT_SOCKET) continue;
            BlockPos at = TorusStructure.pos(core, facing, p);
            BlockState s = level.getBlockState(at);
            if (!(s.getBlock() instanceof MultiblockPortBlock port) || !s.getValue(MultiblockPortBlock.CONCEALED)) continue;
            ps.pushPose();
            ps.translate(at.getX() - core.getX(), at.getY() - core.getY(), at.getZ() - core.getZ());
            renderPort(ps, buffers, blocks, facing, p, port, LiveModel.lightAround(level, at),
                    side -> LevelRenderer.getLightColor(level, at.relative(side)), overlay);
            ps.popPose();
        }

        long time = level.getGameTime();
        renderLive(ps, buffers, facing, be.clientPhase, time, partialTick, time - be.clientHeatStart,
                LevelRenderer.getLightColor(level, core.relative(facing)), overlay);
    }

    /**
     * A hidden port in a stand, the pose in its block: the stand's piece and the port's mark on its two outer sides.
     * {@code pieceLight} is the light around the block ({@link LiveModel#lightAround}), {@code light} the light beside a face for
     * its mark: a hidden port is a solid block, dark inside.
     */
    private static void renderPort(PoseStack ps, MultiBufferSource buffers, BlockRenderDispatcher blocks, Direction facing, TorusStructure.Place p,
                                   MultiblockPortBlock port, int pieceLight, ToIntFunction<Direction> light, int overlay) {
        Direction[] out = TorusStructure.outward(facing, p);
        blocks.renderSingleBlock(NuclearBlocks.TORUS_FRAME.get().defaultBlockState().setValue(TorusFrameBlock.PART, p.index())
                .setValue(TorusFrameBlock.FACING, facing), ps, buffers, pieceLight, overlay);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.TORUS.texture));
        String mark = LiveModel.TORUS.mark(port);
        for (Direction side : out)
            LiveModel.TORUS.renderMark(mark, ps, vc, Axis.YP.rotationDegrees(LiveModel.turn(side)), light.applyAsInt(side), overlay);
    }

    /**
     * The windows, the coils' bands and the solenoid's band, the pose in the core block; {@code heating} the ticks since the
     * ignition started. Each is drawn dim in the world's light, and once more glowing at its step of light.
     */
    static void renderLive(PoseStack ps, MultiBufferSource buffers, Direction facing, Phase phase, long time, float partialTick,
                           long heating, int light, int overlay) {
        // in the model's own coordinates, its axis through the middle of the footprint: the core stands at (0, 1, 3)
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(LiveModel.turn(facing)));
        ps.translate(0, -TorusStructure.CORE.y(), -TorusStructure.CORE.z());
        LiveModel model = LiveModel.TORUS;

        int head = (int) ((time + partialTick) * CLOT_PER_TICK) % 8, lit = (int) (heating * 8 / Fusion.IGNITION_TICKS);
        boolean blink = time / BLINK_STEP % 2 == 0;
        // the windows 0..7, the bands 8..15
        float[] glow = new float[16], dim = new float[16];
        Arrays.fill(dim, SMOULDER);
        for (int k = 0; k < 8; k++) {
            int d = (head - k + 8) % 8;
            glow[k] = switch (phase) {
                case HEAT -> k < lit ? 0.62f : k == lit && blink ? 0.82f : SMOULDER;
                case LIT -> d == 0 ? 1f : d == 1 ? 0.82f : d == 2 ? 0.62f : 0.45f;
                case COOL -> blink ? 0.62f : SMOULDER;
                default -> SMOULDER;
            };
            glow[k + 8] = switch (phase) {
                case LIT -> head == k || head == (k + 1) % 8 ? 1f : 0.35f;
                case HEAT, COOL -> 0.35f;
                default -> SMOULDER;
            };
        }
        float solenoid = switch (phase) {
            case LIT -> 1f;
            case HEAT -> 0.62f;
            case COOL -> blink ? 0.62f : SMOULDER;
            default -> SMOULDER;
        };

        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        draw(ps, model, body, light, overlay, dim, SMOULDER, false);
        VertexConsumer eyes = buffers.getBuffer(RenderType.eyes(model.texture));
        draw(ps, model, eyes, LightTexture.FULL_BRIGHT, overlay, glow, solenoid, true);
        ps.popPose();
    }

    /**
     * The windows (shade 0..7), the bands (8..15) and the solenoid's band; in the glowing pass a part at the smoulder step
     * is left to the dim pass.
     */
    private static void draw(PoseStack ps, LiveModel model, VertexConsumer vc, int light, int overlay, float[] shade, float solenoid,
                             boolean glowing) {
        for (int k = 0; k < 16; k++) {
            float v = shade[k];
            if (glowing && v <= SMOULDER) continue;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(k < 8 ? k * 45f : 22.5f + (k - 8) * 45f));
            model.render(k < 8 ? "window" : "band", ps.last(), vc, light, overlay, v);
            ps.popPose();
        }
        if (!glowing || solenoid > SMOULDER) model.render("solenoid", ps.last(), vc, light, overlay, solenoid);
    }

    /** NeoForge 1.21.1 culls block entity renderers by this box; on Forge 1.20.1 the block entity answers instead. */
    public AABB getRenderBoundingBox(TorusCoreBlockEntity be) {
        return be.renderBox();
    }
}
