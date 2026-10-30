package com.jykito.voltexnuclear.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a machine's core draws itself (DESIGN step 18: everything that stands still is in the block models instead): the
 * live parts of a model and the marks of hidden ports, from {@code models/<name>_live.json} on the machine's atlas. The
 * files are written by the export scripts in tools/texture_viewer from the same models the mock-ups show.
 * <p>
 * A box is {@code f}, {@code t} in blocks and a uv rectangle in atlas pixels per face (east, west, south, north, up,
 * down; null for a face dropped on a cut), each face mapped whole the way block models map theirs.
 */
public final class LiveModel {

    /** The cascade's rotors and slits, and the pieces of the cells a hidden port can take. */
    public static final LiveModel CASCADE = new LiveModel("cascade");
    /** The reactor's rings. */
    public static final LiveModel REACTOR = new LiveModel("reactor");
    /** The radiochemical plant's tongs, handle, pellet and level lamp. */
    public static final LiveModel RADIOCHEM = new LiveModel("radiochem");
    /** The heavy water column: only the squares of its hidden ports. */
    public static final LiveModel COLUMN = new LiveModel("column");
    /** The fusion torus: a window of plasma and a coil's band to turn to their places, the solenoid's band, the squares of its hidden ports. */
    public static final LiveModel TORUS = new LiveModel("torus");
    /** The star forge: the star in its crown, the dome's two gold bands, the squares of its hidden ports. */
    public static final LiveModel STAR_FORGE = new LiveModel("star_forge");

    public final ResourceLocation texture;
    private final ResourceLocation spec;
    private Map<String, List<Quad>> groups;
    private final Map<String, float[]> pivots = new HashMap<>();
    private final Map<Block, String> marks = new IdentityHashMap<>();

    /** Four vertices (top left, bottom left, bottom right, top right as seen from outside), their uv, the normal. */
    private record Quad(float[] pos, float[] uv, float nx, float ny, float nz) {}

    private LiveModel(String name) {
        texture = VoltexNuclear.id("textures/block/" + name + "_atlas.png");
        spec = VoltexNuclear.id("models/" + name + "_live.json");
    }

    private Map<String, List<Quad>> groups() {
        if (groups == null) groups = load();
        return groups;
    }

