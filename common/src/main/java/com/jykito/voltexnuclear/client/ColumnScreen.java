package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.industrialcore.screen.Bars;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.column.ColumnCoreBlockEntity.Problem;
import com.jykito.voltexnuclear.column.ColumnMenu;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.core.column.HeavyWaterColumn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.jykito.voltexnuclear.client.VoltexBars.big;
import static com.jykito.voltexnuclear.client.VoltexBars.decimal;
import static com.jykito.voltexnuclear.column.ColumnCoreBlockEntity.*;

/**
 * The heavy water column window (DESIGN step 13, the author's "Gauges" on :8781/nuclear_deuterium_gui.html): his sunk
 * panel with four of his tanks, the water in the column, the heat in the heat ports as a thermometer with the threshold on
 * its scale, the coolant the cold tiers get of what they need, the heavy water in the output ports; under it the strip of the tiers
 * (the hot ones in the heat colours, the cold ones in the coolant's, the free places up to eight dotted); the energy; on
 * the right the readout. The positions are those column_gui.js writes next to the texture.
 */
public class ColumnScreen extends AbstractContainerScreen<ColumnMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/heavy_water_column.png");
    private static final int TEX_W = 256, TEX_H = 339;

    // layout, in window pixels, as column_gui.js draws it
    private static final int TANK_Y = 22, TANK_W = 16, TANK_H = 64, WATER_X = 30, HEAT_X = 60, COOL_X = 90, HEAVY_X = 120;
    private static final int GLASS_U = 8, GLASS_V = 274;
    /** The thermometer in the heat tank: the mercury under the window, the tube's left edge and the bulb's height in the tank. */
    private static final int MERCURY_U = 28, MERCURY_V = 274, TUBE = 6, BULB = 6;
    private static final int C_TOP = 0xFFFFF4D0, C_TOP2 = 0xFFFFD88A;
    private static final int STRIP_X = 17, STRIP_Y = 97, STRIP_W = 132, STRIP_H = 16;
    private static final int EN_X = 23, EN_Y = 135, EN_W = 120, EN_LABEL_X = 17, EN_LABEL_Y = 126, INFO_X = 158, INFO_R = 238;

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC, C_WARN = 0xFFFFB347, C_BAD = 0xFFFF5A4A;
    private static final int C_RUN = 0xFF5A86F0, C_HEAT = 0xFFFF9A3C, C_COOL = 0xFF00D5FF, C_HEAVY = 0xFF6F9CFF;
    /** The tiers in the strip: off, short, full and a spark; the heat colours for the hot tiers, the coolant's for the cold. */
    private static final int[] HEAT = {0xFF2A1408, 0xFF7A3A12, 0xFFFF9A3C, 0xFFFFC890}, COOL = {0xFF08262E, 0xFF0A6A82, 0xFF00D5FF, 0xFFA8F0FF};
    private static final int C_DOT = 0xFF3A4045, C_TIER = 0xFF3A4045, C_TIER_TOP = 0xFF5D676D;

    private static final Component WATER = Component.translatable("block.minecraft.water");
    private static final Component COOLANT = Component.translatable("fluid_type.industrial_core.coolant_fluid");
    private static final Component HEAVY = Component.translatable("fluid_type.voltex_nuclear.heavy_water_fluid");

    private final VoltexBars bars = new VoltexBars();
    private int ticks;

    public ColumnScreen(ColumnMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 256;
        imageHeight = 265;
        titleLabelY = 10000;
        inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
        tooltips(g, mx - leftPos, my - topPos, mx, my);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        bars.step();
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        drawTanks(g, x, y);
        drawTiers(g, x, y);
        drawEnergy(g, x, y);
        drawReadout(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private void drawTanks(GuiGraphics g, int x, int y) {
        int ty = y + TANK_Y;
        VoltexBars.fluid(g, x + WATER_X, ty, TANK_W, TANK_H, menu.get(D_WATER), HeavyWaterColumn.WATER_TANK, VoltexBars.WATER_TINT);
        thermometer(g, x + HEAT_X, ty);
        VoltexBars.fluid(g, x + COOL_X, ty, TANK_W, TANK_H, menu.get(D_COOL_USED), menu.get(D_COOL_NEED), VoltexBars.COOLANT_TINT);
        VoltexBars.fluid(g, x + HEAVY_X, ty, TANK_W, TANK_H, menu.getWide(D_HEAVY), menu.getWide(D_HEAVY_ROOM), NuclearFluids.HEAVY_WATER_TINT);
        for (int tx : new int[]{WATER_X, HEAT_X, COOL_X, HEAVY_X})
            VoltexBars.overlay(g, TEXTURE, x + tx, ty, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    /**
     * The heat (the author's pick T5 "Thermometer"): the mercury uncovered from the bottom up to the heat on a scale of one
     * of his heat ports, the threshold on the scale's long tick; its top lit, and trembling a pixel while the column runs.
     * The tooltip gives the exact sum of the ports.
     */
    private void thermometer(GuiGraphics g, int x, int y) {
        int heat = menu.getWide(D_HEAT);
        if (heat <= 0) return;
        int level = (int) Math.round((double) TANK_H * Math.min(heat, HeatPortBlockEntity.MAX_HEAT) / HeatPortBlockEntity.MAX_HEAT);
        boolean tremble = menu.get(D_WORKING) == 1 && ticks / 3 % 4 == 1;
        int top = Math.max(BULB, Math.min(TANK_H - 2, level + (tremble ? 1 : 0)));
        VoltexBars.overlay(g, TEXTURE, x, y + TANK_H - top, MERCURY_U, MERCURY_V + TANK_H - top, TANK_W, top, TEX_W, TEX_H);
        if (top > BULB) {
            fill(g, x + TUBE + 1, y + TANK_H - top, 1, 1, C_TOP);
            fill(g, x + TUBE + 2, y + TANK_H - top, 1, 1, C_TOP2);
        }
    }

    /** The tiers along the strip from the left: the hot ones warm, the cold ones icy, dim when stopped, the free places dotted. */
    private void drawTiers(GuiGraphics g, int x, int y) {
        int tiers = menu.get(D_TIERS), hot = HeavyWaterColumn.hot(tiers), speed = menu.get(D_SPEED);
        boolean on = menu.get(D_WORKING) == 1, full = speed >= Math.round(HeavyWaterColumn.fullSpeed(tiers) * 1000);
        int step = ticks / 4;
        for (int k = 0; k < HeavyWaterColumn.MAX_TIERS; k++) {
            int tx = x + STRIP_X + 3 + k * 16, ty = y + STRIP_Y + 3, w = 14, h = 10;
            if (k >= tiers) {
                for (int i = 0; i < w; i += 2) { fill(g, tx + i, ty, 1, 1, C_DOT); fill(g, tx + i, ty + h - 1, 1, 1, C_DOT); }
                for (int j = 0; j < h; j += 2) { fill(g, tx, ty + j, 1, 1, C_DOT); fill(g, tx + w - 1, ty + j, 1, 1, C_DOT); }
                continue;
            }
            boolean isHot = k < hot;
            int[] pal = isHot ? HEAT : COOL;
            fill(g, tx, ty, w, h, C_TIER);
            fill(g, tx, ty, w, 1, C_TIER_TOP);
            fill(g, tx + 2, ty + 3, w - 4, h - 5, !on ? pal[0] : isHot || full ? pal[2] : pal[1]);
            if (on && (step + k) % 4 == 0) fill(g, tx + 3 + (k * 5) % (w - 8), ty + 4, 2, 1, pal[3]);
        }
    }

    private void drawEnergy(GuiGraphics g, int x, int y) {
        int energy = menu.getWide(D_ENERGY);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.column.energy"), x + EN_LABEL_X, y + EN_LABEL_Y, C_LABEL, false);
        String es = big(energy) + " FE";
        g.drawString(font, es, x + EN_X + EN_W - font.width(es), y + EN_LABEL_Y, C_VALUE, false);
        int ew = (int) ((long) (EN_W - 2) * energy / HeavyWaterColumn.BUFFER);
        if (ew > 0) bars.energy(g, x + EN_X + 1, y + EN_Y + 1, ew);
    }

    /** The state line from the numbers of the window: the window and Jade say the same. */
    public static StatusLine status(IntUnaryOperator d) {
        int tiers = d.applyAsInt(D_TIERS);
        double speed = d.applyAsInt(D_SPEED) / 1000.0;
        boolean on = d.applyAsInt(D_WORKING) == 1;
        Problem problem = Problem.values()[Math.min(Problem.values().length - 1, Math.max(0, d.applyAsInt(D_PROBLEM)))];
        if (on && speed < HeavyWaterColumn.fullSpeed(tiers) - 1e-6) return new StatusLine(Component.translatable("gui.voltex_nuclear.column.short"), C_WARN);
        if (on) return new StatusLine(Component.translatable("gui.voltex_nuclear.column.running"), C_RUN);
        return switch (problem) {
            case NOT_FORMED -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.not_formed"), C_BAD);
            case NO_HEAT -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.no_heat"), C_WARN);
            case NO_COOLANT -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.no_coolant"), C_WARN);
            case NO_WATER -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.no_water"), C_WARN);
            case OUTPUT_FULL -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.output_full"), C_BAD);
            case NO_ENERGY -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.no_energy"), C_WARN);
            default -> new StatusLine(Component.translatable("gui.voltex_nuclear.column.idle"), C_LABEL);
        };
    }

    private void drawReadout(GuiGraphics g, int x, int y) {
        int tiers = menu.get(D_TIERS);
        double speed = menu.get(D_SPEED) / 1000.0;
        boolean on = menu.get(D_WORKING) == 1;
        StatusLine status = status(menu::get);
        g.drawString(font, status.text(), x + INFO_X, y + 16, status.color(), true);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.column.tiers", tiers, HeavyWaterColumn.MAX_TIERS), x + INFO_X, y + 28, C_VALUE, false);
        g.drawString(font, (on ? HeavyWaterColumn.energyPerTick(tiers) : 0) + " FE/t", x + INFO_X, y + 38, C_ENERGY, false);
        int heat = menu.getWide(D_HEAT);
        line(g, x, y + 54, "gui.voltex_nuclear.column.heat", decimal(heat / 1000.0, 1) + "k/" + decimal(HeavyWaterColumn.HEAT_THRESHOLD / 1000.0, 1) + "k",
                heat < HeavyWaterColumn.HEAT_THRESHOLD ? C_WARN : C_HEAT);
        int used = menu.get(D_COOL_USED), need = menu.get(D_COOL_NEED);
        line(g, x, y + 64, "gui.voltex_nuclear.column.cold", decimal(used / 100.0, 2) + "/" + decimal(need / 100.0, 2), used < need ? C_WARN : C_COOL);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.column.per_tick"), x + INFO_X, y + 80, C_LABEL, false);
        line(g, x, y + 90, "gui.voltex_nuclear.column.water", decimal(HeavyWaterColumn.water(speed), 1), C_VALUE);
        line(g, x, y + 100, "gui.voltex_nuclear.column.heavy", decimal(HeavyWaterColumn.heavyWater(speed), 2), C_HEAVY);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.column.heavy_mb"), x + INFO_X, y + 116, C_LABEL, false);
        line(g, x, y + 126, "gui.voltex_nuclear.column.per_minute", String.valueOf(Math.round(HeavyWaterColumn.heavyWater(speed) * 1200)), C_VALUE);
        line(g, x, y + 136, "gui.voltex_nuclear.column.made", big(menu.getWide(D_MADE)), C_VALUE);
    }

    private void line(GuiGraphics g, int x, int y, String key, String value, int color) {
        g.drawString(font, Component.translatable(key), x + INFO_X, y, C_LABEL, false);
        g.drawString(font, value, x + INFO_R - font.width(value), y, color, false);
    }

    private static boolean in(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /** Hints over the parts of the window. */
    private void tooltips(GuiGraphics g, int px, int py, int mx, int my) {
        int lx = leftPos, ty = topPos + TANK_Y;
        if (in(px, py, WATER_X, TANK_Y, TANK_W, TANK_H))   // his tank tooltip (FluidGauge), water by its own name
            VoltexBars.tank(g, font, mx, my, lx + WATER_X, ty, TANK_W, TANK_H, WATER, menu.get(D_WATER), HeavyWaterColumn.WATER_TANK);
        else if (in(px, py, HEAT_X, TANK_Y, TANK_W, TANK_H))   // as his late cores say the heat and what they need
            g.renderTooltip(font, Component.translatable("gui.industrial_core.late_core.heat_need",
                    VoltexBars.num(menu.getWide(D_HEAT)), VoltexBars.num(HeavyWaterColumn.HEAT_THRESHOLD)), mx, my);
        else if (in(px, py, COOL_X, TANK_Y, TANK_W, TANK_H))
            g.renderTooltip(font, Component.translatable("gui.voltex_nuclear.column.coolant_tip", COOLANT,
                    decimal(menu.get(D_COOL_USED) / 100.0, 2), decimal(menu.get(D_COOL_NEED) / 100.0, 2)), mx, my);
        else if (in(px, py, HEAVY_X, TANK_Y, TANK_W, TANK_H))
            VoltexBars.tank(g, font, mx, my, lx + HEAVY_X, ty, TANK_W, TANK_H, HEAVY, menu.getWide(D_HEAVY), menu.getWide(D_HEAVY_ROOM));
        else if (in(px, py, STRIP_X, STRIP_Y, STRIP_W, STRIP_H)) {
            int tiers = menu.get(D_TIERS);
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.voltex_nuclear.column.tiers_tip", tiers, HeavyWaterColumn.MAX_TIERS),
                    Component.translatable("gui.voltex_nuclear.column.tiers_tip2", HeavyWaterColumn.hot(tiers), HeavyWaterColumn.cold(tiers),
                            HeavyWaterColumn.MIN_TIERS)), mx, my);
        } else if (in(px, py, EN_LABEL_X, EN_LABEL_Y, EN_X + EN_W - EN_LABEL_X, 17))   // his tooltip, over the label and the bar
            Bars.energy(g, font, mx, my, lx + EN_LABEL_X, topPos + EN_LABEL_Y, EN_X + EN_W - EN_LABEL_X, 17, menu.getWide(D_ENERGY), HeavyWaterColumn.BUFFER);
    }
}
