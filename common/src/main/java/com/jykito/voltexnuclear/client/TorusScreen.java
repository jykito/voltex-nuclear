package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.ModStyle;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.fusion.Fusion;
import com.jykito.voltexnuclear.core.fusion.Torus.Phase;
import com.jykito.voltexnuclear.core.fusion.Torus.Why;
import com.jykito.voltexnuclear.fusion.TorusMenu;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.jykito.voltexnuclear.client.VoltexBars.big;
import static com.jykito.voltexnuclear.client.VoltexBars.decimal;
import static com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity.*;

/**
 * The torus window (DESIGN step 19; texture and positions by tools/gui_gen/nuclear_textures/torus_gui.js, the mock-up on
 * :8781/nuclear_torus.html): the ring seen from above, its windows lit round as it ignites, a clot of plasma running round
 * with a stepped tail while it burns and the coil next to it flashing, blinking red while it cools; the cells in and out;
 * the buffer all amber with a light mark at the ignition reserve; the four tanks in their fills with bubbles; on the right
 * the readout, the knobs of the tritium share (red marks at the edges of the window) and of the share into heat, the
 * Watcher's cell, the run key. The keys and the knobs go to the core as menu buttons.
 */
public class TorusScreen extends AbstractContainerScreen<TorusMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/torus.png");
    private static final ResourceLocation PLASMA_FILL = VoltexNuclear.id("textures/gui/torus_plasma_fill.png"),
            RESERVE_FILL = VoltexNuclear.id("textures/gui/torus_reserve_fill.png"),
            HEAT_FILL = VoltexNuclear.platform().id("industrial_core", "textures/gui/heat_fill_6x_anim.png");
    private static final ResourceLocation[] TANK_FILLS = {VoltexNuclear.id("textures/gui/torus_deuterium_fill.png"),
            VoltexNuclear.id("textures/gui/torus_tritium_fill.png"), VoltexNuclear.id("textures/gui/torus_condensate_fill.png"),
            VoltexNuclear.id("textures/gui/torus_helium_fill.png")};
    private static final String[] TANKS = {"deuterium", "tritium", "condensate", "helium"};
    private static final int[] TANK_DATA = {D_DEUTERIUM, D_TRITIUM, D_CONDENSATE, D_HELIUM};
    private static final int[] TANK_CAP = {Fusion.FUEL_TANK, Fusion.FUEL_TANK, Fusion.CONDENSATE_TANK, Fusion.HELIUM_TANK};
    private static final int TEX_W = 256, TEX_H = 265;
    // layout, in window pixels, as torus_gui.js draws the texture
    private static final int RING_X = 17, RING_Y = 17, RC_X = 49, RC_Y = 49;
    private static final int BUF_X = 91, BUF_Y = 70, BUF_W = 53, TANK_X = 17, TANK_W = 126, TANK_BAR = 9;
    private static final int[] TANK_Y = {85, 103, 121, 139};
    private static final int SIDE_X = 152, SIDE_W = 90, L_STATUS = 16, L_FE = 27, L_HEAT = 37, L_WINDOW = 48;
    private static final int KNOB_MIX = 60, KNOB_SPLIT = 84, KNOB_BAR_X = SIDE_X + 12, KNOB_BAR_W = 66, KNOB_PLUS_X = SIDE_X + SIDE_W - Keys.KNOB;
    private static final int WATCHER_X = 152, WATCHER_Y = 108, RUN_X = 152, RUN_Y = 131, RUN_W = 90, RUN_H = 12;
    private static final int NONE = -1;

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC, C_HEAT = 0xFFFF9A3C, C_OK = 0xFF6BE06B,
            C_BAD = 0xFFFF5A4A, C_OFF = 0xFF6E6E6E, C_HEATING = 0xFFFFB347, C_RESERVE = 0xFFFFF0C0;
    /** The plasma's pinks on the ring, from dark to the white-hot head; red while it cools. */
    private static final int P_OFF = 0xFF2A1F2A, P_DIM = 0xFF5E1A4A, P_MID = 0xFFC2409C, P_HI = 0xFFFF6AD2, P_HEAD = 0xFFFFD8F3, P_RED = 0xFF7A2030;
    /** The lamp of the run key while the torus is stopped. */
    private static final int LAMP_OFF = 0xFF40182E;

    private final VoltexBars bars = new VoltexBars();
    /** The key held down since the click, drawn sunk while the mouse stays on it. */
    private int pressed = NONE;

    public TorusScreen(TorusMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = TEX_W;
        imageHeight = TEX_H;
        inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
        tooltips(g, mx - leftPos, my - topPos, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        bars.step();
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        drawRing(g, x, y);
        drawBuffer(g, x, y);
        drawTanks(g, x, y);
        drawReadout(g, x, y, mx - x, my - y);
    }

    /**
     * The ring from above at half size, as the mock-up draws it: 16 windows of plasma, 8 coils outside them, the solenoid
     * in the middle. Igniting, the windows light round one by one; burning, a clot runs round with a tail of three steps
     * and the coil it passes flashes; cooling, the windows blink red.
     */
    private void drawRing(GuiGraphics g, int x, int y) {
        Phase ph = phase();
        long ms = System.currentTimeMillis();
        int head = (int) (ms * 12 / 1000 % 16), lit = menu.get(D_HEATING) * 16 / Fusion.IGNITION_TICKS;
        boolean blink = ms / 250 % 2 == 0;
        for (int i = 0; i < 16; i++) {
            double a = Math.PI / 2 + i * Math.PI / 8;
            int wx = x + (int) Math.round(RC_X + 20 * Math.cos(a)) - 2, wy = y + (int) Math.round(RC_Y + 20 * Math.sin(a)) - 2;
            int c = switch (ph) {
                case HEAT -> i < lit ? P_MID : i == lit && blink ? P_HI : P_OFF;
                case LIT -> { int d = (head - i + 16) % 16; yield d == 0 ? P_HEAD : d == 1 ? P_HI : d == 2 ? P_MID : P_DIM; }
                case COOL -> blink ? P_RED : P_OFF;
                default -> P_OFF;
            };
            fill(g, wx - 1, wy - 1, 6, 6, 0xFF141014);
            fill(g, wx, wy, 4, 4, c);
            if (c != P_OFF) fill(g, wx, wy, 4, 1, 0x40FFFFFF);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 2 + Math.PI / 16 + k * Math.PI / 4;
            int cx = x + (int) Math.round(RC_X + 27 * Math.cos(a)) - 2, cy = y + (int) Math.round(RC_Y + 27 * Math.sin(a)) - 2;
            boolean near = ph == Phase.LIT && head / 2 == k;
            fill(g, cx, cy, 5, 5, 0xFF3A4045);
            fill(g, cx + 1, cy + 1, 3, 3, near ? P_HI : 0xFF6F7B82);
        }
        int sx = x + RC_X, sy = y + RC_Y;
        fill(g, sx - 5, sy - 5, 10, 10, 0xFF2C3236);
        fill(g, sx - 5, sy - 3, 10, 1, 0xFF8E9CA2);
        fill(g, sx - 5, sy + 2, 10, 1, 0xFF8E9CA2);
        fill(g, sx - 2, sy - 2, 4, 4, ph == Phase.LIT ? P_HI : ph == Phase.HEAT ? P_MID : 0xFF1B1B1B);
    }

    /** The buffer all in amber, the light mark at the ignition reserve, a red edge while energy is being lost. */
    private void drawBuffer(GuiGraphics g, int x, int y) {
        int inner = BUF_W - 2, w = (int) Math.round(inner * Math.min(1, menu.getWide(D_BUFFER) / Fusion.BUFFER));
        if (w > 0) bars.bar(g, RESERVE_FILL, x + BUF_X + 1, y + BUF_Y + 1, w);
        fill(g, x + BUF_X + 1 + (int) Math.round(inner * Fusion.RESERVE / Fusion.BUFFER), y + BUF_Y + 1, 1, 6, C_RESERVE);
        if (menu.getWide(D_LOST) > 0) fill(g, x + BUF_X + BUF_W - 2, y + BUF_Y + 1, 1, 6, C_BAD);
    }

    /** The tanks: the name and what it holds on the line (cells, the condensate in mB), the fill under it. */
    private void drawTanks(GuiGraphics g, int x, int y) {
        for (int k = 0; k < TANKS.length; k++) {
            int v = menu.get(TANK_DATA[k]), ty = y + TANK_Y[k];
            String s = k == 2 ? big(v) + " mB" : decimal(v / (double) Fusion.CELL, 1);
            g.drawString(font, Component.translatable("gui.voltex_nuclear.torus." + TANKS[k]), x + TANK_X, ty, C_LABEL, false);
            g.drawString(font, s, x + TANK_X + TANK_W - font.width(s), ty, C_VALUE, false);
            int w = (int) Math.round((TANK_W - 2) * Math.min(1.0, v / (double) TANK_CAP[k]));
            if (w > 0) bars.bar(g, TANK_FILLS[k], x + TANK_X + 1, ty + TANK_BAR + 1, w);
        }
    }

    /** The state line from the numbers of the window: the window and Jade say the same. */
    public static StatusLine status(IntUnaryOperator d) {
        Phase ph = phase(d.applyAsInt(D_PHASE));
        Component text = switch (ph) {
            case OFF -> Component.translatable("gui.voltex_nuclear.torus.status.off");
            case WAIT -> Component.translatable("gui.voltex_nuclear.torus.why." + why(d.applyAsInt(D_WHY)).name().toLowerCase());
            case HEAT -> Component.translatable("gui.voltex_nuclear.torus.status.heat", d.applyAsInt(D_HEATING) * 100 / Fusion.IGNITION_TICKS);
            case LIT -> Component.translatable("gui.voltex_nuclear.torus.status.lit");
            case COOL -> Component.translatable("gui.voltex_nuclear.torus.status.cool", (Fusion.GRACE - d.applyAsInt(D_COOLING) + 19) / 20);
        };
        return new StatusLine(text, switch (ph) { case HEAT -> C_HEATING; case LIT -> P_HI; case COOL -> C_BAD; case WAIT -> C_LABEL; default -> C_OFF; });
    }

    private void drawReadout(GuiGraphics g, int x, int y, int px, int py) {
        StatusLine status = status(menu::get);
        g.drawString(font, status.text(), x + SIDE_X, y + L_STATUS, status.color(), true);
        g.drawString(font, big(menu.getWide(D_FE)) + " FE/t", x + SIDE_X, y + L_FE, C_ENERGY, false);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.reactor.heat", big(menu.getWide(D_HEAT))), x + SIDE_X, y + L_HEAT, C_HEAT, false);
        double k = knowledge();
        g.drawString(font, Component.translatable("gui.voltex_nuclear.torus.window", Math.round(Fusion.low(k) * 100), Math.round(Fusion.high(k) * 100)),
                x + SIDE_X, y + L_WINDOW, C_VALUE, false);

        knob(g, x, y, px, py, KNOB_MIX, Component.translatable("gui.voltex_nuclear.torus.mix", menu.get(D_MIX)), menu.get(D_MIX), PLASMA_FILL,
                new double[]{Fusion.low(k), Fusion.high(k)}, B_MIX_DOWN, B_MIX_UP);
        knob(g, x, y, px, py, KNOB_SPLIT, Component.translatable("gui.voltex_nuclear.reactor.split", menu.get(D_SPLIT)), menu.get(D_SPLIT), HEAT_FILL,
                new double[0], B_SPLIT_DOWN, B_SPLIT_UP);
        drawWatcher(g, x, y);

        boolean running = menu.get(D_RUNNING) != 0, hRun = in(px, py, RUN_X, RUN_Y, RUN_W, RUN_H);
        Keys.key(g, font, x + RUN_X, y + RUN_Y, RUN_W, RUN_H, Component.translatable(running ? "gui.voltex_nuclear.reactor.stop" : "gui.voltex_nuclear.reactor.start"),
                running ? P_HI : LAMP_OFF, running, hRun, hRun && pressed == B_RUN);
    }

    /** A knob: its line, the - and + keys, its fill; red marks where the window of the plasma ends. */
    private void knob(GuiGraphics g, int x, int y, int px, int py, int top, Component label, int value, ResourceLocation tex, double[] marks, int down, int up) {
        g.drawString(font, label, x + SIDE_X, y + top, C_LABEL, false);
        boolean hDown = in(px, py, SIDE_X, top + 10, Keys.KNOB, Keys.KNOB), hUp = in(px, py, KNOB_PLUS_X, top + 10, Keys.KNOB, Keys.KNOB);
        Keys.knob(g, x + SIDE_X, y + top + 10, false, hDown, hDown && pressed == down);
        Keys.knob(g, x + KNOB_PLUS_X, y + top + 10, true, hUp, hUp && pressed == up);
        int inner = KNOB_BAR_W - 2, w = Math.round(inner * value / 100f);
        if (w > 0) bars.bar(g, tex, x + KNOB_BAR_X + 1, y + top + 11, w);
        for (double m : marks) fill(g, x + KNOB_BAR_X + 1 + (int) Math.round(inner * m) - (m >= 1 ? 1 : 0), y + top + 10, 1, 8, C_BAD);
    }

    /** The Watcher's cell, dark as the reactor's; its name and what it does: nothing, watching, or knocking the mix off. */
    private void drawWatcher(GuiGraphics g, int x, int y) {
        int cx = x + WATCHER_X, cy = y + WATCHER_Y;
        fill(g, cx, cy, 18, 18, 0xFF1B1B1B);
        fill(g, cx + 1, cy + 1, 17, 17, 0xFF6E6E6E);
        fill(g, cx + 1, cy + 1, 16, 16, 0xFF2C2C2C);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.reactor.watcher"), cx + 22, cy + 1, C_LABEL, false);
        boolean empty = menu.getSlot(WATCHER).getItem().isEmpty(), knocked = menu.get(D_JITTER) != 0;
        Component line = Component.translatable(empty ? "gui.voltex_nuclear.reactor.watcher.empty"
                : knocked ? "gui.voltex_nuclear.torus.watcher.knock" : "gui.voltex_nuclear.reactor.watcher.watching");
        g.drawString(font, line, cx + 22, cy + 10, empty ? C_OFF : knocked ? C_BAD : C_OK, false);
    }

    /** The texts are drawn with the background; the title and the inventory label stay off, there is no room for them. */
    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    // ------------------------------------------------------------------------------------------------------------------
    // Clicks and hints
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int id = buttonAt((int) mx - leftPos, (int) my - topPos);
            if (id >= 0) {
                pressed = id;
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) pressed = NONE;
        return super.mouseReleased(mx, my, button);
    }

    /** The menu button under the mouse, or {@link #NONE}. A click on a knob's bar sets it to the nearest 5 %. */
    private int buttonAt(int px, int py) {
        if (in(px, py, RUN_X, RUN_Y, RUN_W, RUN_H)) return B_RUN;
        for (int[] k : new int[][]{{KNOB_MIX, B_MIX_DOWN, B_MIX_UP, B_MIX_SET}, {KNOB_SPLIT, B_SPLIT_DOWN, B_SPLIT_UP, B_SPLIT_SET}}) {
            int top = k[0] + 10;
            if (in(px, py, SIDE_X, top, Keys.KNOB, Keys.KNOB)) return k[1];
            if (in(px, py, KNOB_PLUS_X, top, Keys.KNOB, Keys.KNOB)) return k[2];
            if (in(px, py, KNOB_BAR_X, top, KNOB_BAR_W, 9)) {
                double f = Math.max(0, Math.min(1, (px - KNOB_BAR_X - 1) / (double) (KNOB_BAR_W - 2)));
                return k[3] + (int) Math.round(f * KNOB_STEPS);
            }
        }
        return NONE;
    }

    private void tooltips(GuiGraphics g, int px, int py, int mx, int my) {
        List<Component> l = new ArrayList<>();
        if (in(px, py, BUF_X - 6, BUF_Y, BUF_W + 6, 8)) {
            // his bar line first (Bars.energy), then the reserve, what goes out and what is lost
            int buf = menu.getWide(D_BUFFER);
            l.add(Component.translatable("gui.industrial_core.bar.energy", VoltexBars.num(buf), VoltexBars.num((int) Fusion.BUFFER)).withStyle(ModStyle.ENERGY_STYLE));
            l.add(Component.translatable("gui.voltex_nuclear.torus.reserve", big((int) Math.min(buf, Fusion.RESERVE)), big((int) Fusion.RESERVE)).withStyle(s -> s.withColor(C_RESERVE)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.buffer_out", big(menu.getWide(D_GIVEN))));
            if (menu.getWide(D_LOST) > 0) l.add(Component.translatable("gui.voltex_nuclear.reactor.buffer_lost", big(menu.getWide(D_LOST))).withStyle(s -> s.withColor(C_BAD)));
        } else if (in(px, py, SIDE_X, L_STATUS, SIDE_W, L_WINDOW + 9 - L_STATUS)) {
            Phase ph = phase();
            if (ph == Phase.COOL) l.add(Component.translatable("gui.voltex_nuclear.torus.why." + why().name().toLowerCase()).withStyle(s -> s.withColor(C_BAD)));
            int plasma = menu.get(D_PLASMA);
            if (plasma != 0xFFFF) l.add(Component.translatable("gui.voltex_nuclear.torus.plasma", Math.round(plasma / 10.0), Math.round(menu.get(D_YIELD) / 10.0)));
            l.add(Component.translatable("gui.voltex_nuclear.torus.window_tip"));
        } else if (in(px, py, SIDE_X, KNOB_MIX, SIDE_W, 20)) {
            l.add(Component.translatable("gui.voltex_nuclear.torus.mix_tip", Fusion.GRACE / 20));
        } else if (in(px, py, SIDE_X, KNOB_SPLIT, SIDE_W, 20)) {
            l.add(Component.translatable("gui.voltex_nuclear.reactor.split_tip", ReactorCoreBlockEntity.FE_PER_HEAT));
        } else {
            for (int k = 0; k < TANKS.length; k++) {
                if (!in(px, py, TANK_X, TANK_Y[k], TANK_W, TANK_BAR + 8)) continue;
                Component name = Component.translatable("gui.voltex_nuclear.torus." + TANKS[k]);
                int v = menu.get(TANK_DATA[k]);
                if (k == 2) {
                    l.add(Component.literal("").append(name).append(": " + VoltexBars.num(v) + " / " + VoltexBars.num(TANK_CAP[k]) + " mB"));
                    l.add(Component.translatable("gui.voltex_nuclear.torus.condensate_tip", decimal(Fusion.CONDENSATE * 20, 1)));
                } else {
                    l.add(Component.translatable("gui.voltex_nuclear.torus.cells", name, decimal(v / (double) Fusion.CELL, 1), TANK_CAP[k] / Fusion.CELL));
                }
            }
        }
        if (!l.isEmpty()) g.renderComponentTooltip(font, l, mx, my);
    }

    private Phase phase() {
        return phase(menu.get(D_PHASE));
    }

    private static Phase phase(int s) {
        return s >= 0 && s < Phase.values().length ? Phase.values()[s] : Phase.OFF;
    }

    private Why why() {
        return why(menu.get(D_WHY));
    }

    private static Why why(int s) {
        return s >= 0 && s < Why.values().length ? Why.values()[s] : Why.NONE;
    }

    private double knowledge() {
        return menu.get(D_KNOWLEDGE) / 1000.0;
    }

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private static boolean in(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
