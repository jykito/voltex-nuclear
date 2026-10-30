package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.api.client.GuideModels;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.cascade.CascadeCoreBlock;
import com.jykito.voltexnuclear.cascade.CascadeStructure;
import com.jykito.voltexnuclear.cascade.CasingRole;
import com.jykito.voltexnuclear.cascade.CentrifugeBlock;
import com.jykito.voltexnuclear.cascade.NuclearCasingBlock;
import com.jykito.voltexnuclear.column.ColumnCoreBlock;
import com.jykito.voltexnuclear.column.ColumnStructure;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.fusion.Torus;
import com.jykito.voltexnuclear.core.reactor.FluxMap;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.core.reactor.Scheme;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.fusion.TorusCoilBlock;
import com.jykito.voltexnuclear.fusion.TorusCoreBlock;
import com.jykito.voltexnuclear.fusion.TorusFrameBlock;
import com.jykito.voltexnuclear.fusion.TorusStructure;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlock;
import com.jykito.voltexnuclear.starforge.StarForgeFrameBlock;
import com.jykito.voltexnuclear.starforge.StarForgeStructure;
import com.jykito.voltexnuclear.radiochem.HotCellGlassBlock;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlock;
import com.jykito.voltexnuclear.radiochem.RadiochemStructure;
import com.jykito.voltexnuclear.reactor.ExpansionChamberBlock;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlock;
import com.jykito.voltexnuclear.reactor.ReactorFrameBlock;
import com.jykito.voltexnuclear.reactor.ReactorStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formed multiblocks of the add-on for the Voltex guide panel (its API {@link GuideModels}, M3 of the plan). The
 * schemas of the add-on's chapter name these models; once all their layers show, the guide draws the model from the
 * schema's core block instead of the blocks, with a button to show the blocks again. The models are the very block models
 * and live parts the world draws: the reactor runs the powerful scheme at its knob, the cascade turns at full speed, the
 * plant works with full tanks, the torus burns, the star forge melts. The models show no ports: the player puts them where it suits them, and in
 * every socket stands the formed casing. The light is full, as the guide draws its own models.
 */
public final class NuclearGuideModels {

    private NuclearGuideModels() {}

    /** The names the schemas of the chapter use (assets/industrial_core/guide/&lt;lang&gt;/50_voltex_nuclear.json). */
    public static final String REACTOR = "voltex_nuclear:reactor", CASCADE = "voltex_nuclear:cascade",
            RADIOCHEM = "voltex_nuclear:radiochem", COLUMN = "voltex_nuclear:heavy_water_column", TORUS = "voltex_nuclear:torus",
            STAR_FORGE = "voltex_nuclear:star_forge";

    /** The schemas stand with the front to the south, the way the core blocks face when placed by the guide. */
    private static final Direction FACING = Direction.SOUTH;
    private static final int LIGHT = LightTexture.FULL_BRIGHT, OVERLAY = OverlayTexture.NO_OVERLAY;
    /** The columns of the cascade in its schema. */
    private static final int CASCADE_COLUMNS = 4;
    /** The water of the guide's plant in eighths: full tanks. */
    private static final int RADIOCHEM_WATER = 8;
    /** The tiers of the column schema. */
    private static final int COLUMN_TIERS = 4;

    /** The lid of the guide's reactor: the powerful scheme, worked out once. */
    private static Zone zone;
    private static Part[] parts;
    private static FluxMap flux;

    /** Called once on client setup. */
    public static void register() {
        GuideModels.register(REACTOR, VoltexNuclear.id("reactor_core"), NuclearGuideModels::reactor);
        GuideModels.register(CASCADE, VoltexNuclear.id("cascade_core"), NuclearGuideModels::cascade);
        GuideModels.register(RADIOCHEM, VoltexNuclear.id("radiochem_core"), NuclearGuideModels::radiochem);
        GuideModels.register(COLUMN, VoltexNuclear.id("heavy_water_column_core"), NuclearGuideModels::column);
        GuideModels.register(TORUS, VoltexNuclear.id("torus_core"), NuclearGuideModels::torus);
        GuideModels.register(STAR_FORGE, VoltexNuclear.id("star_forge_core"), NuclearGuideModels::starForge);
    }

    private static void reactor(PoseStack ps, MultiBufferSource buffers, float ticks) {
        for (ReactorStructure.Place p : ReactorStructure.PLACES) block(ps, buffers, ReactorStructure.pos(BlockPos.ZERO, FACING, p), reactorBlock(p));
        if (zone == null) {
            zone = Scheme.POWERFUL.zone();
            parts = new Part[Zone.SIZE * Zone.SIZE];
            for (int i = 0; i < parts.length; i++) parts[i] = zone.get(i / Zone.SIZE, i % Zone.SIZE);
            flux = FluxMap.of(zone);
        }
        float power = (float) Scheme.POWERFUL.knob();
        ReactorRenderer.renderLive(ps, buffers, FACING, zone, parts, flux, power, ReactorRenderer.glow(power), LIGHT, LIGHT, OVERLAY);
    }

