package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlock;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity;
import com.jykito.voltexnuclear.starforge.StarForgeFrameBlock;
import com.jykito.voltexnuclear.starforge.StarForgeStructure;
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

import java.util.function.ToIntFunction;

/**
 * The live parts of a formed star forge (DESIGN step 28, as the forge page animates them): the star hanging in its crown
 * and the dome's two gold bands; and where a hidden Voltex port stands in a stand, the stand's piece in its block (the
 * very model of the frame block in that place) with the port's own square on the stand's two outer sides. The rest of the
 * forge is in the block models, the lit face of a melting core too.
 * <p>
 * While the forge melts (its core lit) the star turns slowly and bobs in its crown, its light flickering in steps, and the
 * bands pulse in two steps; otherwise both smoulder and the star stands still. The light comes from the core's block state,
 * the motion from the game's time.
 */
public class StarForgeRenderer implements BlockEntityRenderer<StarForgeCoreBlockEntity> {

    /** The lowest step of the glow: a star and bands that barely smoulder. */
    private static final float SMOULDER = 0.22f;
    /** The star turns 1.2 radians a second and bobs 0.06 of a block twice a second's worth of radians, as on the page. */
    private static final float TURN_PER_TICK = 1.2f / 20, BOB = 0.06f, BOB_PER_TICK = 2f / 20;
    private static final float[] STAR_STEPS = {1f, 0.82f, 1f, 0.9f};

    private final BlockRenderDispatcher blocks;

    public StarForgeRenderer(BlockEntityRendererProvider.Context ctx) {
        blocks = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(StarForgeCoreBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !state.getValue(StarForgeCoreBlock.FORMED)) return;
        Direction facing = state.getValue(StarForgeCoreBlock.FACING);
        BlockPos core = be.getBlockPos();

        for (StarForgeStructure.Place p : StarForgeStructure.PLACES) {
            if (p.role() != StarForgeStructure.Role.PORT_SOCKET) continue;
            BlockPos at = StarForgeStructure.pos(core, facing, p);
            BlockState s = level.getBlockState(at);
            if (!(s.getBlock() instanceof MultiblockPortBlock port) || !s.getValue(MultiblockPortBlock.CONCEALED)) continue;
            ps.pushPose();
            ps.translate(at.getX() - core.getX(), at.getY() - core.getY(), at.getZ() - core.getZ());
            renderPort(ps, buffers, blocks, facing, p, port, LiveModel.lightAround(level, at),
                    side -> LevelRenderer.getLightColor(level, at.relative(side)), overlay);
            ps.popPose();
        }

        // the light over the top of the dome, where the star hangs
        BlockPos over = core.relative(facing, -StarForgeStructure.CORE.z()).above(StarForgeStructure.LAYERS - StarForgeStructure.CORE.y());
        int lt = LevelRenderer.getLightColor(level, over);
        renderLive(ps, buffers, facing, state.getValue(StarForgeCoreBlock.LIT), level.getGameTime() + partialTick, lt, overlay);
    }

    /**
     * A hidden port in a stand, the pose in its block: the stand's piece and the port's mark on its two outer sides.
     * {@code pieceLight} is the light around the block ({@link LiveModel#lightAround}), {@code light} the light beside a face for
     * its mark: a hidden port is a solid block, dark inside.
     */
    private static void renderPort(PoseStack ps, MultiBufferSource buffers, BlockRenderDispatcher blocks, Direction facing, StarForgeStructure.Place p,
                                   MultiblockPortBlock port, int pieceLight, ToIntFunction<Direction> light, int overlay) {
        Direction[] out = StarForgeStructure.outward(facing, p);
        blocks.renderSingleBlock(NuclearBlocks.STAR_FORGE_FRAME.get().defaultBlockState().setValue(StarForgeFrameBlock.PART, p.index())
                .setValue(StarForgeFrameBlock.FACING, facing), ps, buffers, pieceLight, overlay);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.STAR_FORGE.texture));
        String mark = LiveModel.STAR_FORGE.mark(port);
        for (Direction side : out)
            LiveModel.STAR_FORGE.renderMark(mark, ps, vc, Axis.YP.rotationDegrees(LiveModel.turn(side)), light.applyAsInt(side), overlay);
    }

    /**
     * The star and the bands, the pose in the core block; {@code time} the game's ticks with the partial one. Each is drawn
     * dim in the world's light, and while the forge melts once more glowing at its step of light.
     */
    static void renderLive(PoseStack ps, MultiBufferSource buffers, Direction facing, boolean lit, float time, int light, int overlay) {
        // in the model's own coordinates, its axis through the middle of the footprint: the core stands at (0, 0, 2)
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(LiveModel.turn(facing)));
        ps.translate(0, -StarForgeStructure.CORE.y(), -StarForgeStructure.CORE.z());
        LiveModel model = LiveModel.STAR_FORGE;

        float star = lit ? STAR_STEPS[(int) (time * 6 / 20) % 4] : SMOULDER;
        float bands = lit ? ((int) (time * 2 / 20) % 2 == 1 ? 1f : 0.82f) : SMOULDER;

        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        draw(ps, model, body, light, overlay, SMOULDER, SMOULDER, lit, time);
        if (lit) {
            VertexConsumer eyes = buffers.getBuffer(RenderType.eyes(model.texture));
            draw(ps, model, eyes, LightTexture.FULL_BRIGHT, overlay, star, bands, true, time);
        }
        ps.popPose();
    }

    /** The bands as they stand, the star turned and lifted about its pivot while the forge melts. */
    private static void draw(PoseStack ps, LiveModel model, VertexConsumer vc, int light, int overlay, float star, float bands, boolean moving,
                             float time) {
        model.render("band", ps.last(), vc, light, overlay, bands);
        float[] pivot = model.pivot("star");
        ps.pushPose();
        ps.translate(pivot[0], pivot[1] + (moving ? BOB * (float) Math.sin(time * BOB_PER_TICK) : 0), pivot[2]);
        if (moving) ps.mulPose(Axis.YP.rotation(time * TURN_PER_TICK));
        ps.translate(-pivot[0], -pivot[1], -pivot[2]);
        model.render("star", ps.last(), vc, light, overlay, star);
        ps.popPose();
    }

    /** NeoForge 1.21.1 culls block entity renderers by this box; on Forge 1.20.1 the block entity answers instead. */
    public AABB getRenderBoundingBox(StarForgeCoreBlockEntity be) {
        return be.renderBox();
    }
}
