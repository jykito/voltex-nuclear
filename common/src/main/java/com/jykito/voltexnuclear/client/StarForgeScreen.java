package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.starforge.StarForge;
import com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity.Status;
import com.jykito.voltexnuclear.starforge.StarForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import static com.jykito.voltexnuclear.client.VoltexBars.big;
import static com.jykito.voltexnuclear.client.VoltexBars.decimal;
import static com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity.*;

/**
 * The star forge window (DESIGN step 28, the author's O-4 "Panorama" on :8781/nuclear_forge_gui.html, after O-1 "is
 * somewhat bare"): the column's thermometer on the left, the star heat on its long tick; across the top a window into the
 * dome where the star of the model stands on its crown inside its two bands, smouldering while it is cold, burning in
 * steps over the star heat, lights running round the bands while it melts; under it the slots in and out with the
 * chevrons of the Voltex late cores; the readout in two columns at the bottom. The positions are those forge_gui.js writes
 * next to the texture, the drawing that of the mock-up page.
 */
public class StarForgeScreen extends AbstractContainerScreen<StarForgeMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/star_forge.png");
    private static final int TEX_W = 256, TEX_H = 406;

    // layout, in window pixels, as forge_gui.js draws it
    private static final int TANK_X = 22, TANK_Y = 22, TANK_W = 16, TANK_H = 136, GLASS_U = 8, GLASS_V = 269;
    /** The thermometer in the tank: the mercury under the window, the tube's left edge and the bulb's height in the tank. */
    private static final int MERCURY_U = 28, MERCURY_V = 269, TUBE = 6, BULB = 6;
    private static final int C_TOP = 0xFFFFF4D0, C_TOP2 = 0xFFFFD88A;
    private static final int DOME_X = 50, DOME_Y = 17, DOME_W = 188, DOME_H = 72;
    private static final int STAR_X = 144, STAR_Y = 50, STAR_R = 15, BAND_RX = 70, BAND_RY = 14;
    public static final int PROG_X = 114, PROG_Y = 102, PROG_W = 58, PROG_H = 8;
    private static final int INFO_X = 50, INFO_R = 238, INFO_Y = 124, INFO_GAP = 8, INFO_HALF = (INFO_R - INFO_X - INFO_GAP) / 2;

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_HEAT = 0xFFFF9A3C, C_OK = 0xFF6BE06B,
            C_WARN = 0xFFFFB347, C_BAD = 0xFFFF5A4A, C_OFF = 0xFF6E6E6E;

    /** The star: its tones from the core out, the two discs of its halo while it burns. */
    private static final int[] STAR = {0xFFFFFBE8, 0xFFFFE9A0, 0xFFFFC24A, 0xFF8A5A10};
    private static final int[] HALO = {0xFF2A1806, 0xFF4A2C0A};
    /** The bands: behind the star, in front of it (cold and hot), the shade under it, the running light and its tail. */
    private static final int B_BACK = 0xFF2A2112, B_FRONT = 0xFF6A5228, B_LIT = 0xFFB48A3C, B_SHADE = 0xFF3A2C14, B_HEAD = 0xFFFFF0B0;
    private static final int[] B_TAIL = {0xFFFFD27A, 0xFFE0A040, 0xFFB07A2C};
    /** The two bands, tilted either way, the second's light half a turn behind the first's. */
    private static final double[][] BANDS = {{0.32, 0}, {-0.32, 0.5}};
    private static final List<List<int[]>> BAND_PIXELS = new ArrayList<>();

    static {
        for (double[] b : BANDS) BAND_PIXELS.add(bandPixels(b[0]));
    }

    private final VoltexBars bars = new VoltexBars();
    private int ticks;

    public StarForgeScreen(StarForgeMenu menu, Inventory inv, Component title) {
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
        thermometer(g, x + TANK_X, y + TANK_Y);
        VoltexBars.overlay(g, TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
        dome(g, x, y);
        int pw = Math.min(PROG_W - 2, (PROG_W - 2) * menu.get(D_DONE) / 1000);
        if (pw > 0) bars.progress(g, x + PROG_X + 1, y + PROG_Y + 1, pw);
        drawReadout(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    private Status status() {
        return state(menu.get(D_STATUS));
    }

    private static Status state(int s) {
        return Status.values()[Math.min(Status.values().length - 1, Math.max(0, s))];
    }

    /** The state line from the numbers of the window: the window and Jade say the same. */
    public static StatusLine status(IntUnaryOperator d) {
        int heat = StatusLine.wide(d, D_HEAT);
        return switch (state(d.applyAsInt(D_STATUS))) {
            case MELTING -> line("melting", C_OK);
            case NO_INPUT -> line("no_input", C_WARN);
            case OUTPUT_FULL -> line("output_full", C_BAD);
            default -> d.applyAsInt(D_HEAT_PORTS) == 0 ? line("no_heat_port", C_BAD) : heat > 0 ? line("short", C_WARN) : line("cold", C_OFF);
        };
    }

    private static StatusLine line(String key, int color) {
        return new StatusLine(Component.translatable("gui.voltex_nuclear.star_forge." + key), color);
    }

    private static void px(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }

    /** A colour at the step of light k. */
    private static int tone(int c, double k) {
        int r = (int) Math.min(255, Math.round((c >> 16 & 0xFF) * k)), gr = (int) Math.min(255, Math.round((c >> 8 & 0xFF) * k)),
                b = (int) Math.min(255, Math.round((c & 0xFF) * k));
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    /**
     * The heat as the column's thermometer, on the scale of one of his heat ports: the mercury uncovered from the bottom up,
     * the star heat on the scale's long tick; its top lit, and trembling a pixel while the forge melts.
     */
    private void thermometer(GuiGraphics g, int x, int y) {
        int heat = menu.getWide(D_HEAT);
        if (heat <= 0) return;
        int level = (int) Math.round((double) TANK_H * Math.min(heat, HeatPortBlockEntity.MAX_HEAT) / HeatPortBlockEntity.MAX_HEAT);
        boolean tremble = status() == Status.MELTING && ticks / 3 % 4 == 1;
        int top = Math.max(BULB, Math.min(TANK_H - 2, level + (tremble ? 1 : 0)));
        VoltexBars.overlay(g, TEXTURE, x, y + TANK_H - top, MERCURY_U, MERCURY_V + TANK_H - top, TANK_W, top, TEX_W, TEX_H);
        if (top > BULB) {
            px(g, x + TUBE + 1, y + TANK_H - top, C_TOP);
            px(g, x + TUBE + 2, y + TANK_H - top, C_TOP2);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The dome: the crown, the bands behind the star, the star, the bands in front of it
    // ------------------------------------------------------------------------------------------------------------------

    private void dome(GuiGraphics g, int x, int y) {
        int heat = menu.getWide(D_HEAT);
        boolean hot = heat >= StarForge.STAR_HEAT, melting = status() == Status.MELTING;
        crown(g, x, y);
        bands(g, x, y, false, hot, melting);
        double k = !hot ? Math.max(0.18, 0.18 + 0.42 * heat / StarForge.STAR_HEAT) : melting ? new double[]{1, 0.86, 1, 0.92}[ticks / 3 % 4] : 1;
        star(g, x + STAR_X, y + STAR_Y, k, hot);
        bands(g, x, y, true, hot, melting);
    }

    /** The holder of the star at the bottom of the dome, as the crown of the model: a plinth of his greys, a stand, two horns. */
    private static void crown(GuiGraphics g, int x, int y) {
        int cx = x + STAR_X, b = y + DOME_Y + DOME_H - 2, pw = (int) Math.round(Math.min(44, DOME_W * 0.32));
        int[] plinth = {0xFF5D5D5D, 0xFF3A3A3A, 0xFF2C2C2C, 0xFF232323, 0xFF1C1C1C};
        for (int i = 0; i < plinth.length; i++) g.fill(cx - pw / 2 - i, b - 5 + i, cx + pw / 2 + i, b - 4 + i, plinth[i]);
        g.fill(cx - pw / 2, b - 5, cx + pw / 2, b - 4, 0xFF7A7A7A);
        int top = (int) Math.round(y + STAR_Y + STAR_R * 0.9);
        for (int sy = top; sy < b - 5; sy++) {
            g.fill(cx - 1, sy, cx + 2, sy + 1, 0xFF4A4A4A);
            px(g, cx - 1, sy, 0xFF6A6A6A);
        }
        int reach = (int) Math.round(STAR_R * 0.75), up = (int) Math.round(STAR_R * 0.55);
        for (int side = -1; side <= 1; side += 2)
            for (int i = 0; i <= reach; i++) {
                int hx = cx + side * i, hy = (int) Math.round(top - up * Math.sin(Math.PI / 2 * i / reach));
                px(g, hx, hy, 0xFF6A6A6A);
                px(g, hx, hy + 1, 0xFF3A3A3A);
            }
    }

    /** The star: a halo in two crisp discs while it is hot, rays (two pixels thick near the core), shorter diagonals, a core. */
    private static void star(GuiGraphics g, int cx, int cy, double k, boolean burning) {
        if (burning)
            for (int h = 0; h < HALO.length; h++) {
                double r = STAR_R * (h == 0 ? 0.62 : 0.4);
                for (int y = -(int) Math.ceil(r); y <= r; y++)
                    for (int x = -(int) Math.ceil(r); x <= r; x++) if (x * x + y * y <= r * r) px(g, cx + x, cy + y, HALO[h]);
            }
        for (int r = -STAR_R; r <= STAR_R; r++) {
            int c = tone(ray(Math.abs(r)), k);
            px(g, cx + r, cy, c);
            px(g, cx, cy + r, c);
            if (Math.abs(r) < STAR_R * 0.45) {
                int c2 = tone(ray(Math.abs(r) + 2), k);
                px(g, cx + r, cy + 1, c2);
                px(g, cx + 1, cy + r, c2);
            }
        }
        int d = (int) Math.round(STAR_R * 0.55);
        for (int r = -d; r <= d; r++) {
            int c = tone(ray(Math.abs(r) * 1.6), k);
            px(g, cx + r, cy + r, c);
            px(g, cx + r, cy - r, c);
        }
        for (int y = -3; y <= 3; y++)
            for (int x = -3; x <= 3; x++) {
                int m = Math.abs(x) + Math.abs(y);
                if (m <= 4) px(g, cx + x, cy + y, tone(m < 2 ? STAR[0] : m < 4 ? STAR[1] : STAR[2], k));
            }
    }

    private static int ray(double d) {
        return d < STAR_R * 0.25 ? STAR[0] : d < STAR_R * 0.5 ? STAR[1] : d < STAR_R * 0.8 ? STAR[2] : STAR[3];
    }

    /** A band round the star: an ellipse tilted by tilt, its pixels in order round it, each with whether it is in front. */
    private static List<int[]> bandPixels(double tilt) {
        List<int[]> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int n = (int) Math.round(2 * Math.PI * BAND_RX * 1.5);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n, ex = BAND_RX * Math.cos(a), ey = BAND_RY * Math.sin(a);
            int x = (int) Math.round(STAR_X + ex * Math.cos(tilt) - ey * Math.sin(tilt)), y = (int) Math.round(STAR_Y + ex * Math.sin(tilt) + ey * Math.cos(tilt));
            if (seen.add((long) x << 32 | (y & 0xFFFFFFFFL))) out.add(new int[]{x, y, Math.sin(a) > 0 ? 1 : 0});
        }
        return out;
    }

    /** One half of the bands, behind or in front of the star: the front lit while it is hot, a light running round while it melts. */
    private void bands(GuiGraphics g, int x, int y, boolean front, boolean hot, boolean melting) {
        int step = ticks / 2;
        for (int b = 0; b < BANDS.length; b++) {
            List<int[]> pixels = BAND_PIXELS.get(b);
            int n = pixels.size(), head = (int) Math.floor(step + BANDS[b][1] * n) % n;
            for (int i = 0; i < n; i++) {
                int[] p = pixels.get(i);
                if ((p[2] == 1) != front) continue;
                int c = front ? (hot ? B_LIT : B_FRONT) : B_BACK;
                if (melting) {
                    int d = (head - i + n) % n;
                    if (d == 0) c = B_HEAD;
                    else if (d <= 5) c = B_TAIL[Math.min(2, (d - 1) >> 1)];
                }
                px(g, x + p[0], y + p[1] + 1, front ? B_SHADE : B_BACK);
                px(g, x + p[0], y + p[1], c);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The readout: the state, the heat and the ingot on the left, the speed, the heat ports and the count on the right
    // ------------------------------------------------------------------------------------------------------------------

    private void drawReadout(GuiGraphics g, int x, int y) {
        int heat = menu.getWide(D_HEAT);
        boolean hot = heat >= StarForge.STAR_HEAT;
        StatusLine status = status(menu::get);
        int left = x + INFO_X, right = left + INFO_HALF + INFO_GAP;
        g.drawString(font, status.text(), left, y + INFO_Y, status.color(), true);
        line(g, left, y + INFO_Y + 12, "heat", decimal(heat / 1000.0, 1) + "k/" + decimal(StarForge.STAR_HEAT / 1000.0, 1) + "k", hot ? C_HEAT : C_WARN);
        line(g, left, y + INFO_Y + 24, "ingot", hot ? Component.translatable("gui.voltex_nuclear.star_forge.seconds",
                Math.round(1 / StarForge.rate(heat) / 20)).getString() : "-", hot ? C_VALUE : C_OFF);
        line(g, right, y + INFO_Y, "speed", hot ? "×" + decimal(StarForge.rate(heat) * StarForge.SLOW_TICKS, 1) : "-", hot ? C_VALUE : C_OFF);
        line(g, right, y + INFO_Y + 12, "heat_ports", String.valueOf(menu.get(D_HEAT_PORTS)), menu.get(D_HEAT_PORTS) > 0 ? C_VALUE : C_BAD);
        line(g, right, y + INFO_Y + 24, "made", big(menu.getWide(D_MADE)), C_VALUE);
    }

    /** A label on the left of its column and its value on the right. */
    private void line(GuiGraphics g, int x, int y, String key, String value, int color) {
        g.drawString(font, Component.translatable("gui.voltex_nuclear.star_forge." + key), x, y, C_LABEL, false);
        g.drawString(font, value, x + INFO_HALF - font.width(value), y, color, false);
    }

    private static boolean in(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /** Hints over the thermometer and the chevrons. */
    private void tooltips(GuiGraphics g, int px, int py, int mx, int my) {
        if (in(px, py, TANK_X, TANK_Y, TANK_W, TANK_H))   // as his late cores say the heat and what they need
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.industrial_core.late_core.heat_need", VoltexBars.num(menu.getWide(D_HEAT)), VoltexBars.num(StarForge.STAR_HEAT)),
                    Component.translatable("gui.voltex_nuclear.star_forge.heat_tip", menu.get(D_HEAT_PORTS), VoltexBars.num(StarForge.TOP_HEAT))), mx, my);
        else if (in(px, py, PROG_X, PROG_Y, PROG_W, PROG_H))
            g.renderTooltip(font, Component.translatable("gui.voltex_nuclear.star_forge.melt_tip", menu.get(D_DONE) / 10), mx, my);
    }
}
