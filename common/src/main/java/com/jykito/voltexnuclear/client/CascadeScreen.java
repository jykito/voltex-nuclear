package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.screen.Bars;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity.Problem;
import com.jykito.voltexnuclear.cascade.CascadeMenu;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.cascade.Cascade;
import com.jykito.voltexnuclear.core.cascade.Cascade.Run;
import com.jykito.voltexnuclear.core.cascade.Cascade.Stage;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity.*;
import static com.jykito.voltexnuclear.client.VoltexBars.big;
import static com.jykito.voltexnuclear.client.VoltexBars.decimal;

/**
 * The cascade core window (DESIGN step 3, mock-up :8781/nuclear_cascade.html): top left the row as the core sees it, its
 * columns turning, the empty places up to twelve dotted, under it the enrichment strip with red marks at 4 and 10 columns;
 * the yellowcake, the cycle and the uranium in the ports; the energy; the stage buttons; on the right the readout.
 * The progress runs in the chevrons of the Voltex late cores, the energy in his animated fill.
 */
public class CascadeScreen extends AbstractContainerScreen<CascadeMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/cascade.png");
    private static final int TEX_W = 256, TEX_H = 270;

    // layout, in window pixels
    private static final int ROW_X = 17, ROW_Y = 17, ROW_W = 132, ROW_H = 58, C0 = 28, PITCH = 10;
    private static final int KEK_X = 17, PROD_X = 99, DEP_X = 121, SLOT_Y = 82;
    /** The chevrons; the JEI click area. */
    public static final int PROG_X = 39, PROG_Y = 87, PROG_W = 56, PROG_H = 8;
    /** The energy bar with his bolt left of it (EN_LABEL_X, where the label above starts too). */
    private static final int EN_X = 23, EN_Y = 115, EN_W = 120, EN_LABEL_X = 17;
    private static final int BTN_Y = 144, BTN_W = 63, BTN_H = 20, INFO_X = 152, INFO_R = 242;
    private static final int[] BTN_X = {17, 81};

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC, C_OK = 0xFF7DDC4B, C_WARN = 0xFFFFB347, C_BAD = 0xFFFF5A4A;
    /** The uranium green of the add-on: light, bright, mid, dark, off. */
    private static final int G_L = 0xFFD9FFC0, G_B = 0xFF7DDC4B, G_M = 0xFF5AA834, G_D = 0xFF2F5A1A, G_OFF = 0xFF1F2A1A;
    /** The strip under the row: the enrichment climbs from the left. */
    private static final int[] RAMP = {0xFF24401A, 0xFF2F5A1A, 0xFF3D6E22, 0xFF4A8228, 0xFF5AA834, 0xFF68B83C, 0xFF7DDC4B, 0xFF94E566,
            0xFFA9EC82, 0xFFBFF29E, 0xFFD0F6B6, 0xFFE4FCD6};

    private final VoltexBars bars = new VoltexBars();
    private float spin, angle;
    private long last;

    public CascadeScreen(CascadeMenu menu, Inventory inv, Component title) {
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
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        long now = Util.getMillis();
        float dt = last == 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        boolean working = menu.get(D_WORKING) == 1;
        spin += ((working ? 1 : 0) - spin) * Math.min(1f, dt * 0.7f);
        angle = (angle + spin * dt * 8) % (float) (Math.PI * 2);
        bars.step();

        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        drawRow(g, x, y, working, now / 1000f);
        drawSlots(g, x, y);
        drawBars(g, x, y);
        drawButtons(g, x, y);
        drawReadout(g, x, y, working);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private static int glowAt(float k) {
        return k > 0.66f ? G_B : k > 0.33f ? G_M : k > 0.05f ? G_D : G_OFF;
    }

    /** The row: the core cabinet at the left, the header pipe over the built columns, the rail with its zebra underneath. */
    private void drawRow(GuiGraphics g, int x, int y, boolean on, float t) {
        int n = menu.get(D_COLUMNS);
        fill(g, x + 19, y + 21, 8, 48, 0xFF2C3236);
        fill(g, x + 19, y + 21, 1, 48, 0xFF3E464C);
        fill(g, x + 26, y + 21, 1, 48, 0xFF1D2124);
        fill(g, x + 20, y + 26, 6, 6, 0xFF0B0B0B);
        fill(g, x + 21, y + 27, 4, 4, on ? G_D : 0xFF1B1B1B);
        if (on) fill(g, x + 21 + (int) (t * 3) % 4, y + 30 - (int) (t * 6) % 3, 1, 1, G_L);
        fill(g, x + 21, y + 52, 4, 2, on ? G_B : 0xFF3A2A2A);
        if (n > 0) {
            int lastX = C0 + (n - 1) * PITCH;
            fill(g, x + 23, y + 21, lastX + 5 - 23, 3, 0xFF8E9CA2);
            fill(g, x + 23, y + 23, lastX + 5 - 23, 1, 0xFF5D676D);
        }
        for (int px = 19; px < 147; px += 2) fill(g, x + px, y + 64, 1, 2, (px >> 1) % 2 == 1 ? 0xFFE0B020 : 0xFF1B1B1B);
        fill(g, x + 19, y + 66, 128, 4, 0xFF3A4045);
        fill(g, x + 19, y + 66, 128, 1, 0xFF4C555C);
        for (int i = 0; i < Cascade.MAX_COLUMNS; i++) {
            int cx = x + C0 + i * PITCH;
            if (i >= n) {   // an empty place: a dotted outline
                for (int py = 26; py < 64; py += 2) { fill(g, cx + 2, y + py, 1, 1, 0xFF262B31); fill(g, cx + 7, y + py, 1, 1, 0xFF262B31); }
                for (int px = 2; px < 8; px += 2) { fill(g, cx + px, y + 25, 1, 1, 0xFF262B31); fill(g, cx + px, y + 63, 1, 1, 0xFF262B31); }
                continue;
            }
            fill(g, cx + 4, y + 24, 2, 2, 0xFF8E9CA2);
            fill(g, cx + 2, y + 26, 6, 38, 0xFF262C33);
            fill(g, cx + 2, y + 26, 1, 38, 0xFF3A424A);
            fill(g, cx + 7, y + 26, 1, 38, 0xFF181C21);
            fill(g, cx + 2, y + 29, 6, 1, 0xFF6F7B82);
            fill(g, cx + 2, y + 60, 6, 1, 0xFF6F7B82);
            // the slit runs across the body as it turns, each column a little behind the one before
            double turn = (angle + 0.9 * i) / (Math.PI * 2);
            int pos = (int) ((turn - Math.floor(turn)) * 6);
            int sc = glowAt(spin);
            fill(g, cx + 2 + pos, y + 31, 1, 11, sc);
            fill(g, cx + 2 + pos, y + 47, 1, 11, sc);
            if (spin > 0.33f) {
                int tr = (pos + 5) % 6;
                fill(g, cx + 2 + tr, y + 31, 1, 11, G_D);
                fill(g, cx + 2 + tr, y + 47, 1, 11, G_D);
            }
            fill(g, cx + 1, y + 43, 8, 3, glowAt(spin));
            if (spin > 0.66f) fill(g, cx + 1, y + 43, 8, 1, G_L);
        }
        for (int i = 0; i < Cascade.MAX_COLUMNS; i++) fill(g, x + C0 + i * PITCH, y + 71, PITCH - 1, 2, i < n ? RAMP[i] : 0xFF161A16);
        for (Stage s : Stage.values()) fill(g, x + C0 + s.need * PITCH - 1, y + 69, 1, 5, C_BAD);
    }

    /** The run the window shows: the cycle under way, else what the chosen stage starts next (tails when only they are in). */
    private Run shownRun() {
        int cycle = menu.get(D_CYCLE);
        if (cycle > 0) return Run.byIndex(cycle - 1);
        Stage s = Stage.byIndex(menu.get(D_STAGE));
        return s == Stage.LOW && menu.getWide(D_FEED) < s.feed && menu.get(D_TAILS) >= Run.TAILS.feed() ? Run.TAILS : Run.of(s);
    }

    private static Item product(Stage s) {
        return (s == Stage.LOW ? NuclearItems.LOW_ENRICHED_URANIUM : NuclearItems.HIGH_ENRICHED_URANIUM).get();
    }

    /** The input of the run: the yellowcake, or the depleted uranium on tails. */
    private Item feedItem(Run run) {
        return (run.tails() ? NuclearItems.DEPLETED_URANIUM : NuclearItems.YELLOWCAKE).get();
    }

    private int feedCount(Run run) {
        return run.tails() ? menu.get(D_TAILS) : menu.getWide(D_FEED);
    }

    /**
     * What the ports hold: the yellowcake in (the depleted uranium on tails), the uranium and the depleted uranium out. An
     * empty product slot shows its item faintly.
     */
    private void drawSlots(GuiGraphics g, int x, int y) {
        Run run = shownRun();
        slot(g, x + KEK_X, y + SLOT_Y, feedItem(run), feedCount(run), false);
        slot(g, x + PROD_X, y + SLOT_Y, product(run.stage()), menu.get(D_ENRICHED), true);
        slot(g, x + DEP_X, y + SLOT_Y, NuclearItems.DEPLETED_URANIUM.get(), menu.get(D_DEPLETED), false);
    }

    private void slot(GuiGraphics g, int x, int y, Item item, int count, boolean ghost) {
        if (count > 0) {
            ItemStack stack = new ItemStack(item);
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1, String.valueOf(count));
        } else if (ghost) {
            g.renderItem(new ItemStack(item), x + 1, y + 1);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            fill(g, x + 1, y + 1, 16, 16, 0xB0202020);
            g.pose().popPose();
        }
    }


    private void drawBars(GuiGraphics g, int x, int y) {
        int work = menu.get(D_WORK);
        int pw = work > 0 ? Math.min(PROG_W - 2, (PROG_W - 2) * menu.get(D_PROGRESS) / work) : 0;
        if (pw > 0) bars.progress(g, x + PROG_X + 1, y + PROG_Y + 1, pw);
        int energy = menu.getWide(D_ENERGY);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.cascade.energy"), x + EN_LABEL_X, y + 106, C_LABEL, false);
        String es = big(energy) + " FE";
        g.drawString(font, es, x + 143 - font.width(es), y + 106, C_VALUE, false);
        int ew = (int) ((long) (EN_W - 2) * energy / Cascade.BUFFER);
        if (ew > 0) bars.energy(g, x + EN_X + 1, y + EN_Y + 1, ew);
    }

    /** The stage: two buttons with the product on them; a stage the row cannot make is dim. */
    private void drawButtons(GuiGraphics g, int x, int y) {
        g.drawString(font, Component.translatable("gui.voltex_nuclear.cascade.stage"), x + 17, y + 134, C_LABEL, false);
        int columns = menu.get(D_COLUMNS), chosen = menu.get(D_STAGE);
        for (Stage s : Stage.values()) {
            int bx = x + BTN_X[s.ordinal()], by = y + BTN_Y;
            boolean sel = chosen == s.ordinal(), shut = !s.open(columns);
            fill(g, bx, by, BTN_W, BTN_H, sel ? G_B : 0xFF0B0B0B);
            fill(g, bx + 1, by + 1, BTN_W - 2, BTN_H - 2, sel ? 0xFF2F3A2A : 0xFF2E2E2E);
            g.renderItem(new ItemStack(product(s)), bx + 2, by + 2);
            if (shut) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                fill(g, bx + 2, by + 2, 16, 16, 0xA02E2E2E);
                g.pose().popPose();
            }
            Component label = Component.translatable("gui.voltex_nuclear.cascade.stage." + s.name().toLowerCase());
            g.drawString(font, label, bx + 19 + (BTN_W - 20 - font.width(label)) / 2, by + 6, shut ? 0xFF6E6E6E : sel ? G_L : C_VALUE, !shut);
        }
    }

    /** The state line from the numbers of the window: the window and Jade say the same. */
    public static StatusLine status(IntUnaryOperator d) {
        Problem problem = Problem.values()[Math.min(Problem.values().length - 1, Math.max(0, d.applyAsInt(D_PROBLEM)))];
        if (d.applyAsInt(D_WORKING) == 1) return new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.running"), C_OK);
        return switch (problem) {
            case NO_YELLOWCAKE -> new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.no_yellowcake"), C_LABEL);
            case NO_ENERGY -> new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.no_energy"), C_WARN);
            case OUTPUT_FULL -> new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.output_full"), C_BAD);
            case STAGE_SHUT -> new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.needs_columns", Stage.HIGH.need), C_BAD);
            case NOT_FORMED -> new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.not_formed"), C_BAD);
            default -> new StatusLine(Component.translatable("gui.voltex_nuclear.cascade.idle"), C_LABEL);
        };
    }

    private void drawReadout(GuiGraphics g, int x, int y, boolean working) {
        int n = menu.get(D_COLUMNS);
        StatusLine status = status(menu::get);
        g.drawString(font, status.text(), x + INFO_X, y + 16, status.color(), true);
        Run run = shownRun();
        double v = run.stage().speed(n);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.cascade.columns", n), x + INFO_X, y + 28, C_VALUE, false);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.cascade.speed", decimal(v, 2)), x + INFO_X, y + 38, v >= 1 ? C_VALUE : C_WARN, false);
        g.drawString(font, v > 0 ? Component.translatable("gui.voltex_nuclear.cascade.per_uranium", decimal(run.ticksAt(n) / 20.0, 1))
                : Component.translatable("gui.voltex_nuclear.cascade.no_uranium"), x + INFO_X, y + 48, C_VALUE, false);
        g.drawString(font, menu.get(D_FE) + " FE/t", x + INFO_X, y + 58, C_ENERGY, false);
        line(g, x, y + 74, run.tails() ? "gui.voltex_nuclear.cascade.tails_feed" : "gui.voltex_nuclear.cascade.feed", run.feed());
        line(g, x, y + 84, "gui.voltex_nuclear.cascade.depleted", run.depleted());
        line(g, x, y + 100, "gui.voltex_nuclear.cascade.made", menu.getWide(D_MADE));
    }

    private void line(GuiGraphics g, int x, int y, String key, int value) {
        g.drawString(font, Component.translatable(key), x + INFO_X, y, C_LABEL, false);
        String v = String.valueOf(value);
        g.drawString(font, v, x + INFO_R - font.width(v), y, C_VALUE, false);
    }


    private static boolean in(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int px = (int) mx - leftPos, py = (int) my - topPos;
            for (Stage s : Stage.values()) {
                if (!in(px, py, BTN_X[s.ordinal()], BTN_Y, BTN_W, BTN_H)) continue;
                if (s.open(menu.get(D_COLUMNS)) && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, s.ordinal());
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** Hints over the parts of the window. */
    private void tooltips(GuiGraphics g, int px, int py, int mx, int my) {
        int n = menu.get(D_COLUMNS);
        Run run = shownRun();
        if (in(px, py, KEK_X, SLOT_Y, 18, 18) && feedCount(run) > 0) g.renderTooltip(font, new ItemStack(feedItem(run)), mx, my);
        else if (in(px, py, PROD_X, SLOT_Y, 18, 18)) g.renderTooltip(font, new ItemStack(product(run.stage())), mx, my);
        else if (in(px, py, DEP_X, SLOT_Y, 18, 18) && menu.get(D_DEPLETED) > 0) g.renderTooltip(font, new ItemStack(NuclearItems.DEPLETED_URANIUM.get()), mx, my);
        else if (in(px, py, EN_LABEL_X, 106, EN_X + EN_W - EN_LABEL_X, 17))   // his tooltip, over the label and the bar
            Bars.energy(g, font, mx, my, leftPos + EN_LABEL_X, topPos + 106, EN_X + EN_W - EN_LABEL_X, 17, menu.getWide(D_ENERGY), Cascade.BUFFER);
        else if (in(px, py, ROW_X, ROW_Y, ROW_W, ROW_H))
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.voltex_nuclear.cascade.row_tip", n),
                    Component.translatable("gui.voltex_nuclear.cascade.row_tip2", Stage.LOW.need, Stage.HIGH.need)), mx, my);
        else for (Stage s : Stage.values())
            if (in(px, py, BTN_X[s.ordinal()], BTN_Y, BTN_W, BTN_H)) {
                String key = "gui.voltex_nuclear.cascade.stage." + s.name().toLowerCase() + "_tip";
                g.renderComponentTooltip(font, s.open(n)
                        ? List.of(Component.translatable(key, s.feed, s.depleted()))
                        : List.of(Component.translatable(key, s.feed, s.depleted()), Component.translatable("gui.voltex_nuclear.cascade.stage_shut", s.need)), mx, my);
            }
    }
}