    /** A block of the formed reactor in its place: the core lit, a chamber looking out, the frame piece elsewhere. */
    private static BlockState reactorBlock(ReactorStructure.Place p) {
        return switch (p.role()) {
            case CORE -> NuclearBlocks.REACTOR_CORE.get().defaultBlockState().setValue(ReactorCoreBlock.FACING, FACING)
                    .setValue(ReactorCoreBlock.FORMED, true).setValue(ReactorCoreBlock.LIT, true);
            case CHAMBER_SOCKET -> NuclearBlocks.EXPANSION_CHAMBER.get().defaultBlockState()
                    .setValue(ExpansionChamberBlock.FACING, ReactorStructure.outward(FACING, p)).setValue(ExpansionChamberBlock.FORMED, true);
            default -> NuclearBlocks.REACTOR_FRAME.get().defaultBlockState().setValue(ReactorFrameBlock.PART, p.index())
                    .setValue(ReactorFrameBlock.FACING, FACING);
        };
    }

    private static void cascade(PoseStack ps, MultiBufferSource buffers, float ticks) {
        for (int c = 0; c <= CASCADE_COLUMNS; c++)
            for (int r = 0; r < CascadeStructure.ROWS; r++)
                if (c != 0 || r != CascadeStructure.ROWS - 1)   // the core, drawn below
                    block(ps, buffers, CascadeStructure.cell(BlockPos.ZERO, FACING, c, r), cascadeBlock(c, r));
        block(ps, buffers, BlockPos.ZERO, NuclearBlocks.CASCADE_CORE.get().defaultBlockState().setValue(CascadeCoreBlock.FACING, FACING)
                .setValue(CascadeCoreBlock.FORMED, true).setValue(CascadeCoreBlock.LIT, true)
                .setValue(CascadeCoreBlock.COLUMNS, CASCADE_COLUMNS));
        CascadeRenderer.renderLive(ps, buffers, FACING, CASCADE_COLUMNS, (ticks * CascadeRenderer.SPEED) % (float) (Math.PI * 2), 1f,
                (c, r) -> null, (c, r, side) -> LIGHT, OVERLAY);
    }

    /** A block of the formed, running cascade in a cell: a centrifuge in the ring row of a column, casing in its role elsewhere. */
    private static BlockState cascadeBlock(int column, int row) {
        if (column > 0 && row == 2) return NuclearBlocks.CENTRIFUGE.get().defaultBlockState().setValue(CentrifugeBlock.FACING, FACING)
                .setValue(CentrifugeBlock.FORMED, true).setValue(CentrifugeBlock.LIT, true);
        return NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState()
                .setValue(NuclearCasingBlock.ROLE, new CascadeStructure.Cell(BlockPos.ZERO, column, row).role())
                .setValue(NuclearCasingBlock.FACING, FACING).setValue(NuclearCasingBlock.LIT, true);
    }

    private static void radiochem(PoseStack ps, MultiBufferSource buffers, float ticks) {
        for (int c = 0; c < RadiochemStructure.COLUMNS; c++)
            for (int d = 0; d < RadiochemStructure.DEPTH; d++)
                for (int l = 0; l < RadiochemStructure.LEVELS; l++)
                    block(ps, buffers, RadiochemStructure.cell(BlockPos.ZERO, FACING, c, d, l), radiochemBlock(c, d, l));
        RadiochemRenderer.renderLive(ps, buffers, FACING, (ticks * RadiochemRenderer.SPEED) % 1f, true, RADIOCHEM_WATER, ticks, LIGHT, OVERLAY);
    }

    /** A block of the formed, working plant in a cell: the core lit, the panes of the window, casing in its piece elsewhere. */
    private static BlockState radiochemBlock(int column, int depth, int level) {
        return switch (RadiochemStructure.kind(column, depth, level)) {
            case CORE -> NuclearBlocks.RADIOCHEM_CORE.get().defaultBlockState().setValue(RadiochemCoreBlock.FACING, FACING)
                    .setValue(RadiochemCoreBlock.FORMED, true).setValue(RadiochemCoreBlock.LIT, true).setValue(RadiochemCoreBlock.LEVEL, RADIOCHEM_WATER);
            case GLASS -> NuclearBlocks.HOT_CELL_GLASS.get().defaultBlockState().setValue(HotCellGlassBlock.FORMED, true)
                    .setValue(HotCellGlassBlock.PANE, column - 1).setValue(HotCellGlassBlock.FACING, FACING);
            default -> NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState().setValue(NuclearCasingBlock.ROLE, CasingRole.PLANT)
                    .setValue(NuclearCasingBlock.PIECE, RadiochemStructure.index(column, depth, level))
                    .setValue(NuclearCasingBlock.FACING, FACING).setValue(NuclearCasingBlock.LIT, true);
        };
    }

