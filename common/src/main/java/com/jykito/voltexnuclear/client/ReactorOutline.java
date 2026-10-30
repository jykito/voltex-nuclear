package com.jykito.voltexnuclear.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlock;
import com.jykito.voltexnuclear.reactor.ReactorStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The outline of a formed fission reactor (DESIGN step 16): aimed at any of its blocks, the game draws one outline round
 * the whole model instead of the block's box, in the lines and colour of the vanilla one. The outlines come from
 * {@code models/reactor_outline.json}, written by tools/texture_viewer/export_nuclear_reactor.js: the crease edges of the
 * model in 1/16 steps, one per set of chambers. Each loader's highlight event calls {@link #draw} and drops its own box
 * when this one drew.
 */
public final class ReactorOutline {

    private static final ResourceLocation SPEC = VoltexNuclear.id("models/reactor_outline.json");
    /** The vanilla outline's colour. */
    private static final float ALPHA = 0.4f;

    /** Segments per set of chambers ("", "B", "LRTB" ...), six numbers each in the model's coordinates, in blocks. */
    private static Map<String, float[]> outlines;

    private ReactorOutline() {}

    private static Map<String, float[]> outlines() {
        if (outlines == null) outlines = load();
        return outlines;
    }

    private static Map<String, float[]> load() {
        Map<String, float[]> out = new HashMap<>();
        try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(SPEC)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            float unit = root.get("unit").getAsFloat();
            float[] origin = {root.getAsJsonArray("origin").get(0).getAsFloat(), root.getAsJsonArray("origin").get(1).getAsFloat(), root.getAsJsonArray("origin").get(2).getAsFloat()};
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("outlines").entrySet()) {
                String[] v = e.getValue().getAsString().split(" ");
                float[] seg = new float[v.length];
                for (int i = 0; i < v.length; i++) seg[i] = origin[i % 3] + Integer.parseInt(v[i]) * unit;
                out.put(e.getKey(), seg);
            }
        } catch (Exception e) {
            VoltexNuclear.LOGGER.error("Reactor outline {} not loaded", SPEC, e);
        }
        return out;
    }

    /** The key of a set of chambers: its sides in the order L, R, T, B. */
    private static String key(Set<Zone.Side> chambers) {
        StringBuilder k = new StringBuilder();
        if (chambers.contains(Zone.Side.LEFT)) k.append('L');
        if (chambers.contains(Zone.Side.RIGHT)) k.append('R');
        if (chambers.contains(Zone.Side.TOP)) k.append('T');
        if (chambers.contains(Zone.Side.BOTTOM)) k.append('B');
        return k.toString();
    }

    /**
     * Draws the outline of the reactor the aimed block belongs to; false when it belongs to none, and the game draws its
     * own box. {@code pose} is the highlight event's, relative to the camera.
     */
    public static boolean draw(Level level, BlockPos aimed, PoseStack pose, MultiBufferSource buffers, Vec3 camera) {
        BlockPos core = ReactorStructure.formedCore(level, aimed, level.getBlockState(aimed));
        if (core == null) return false;
        Direction facing = level.getBlockState(core).getValue(ReactorCoreBlock.FACING);
        float[] seg = outlines().get(key(ReactorStructure.chambersOf(level, core, facing)));
        if (seg == null) return false;
        pose.pushPose();
        pose.translate(core.getX() - camera.x + 0.5, core.getY() - camera.y, core.getZ() - camera.z + 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch (facing) { case EAST -> 90f; case NORTH -> 180f; case WEST -> 270f; default -> 0f; }));
        pose.translate(0, -ReactorStructure.CORE.y(), -ReactorStructure.CORE.z());
        PoseStack.Pose p = pose.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i + 5 < seg.length; i += 6) {
            float dx = seg[i + 3] - seg[i], dy = seg[i + 4] - seg[i + 1], dz = seg[i + 5] - seg[i + 2];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len; dy /= len; dz /= len;
            Quads.lines.vertex(vc, p, seg[i], seg[i + 1], seg[i + 2], 0, 0, 0, ALPHA, dx, dy, dz);
            Quads.lines.vertex(vc, p, seg[i + 3], seg[i + 4], seg[i + 5], 0, 0, 0, ALPHA, dx, dy, dz);
        }
        pose.popPose();
        return true;
    }
}
