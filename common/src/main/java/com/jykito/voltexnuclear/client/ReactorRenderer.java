package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.reactor.FluxMap;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.reactor.ReactorParts;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlock;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity;
import com.jykito.voltexnuclear.reactor.ReactorFrameBlock;
import com.jykito.voltexnuclear.reactor.ReactorStructure;
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
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * The live parts of a formed fission reactor (DESIGN steps 16 and 26): the two rings round the body, the zone on the lid
 * (a cover on a shut cell, a plate on an open one and on it the cap of its part, the fuel caps in the colour of their
 * fuel, the control rods), and where a hidden Voltex port stands in the plinth, the piece of the plinth in its block (the
 * very model of the frame block in that place) with the port's own square on every face a pipe can come to. The rest of
 * the reactor is in the block models, the lit face of a running core too.
 * <p>
 * The light comes in four steps, with no smooth change: the rings follow the power, each fuel cap its own cell's flux
 * against the hottest one times the power; SCRAM blinks the rings for 1.5 s, then they smoulder. The control rods go down
 * as the power goes down. The zone, the power and SCRAM come from the core's update packets, sent only when they change;
 * the flux the client works out from the zone with the core module's FluxMap.
 */
public class ReactorRenderer implements BlockEntityRenderer<ReactorCoreBlockEntity> {

    /** The lowest of the four steps of the glow (DESIGN step 16): a ring that barely smoulders. */
    private static final float SMOULDER = 0.22f;

    private final BlockRenderDispatcher blocks;

    public ReactorRenderer(BlockEntityRendererProvider.Context ctx) {
        blocks = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(ReactorCoreBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !state.getValue(ReactorCoreBlock.FORMED)) return;
        Direction facing = state.getValue(ReactorCoreBlock.FACING);
        BlockPos core = be.getBlockPos();

        for (ReactorStructure.Place p : ReactorStructure.PLACES) {
            if (p.role() != ReactorStructure.Role.PORT_SOCKET) continue;
            BlockPos at = ReactorStructure.pos(core, facing, p);
            if (!(level.getBlockState(at).getBlock() instanceof MultiblockPortBlock port)) continue;
            ps.pushPose();
            ps.translate(at.getX() - core.getX(), at.getY() - core.getY(), at.getZ() - core.getZ());
            renderPort(ps, buffers, blocks, facing, p, port, LiveModel.lightAround(level, at),
                    side -> LevelRenderer.getLightColor(level, at.relative(side)), overlay);
            ps.popPose();
        }

        float power = (float) be.clientPower;
        long ticks = level.getGameTime() - be.clientScramAt;
        float rings = be.clientScram && ticks < SCRAM_BLINK ? (ticks / BLINK_STEP % 2 == 0 ? 1f : SMOULDER) : glow(power);
        renderLive(ps, buffers, facing, be.clientZone, be.clientParts, be.clientFlux, power, rings,
                LevelRenderer.getLightColor(level, core.relative(facing)),
                LevelRenderer.getLightColor(level, ReactorStructure.at(core, facing, 0, ReactorStructure.LAYERS, 0)), overlay);
    }