    private static void column(PoseStack ps, MultiBufferSource buffers, float ticks) {
        for (int t = 0; t < COLUMN_TIERS; t++)
            for (int a = -1; a <= 1; a++)
                for (int d = 0; d < 3; d++) {
                    BlockState state = t == 0 && a == 0 && d == 0
                            ? NuclearBlocks.COLUMN_CORE.get().defaultBlockState().setValue(ColumnCoreBlock.FACING, FACING)
                                    .setValue(ColumnCoreBlock.FORMED, true).setValue(ColumnCoreBlock.LIT, true)
                            : columnCasing(ColumnStructure.piece(new ColumnStructure.Cell(BlockPos.ZERO, ColumnStructure.Kind.TIER, t, a, d), COLUMN_TIERS));
                    block(ps, buffers, ColumnStructure.cell(BlockPos.ZERO, FACING, t, a, d), state);
                }
        for (int up = 0; up < ColumnStructure.PILLAR; up++)
            block(ps, buffers, ColumnStructure.pillar(BlockPos.ZERO, FACING, up), columnCasing(ColumnStructure.pillarPiece(up, COLUMN_TIERS)));
    }

    private static BlockState columnCasing(int piece) {
        return NuclearBlocks.NUCLEAR_CASING.get().defaultBlockState().setValue(NuclearCasingBlock.ROLE, CasingRole.HEAVY_COLUMN)
                .setValue(NuclearCasingBlock.PIECE, piece).setValue(NuclearCasingBlock.FACING, FACING).setValue(NuclearCasingBlock.LIT, true);
    }

    private static void torus(PoseStack ps, MultiBufferSource buffers, float ticks) {
        for (TorusStructure.Place p : TorusStructure.PLACES) block(ps, buffers, TorusStructure.pos(BlockPos.ZERO, FACING, p), torusBlock(p));
        long time = (long) ticks;
        TorusRenderer.renderLive(ps, buffers, FACING, Torus.Phase.LIT, time, ticks - time, 0, LIGHT, OVERLAY);
    }

    /** A block of the formed, burning torus in its place: the core lit, a coil in its turn, the frame piece elsewhere. */
    private static BlockState torusBlock(TorusStructure.Place p) {
        return switch (p.role()) {
            case CORE -> NuclearBlocks.TORUS_CORE.get().defaultBlockState().setValue(TorusCoreBlock.FACING, FACING)
                    .setValue(TorusCoreBlock.FORMED, true).setValue(TorusCoreBlock.LIT, true);
            case COIL -> NuclearBlocks.TORUS_COIL.get().defaultBlockState().setValue(TorusCoilBlock.FACING, FACING)
                    .setValue(TorusCoilBlock.FORMED, true).setValue(TorusCoilBlock.COIL, p.coil());
            default -> NuclearBlocks.TORUS_FRAME.get().defaultBlockState().setValue(TorusFrameBlock.PART, p.index())
                    .setValue(TorusFrameBlock.FACING, FACING);
        };
    }

    private static void starForge(PoseStack ps, MultiBufferSource buffers, float ticks) {
        for (StarForgeStructure.Place p : StarForgeStructure.PLACES) block(ps, buffers, StarForgeStructure.pos(BlockPos.ZERO, FACING, p), forgeBlock(p));
        StarForgeRenderer.renderLive(ps, buffers, FACING, true, ticks, LIGHT, OVERLAY);
    }

    /** A block of the formed, melting forge in its place: the core lit, the frame piece elsewhere. */
    private static BlockState forgeBlock(StarForgeStructure.Place p) {
        return p.role() == StarForgeStructure.Role.CORE
                ? NuclearBlocks.STAR_FORGE_CORE.get().defaultBlockState().setValue(StarForgeCoreBlock.FACING, FACING)
                        .setValue(StarForgeCoreBlock.FORMED, true).setValue(StarForgeCoreBlock.LIT, true)
                : NuclearBlocks.STAR_FORGE_FRAME.get().defaultBlockState().setValue(StarForgeFrameBlock.PART, p.index())
                        .setValue(StarForgeFrameBlock.FACING, FACING);
    }

    /** A block of a model at {@code at}, from the core block. */
    private static void block(PoseStack ps, MultiBufferSource buffers, BlockPos at, BlockState state) {
        ps.pushPose();
        ps.translate(at.getX(), at.getY(), at.getZ());
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ps, buffers, LIGHT, OVERLAY);
        ps.popPose();
    }
}
