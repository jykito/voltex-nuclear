package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.cascade.CasingRole;
import com.jykito.voltexnuclear.cascade.NuclearCasingBlock;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlock;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity;
import com.jykito.voltexnuclear.radiochem.RadiochemStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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

import java.util.function.ToIntFunction;

/**
 * The live parts of a formed radiochemical plant (DESIGN step 18, R3 as the machines page animates it): while the plant
 * works the tongs in the hot cell swing over the table and dip to the pellet, and the handle outside the wall repeats them
 * (a master-slave manipulator); stopped, they ease to rest where they are. The pellet glows and flickers in steps while
 * the plant works and goes dark when it stops. The level lamp on the tanks stands as high as the water in the plant.
 * The rest of the plant is in the block models; where a hidden Voltex port stands this renderer draws the casing's piece
 * for that cell and the port's mark on its outer faces, as the reactor does. Everything comes from the block states and
 * the game time: the server sends nothing per frame.
 */
public class RadiochemRenderer implements BlockEntityRenderer<RadiochemCoreBlockEntity> {

    /** A swing, in turns a tick (one in about 3.3 seconds), its sway in radians, the dip in blocks. */
    static final float SPEED = 0.015f, SWAY = 0.18f, DIP = 0.12f;
    /** How fast the arm eases into motion and to rest. */
    private static final float EASE = 0.05f;
    /** The pellet's glow, a step every 4 ticks. */
    private static final float[] FLICKER = {1f, 0.82f, 1f, 0.62f};