    private Map<String, List<Quad>> load() {
        Map<String, List<Quad>> out = new HashMap<>();
        try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(spec)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            float atlas = root.get("atlas").getAsFloat();
            if (root.has("pivots"))
                for (Map.Entry<String, JsonElement> p : root.getAsJsonObject("pivots").entrySet()) pivots.put(p.getKey(), vec(root.getAsJsonObject("pivots"), p.getKey()));
            for (JsonElement el : root.getAsJsonArray("boxes")) {
                JsonObject b = el.getAsJsonObject();
                addBox(out.computeIfAbsent(b.get("g").getAsString(), k -> new ArrayList<>()), vec(b, "f"), vec(b, "t"), b.getAsJsonArray("uv"), atlas);
            }
        } catch (Exception e) {
            VoltexNuclear.LOGGER.error("Live model {} not loaded", spec, e);
        }
        return out;
    }

    private static float[] vec(JsonObject o, String key) {
        JsonArray a = o.getAsJsonArray(key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static final float[][] NORMALS = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};

    private static void addBox(List<Quad> to, float[] f, float[] t, JsonArray uv, float atlas) {
        float x1 = f[0], y1 = f[1], z1 = f[2], x2 = t[0], y2 = t[1], z2 = t[2];
        float[][] corners = {
                {x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1},   // east: u runs along -z
                {x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2},   // west: along +z
                {x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2},   // south: along +x
                {x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1},   // north: along -x
                {x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1},   // up: v runs along +z
                {x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2}};  // down: v along -z
        for (int i = 0; i < 6; i++) {
            if (uv.get(i).isJsonNull()) continue;
            JsonArray r = uv.get(i).getAsJsonArray();
            float u1 = r.get(0).getAsFloat() / atlas, v1 = r.get(1).getAsFloat() / atlas, u2 = r.get(2).getAsFloat() / atlas, v2 = r.get(3).getAsFloat() / atlas;
            to.add(new Quad(corners[i], new float[]{u1, v1, u1, v2, u2, v2, u2, v1}, NORMALS[i][0], NORMALS[i][1], NORMALS[i][2]));
        }
    }

    /** The point a live group turns or scales about, as the export wrote it; the origin when it has none. */
    public float[] pivot(String group) {
        groups();
        return pivots.getOrDefault(group, new float[3]);
    }

    /** Whether the model has a group of this name. */
    public boolean has(String group) {
        return groups().containsKey(group);
    }

    /** Draws a group in the pose; shade dims it (1 as painted). */
    public void render(String group, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, float shade) {
        List<Quad> qs = groups().get(group);
        if (qs == null) return;
        for (Quad q : qs) vertices(q, pose, vc, light, overlay, shade);
    }

    /**
     * Draws only the outermost faces of a group that look south (+z) in the model: one side of a group made for two, such
     * as the mark of a port, which the caller turns onto each face it wants. The cascade's marks hold a back plate too,
     * whose south face lies in the plane of the opposite face of the piece: drawn, it fought that face (z-fighting through
     * the open pixels of the energy port's mark), so only the faces furthest south are drawn.
     */
    public void renderSouth(String group, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, float shade) {
        List<Quad> qs = groups().get(group);
        if (qs == null) return;
        float front = Float.NEGATIVE_INFINITY;
        for (Quad q : qs) if (q.nz() > 0.5f) front = Math.max(front, q.pos()[2]);
        for (Quad q : qs) if (q.nz() > 0.5f && q.pos()[2] > front - 1e-3f) vertices(q, pose, vc, light, overlay, shade);
    }

    private static void vertices(Quad q, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, float shade) {
        for (int i = 0; i < 4; i++)
            Quads.sink.vertex(vc, pose, q.pos()[i * 3], q.pos()[i * 3 + 1], q.pos()[i * 3 + 2], shade, q.uv()[i * 2], q.uv()[i * 2 + 1],
                    overlay, light, q.nx(), q.ny(), q.nz());
    }

    /**
     * The light a hidden port's piece is drawn in: the brightest of the six cells around it, sky and block light apart. A
     * hidden port is a solid block, dark inside, and a block may stand against any one of its sides (an energy bank at a
     * stand), which would put the whole piece in the dark if it took the light of that side.
     */
    public static int lightAround(BlockAndTintGetter level, BlockPos at) {
        int sky = 0, block = 0;
        for (Direction d : Direction.values()) {
            int l = LevelRenderer.getLightColor(level, at.relative(d));
            sky = Math.max(sky, LightTexture.sky(l));
            block = Math.max(block, LightTexture.block(l));
        }
        return LightTexture.pack(block, sky);
    }

    /** A port's mark, made for the south face of a block, turned about the block's centre onto another face. */
    public void renderMark(String mark, PoseStack ps, VertexConsumer vc, Quaternionf turn, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(turn);
        ps.translate(-0.5, -0.5, -0.5);
        renderSouth(mark, ps.last(), vc, light, overlay, 1f);
        ps.popPose();
    }

    /** The turn from south to a direction, the way the blockstates turn the block models. */
    public static float turn(Direction d) {
        return switch (d) {
            case EAST -> 90f;
            case NORTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    /**
     * The mark of a hidden port: its own square by the port block's id (multiblock_input_port -> mark_input,
     * me_pattern_port -> mark_me_pattern), so the ME ports keep their look; a port the model has no square for wears the
     * mark of its kind.
     */
    public String mark(MultiblockPortBlock port) {
        return marks.computeIfAbsent(port, b -> {
            String own = "mark_" + BuiltInRegistries.BLOCK.getKey(b).getPath().replaceFirst("^multiblock_", "").replaceFirst("_port$", "");
            return has(own) ? own : "mark_" + port.kind().name().toLowerCase(Locale.ROOT);
        });
    }
}
