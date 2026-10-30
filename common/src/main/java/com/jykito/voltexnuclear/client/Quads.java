package com.jykito.voltexnuclear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * One vertex of a block entity model or of an outline: written {@code vertex(...).endVertex()} on Forge 1.20.1 and
 * {@code addVertex(...)} on NeoForge 1.21.1, so each loader's client setup hands in its own spelling.
 */
public final class Quads {

    private Quads() {}

    public interface Sink {
        /** A vertex at (x, y, z) in the pose, its normal turned by the pose too. */
        void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float shade, float u, float v,
                    int overlay, int light, float nx, float ny, float nz);
    }

    public interface LineSink {
        /** A vertex of a line (RenderType.lines()): the position, the colour, the line's direction as its normal. */
        void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float r, float g, float b, float a,
                    float nx, float ny, float nz);
    }

    public static Sink sink;
    public static LineSink lines;
}