    public RadiochemRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RadiochemCoreBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !state.getValue(RadiochemCoreBlock.FORMED)) return;
        Direction facing = state.getValue(RadiochemCoreBlock.FACING);
        boolean lit = state.getValue(RadiochemCoreBlock.LIT);
        BlockPos core = be.getBlockPos();

        double time = level.getGameTime() + partialTick;
        float dt = be.clientTime < 0 ? 0 : (float) Math.max(0, Math.min(10, time - be.clientTime));
        be.clientTime = time;
        be.clientSwing += ((lit ? 1 : 0) - be.clientSwing) * Math.min(1f, dt * EASE);
        be.clientAngle = (be.clientAngle + be.clientSwing * dt * SPEED) % 1f;

        // the hidden ports first: drawing a block takes buffers of its own, which closes any taken before
        for (int c = 0; c < RadiochemStructure.COLUMNS; c++)
            for (int d = 0; d < RadiochemStructure.DEPTH; d++) {
                BlockPos at = RadiochemStructure.cell(core, facing, c, d, 0);
                if (!(level.getBlockState(at).getBlock() instanceof MultiblockPortBlock port)) continue;
                BlockPos rel = at.subtract(core);
                ps.pushPose();
                ps.translate(rel.getX(), rel.getY(), rel.getZ());
                renderPort(ps, buffers, facing, c, d, port, side -> LevelRenderer.getLightColor(level, at.relative(side)), overlay);
                ps.popPose();
            }

        renderLive(ps, buffers, facing, be.clientAngle, lit, state.getValue(RadiochemCoreBlock.LEVEL), time,
                LevelRenderer.getLightColor(level, core.relative(facing)), overlay);
    }

    /**
     * The tongs and the handle at {@code angle} of a swing (0..1), the pellet and the level lamp at {@code water} eighths,
     * the pose in the core block; {@code time} in ticks steps the pellet's flicker.
     */
    static void renderLive(PoseStack ps, MultiBufferSource buffers, Direction facing, float angle, boolean lit, int water, double time,
                           int lt, int overlay) {
        float phase = angle * (float) (Math.PI * 2);
        float sway = SWAY * (float) Math.sin(phase), dip = -DIP * (float) Math.max(0, Math.sin(2 * phase));
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(LiveModel.turn(facing)));
        ps.translate(-0.5, 0, -0.5);
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.RADIOCHEM.texture));
        for (String g : new String[]{"arm", "handle"}) {
            float[] p = LiveModel.RADIOCHEM.pivot(g);
            ps.pushPose();
            ps.translate(p[0], p[1] + dip, p[2]);
            ps.mulPose(Axis.ZP.rotation(sway));
            ps.translate(-p[0], -p[1], -p[2]);
            LiveModel.RADIOCHEM.render(g, ps.last(), body, lt, overlay, 1f);
            ps.popPose();
        }
        if (!lit) {
            LiveModel.RADIOCHEM.render("pellet", ps.last(), body, lt, overlay, 0.22f);
            if (water > 0) level(ps, water, () -> LiveModel.RADIOCHEM.render("level", ps.last(), body, lt, overlay, 0.5f));
        } else {
            VertexConsumer eyes = buffers.getBuffer(RenderType.eyes(LiveModel.RADIOCHEM.texture));
            LiveModel.RADIOCHEM.render("pellet", ps.last(), eyes, LightTexture.FULL_BRIGHT, overlay, FLICKER[(int) (time / 4) % FLICKER.length]);
            if (water > 0) level(ps, water, () -> LiveModel.RADIOCHEM.render("level", ps.last(), eyes, LightTexture.FULL_BRIGHT, overlay, 1f));
        }
        ps.popPose();
    }

    /** Draws the level lamp scaled up from its foot to the water in eighths. */
    private static void level(PoseStack ps, int eighths, Runnable draw) {
        float[] p = LiveModel.RADIOCHEM.pivot("level");
        ps.pushPose();
        ps.translate(p[0], p[1], p[2]);
        ps.scale(1, eighths / 8f, 1);
        ps.translate(-p[0], -p[1], -p[2]);
        draw.run();
        ps.popPose();
    }

    /**
     * A hidden port in a bottom cell, the pose in its block: the casing's very piece for that cell, and the port's mark on
     * every face a pipe can come to: below, and the sides that are the outside of the plant.
     */
    private static void renderPort(PoseStack ps, MultiBufferSource buffers, Direction facing, int column, int depth, MultiblockPortBlock port,
                                   ToIntFunction<Direction> light, int overlay) {
        Direction right = RadiochemStructure.right(facing);
        // the piece takes the light of its first outer side; under the plant it is the ground's, often dark
        Direction out = depth == 0 ? facing : depth == RadiochemStructure.DEPTH - 1 ? facing.getOpposite()
                : column == 0 ? right.getOpposite() : column == RadiochemStructure.COLUMNS - 1 ? right : Direction.DOWN;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState()
                .setValue(NuclearCasingBlock.ROLE, CasingRole.PLANT).setValue(NuclearCasingBlock.PIECE, RadiochemStructure.index(column, depth, 0))
                .setValue(NuclearCasingBlock.FACING, facing), ps, buffers, light.applyAsInt(out), overlay);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.RADIOCHEM.texture));
        String mark = LiveModel.RADIOCHEM.mark(port);
        if (depth == 0) mark(ps, vc, mark, facing, light, overlay);
        if (depth == RadiochemStructure.DEPTH - 1) mark(ps, vc, mark, facing.getOpposite(), light, overlay);
        if (column == 0) mark(ps, vc, mark, right.getOpposite(), light, overlay);
        if (column == RadiochemStructure.COLUMNS - 1) mark(ps, vc, mark, right, light, overlay);
        LiveModel.RADIOCHEM.renderMark(mark, ps, vc, Axis.XP.rotationDegrees(90), light.applyAsInt(Direction.DOWN), overlay);
    }

    private static void mark(PoseStack ps, VertexConsumer vc, String mark, Direction side, ToIntFunction<Direction> light, int overlay) {
        LiveModel.RADIOCHEM.renderMark(mark, ps, vc, Axis.YP.rotationDegrees(LiveModel.turn(side)), light.applyAsInt(side), overlay);
    }

    /** NeoForge 1.21.1 culls block entity renderers by this box; on Forge 1.20.1 the block entity answers instead. */
    public AABB getRenderBoundingBox(RadiochemCoreBlockEntity be) {
        return be.renderBox();
    }
}