    /**
     * A hidden port in place {@code p}, the pose in its block: the piece of the plinth (the very model of the frame block in
     * that place) and the port's mark on every face a pipe can come to, out of the reactor, into the notch of a missing
     * corner and below. {@code pieceLight} is the light around the block ({@link LiveModel#lightAround}), {@code light}
     * the light beside a face for its mark: a hidden port is a solid block, dark inside.
     */
    private static void renderPort(PoseStack ps, MultiBufferSource buffers, BlockRenderDispatcher blocks, Direction facing, ReactorStructure.Place p,
                                   MultiblockPortBlock port, int pieceLight, ToIntFunction<Direction> light, int overlay) {
        blocks.renderSingleBlock(NuclearBlocks.REACTOR_FRAME.get().defaultBlockState().setValue(ReactorFrameBlock.PART, p.index())
                .setValue(ReactorFrameBlock.FACING, facing), ps, buffers, pieceLight, overlay);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(LiveModel.REACTOR.texture));
        String mark = LiveModel.REACTOR.mark(port);
        for (int[] d : SIDES) {
            if (ReactorStructure.isPlace(p.x() + d[0], p.y(), p.z() + d[1])) continue;
            Direction w = ReactorStructure.direction(facing, d[0], d[1]);
            LiveModel.REACTOR.renderMark(mark, ps, vc, Axis.YP.rotationDegrees(LiveModel.turn(w)), light.applyAsInt(w), overlay);
        }
        LiveModel.REACTOR.renderMark(mark, ps, vc, Axis.XP.rotationDegrees(90), light.applyAsInt(Direction.DOWN), overlay);
    }

    /**
     * The rings and the zone on the lid, the pose in the core block. {@code rings} is the rings' own glow (SCRAM blinks
     * it); the zone, its parts and their flux may be missing until the core's first update.
     */
    static void renderLive(PoseStack ps, MultiBufferSource buffers, Direction facing, @Nullable Zone zone, Part[] parts, @Nullable FluxMap flux,
                           float power, float rings, int ringLight, int lidLight, int overlay) {
        // in the model's own coordinates, its axis through the middle of the plinth: the core stands at (0, 3, 2); a cell of
        // the zone is half a block, the middle one (3, 3) on the axis
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(LiveModel.turn(facing)));
        ps.translate(0, -ReactorStructure.CORE.y(), -ReactorStructure.CORE.z());
        LiveModel model = LiveModel.REACTOR;

        // still in the world's light: the rings dim, the lid with the zone, the control rods as low as the power is
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        model.render("rings", ps.last(), body, ringLight, overlay, SMOULDER);
        double fmax = 0;
        boolean live = zone != null && flux != null;
        if (live) {
            for (int i = 0; i < ReactorCoreBlockEntity.CELLS; i++)
                if (parts[i] != null && parts[i].isFuel()) fmax = Math.max(fmax, flux.flux(i / 7, i % 7));
            for (int i = 0; i < ReactorCoreBlockEntity.CELLS; i++) {
                int r = i / 7, c = i % 7;
                ps.pushPose();
                ps.translate((c - 3) * CELL, 0, (r - 3) * CELL);
                if (!zone.open(r, c)) model.render("lid_shut", ps.last(), body, lidLight, overlay, 1f);
                else {
                    model.render("lid_open", ps.last(), body, lidLight, overlay, 1f);
                    Part part = parts[i];
                    if (part != null) {
                        if (part.isFuel()) {
                            model.render("cap_fuel", ps.last(), body, lidLight, overlay, 1f);
                            model.render(fuelGlow(part), ps.last(), body, lidLight, overlay, SMOULDER);
                        } else if (part.kind() == Part.Kind.CONTROL_ROD) {
                            model.render("cap_control", ps.last(), body, lidLight, overlay, 1f);
                            ps.translate(0, -(1 - power) * ROD_TRAVEL, 0);
                            model.render("cap_rods", ps.last(), body, lidLight, overlay, 1f);
                        } else {
                            model.render(CAP.get(part.kind()), ps.last(), body, lidLight, overlay, 1f);
                        }
                    }
                }
                ps.popPose();
            }
        }

        // the light of their own, in four steps: the rings by the power, each fuel cap by its own cell's flux
        VertexConsumer eyes = null;
        if (rings > SMOULDER) {
            eyes = buffers.getBuffer(RenderType.eyes(model.texture));
            model.render("rings", ps.last(), eyes, LightTexture.FULL_BRIGHT, overlay, rings);
        }
        if (live && fmax > 0)
            for (int i = 0; i < ReactorCoreBlockEntity.CELLS; i++) {
                Part part = parts[i];
                if (part == null || !part.isFuel()) continue;
                float step = glow(power * flux.flux(i / 7, i % 7) / fmax);
                if (step <= SMOULDER) continue;
                if (eyes == null) eyes = buffers.getBuffer(RenderType.eyes(model.texture));
                ps.pushPose();
                ps.translate((i % 7 - 3) * CELL, 0, (i / 7 - 3) * CELL);
                model.render(fuelGlow(part), ps.last(), eyes, LightTexture.FULL_BRIGHT, overlay, step);
                ps.popPose();
            }
        ps.popPose();
    }

    /** A cell of the zone on the lid, in blocks; how far the control rods go down from full power to none. */
    private static final float CELL = 0.5f, ROD_TRAVEL = 0.3f;
    /** SCRAM blinks the rings for 1.5 s, a step every quarter of a second (DESIGN step 16), in ticks. */
    private static final long SCRAM_BLINK = 30, BLINK_STEP = 5;
    /** The caps of the parts other than fuel and control rods (their groups in the live model). */
    private static final Map<Part.Kind, String> CAP = new EnumMap<>(Map.of(
            Part.Kind.GRAPHITE, "cap_graphite", Part.Kind.HEAVY_WATER, "cap_heavy_water", Part.Kind.REFLECTOR, "cap_reflector",
            Part.Kind.ABSORBER, "cap_absorber", Part.Kind.CHANNEL, "cap_channel", Part.Kind.HEAT_SINK, "cap_sink",
            Part.Kind.HEAT_EXCHANGER, "cap_exchanger", Part.Kind.WATER_CELL, "cap_water_cell", Part.Kind.CRYO_CELL, "cap_cryo_cell"));

    /** The glowing cap of a fuel part, in the colour of its fuel; a pack's fuel wears the cap it chose. */
    private static String fuelGlow(Part fuel) {
        return switch (ReactorParts.cap(fuel.fuel())) {
            case LOW_ENRICHED -> "cap_glow_leu";
            case HIGH_ENRICHED -> "cap_glow_heu";
            case MOX -> "cap_glow_mox";
        };
    }

    /** The four steps of the glow (DESIGN step 16, as on the reactor page): nothing smoulders, then a third, two, all. */
    static float glow(double v) {
        return v <= 0.001 ? SMOULDER : v <= 1 / 3.0 ? 0.5f : v <= 2 / 3.0 ? 0.75f : 1f;
    }

    /** Steps along the reactor's own frame to the four sides of a block: {dx, dz}. */
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};


    /** NeoForge 1.21.1 culls block entity renderers by this box; on Forge 1.20.1 the block entity answers instead. */
    public AABB getRenderBoundingBox(ReactorCoreBlockEntity be) {
        return be.renderBox();
    }
}
