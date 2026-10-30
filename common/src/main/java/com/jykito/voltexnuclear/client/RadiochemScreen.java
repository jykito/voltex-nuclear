package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.screen.Bars;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.radiochem.Radiochem;
import com.jykito.voltexnuclear.core.radiochem.Radiochem.Product;
import com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity.Problem;
import com.jykito.voltexnuclear.radiochem.RadiochemMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.jykito.voltexnuclear.client.VoltexBars.big;
import static com.jykito.voltexnuclear.client.VoltexBars.decimal;
import static com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity.*;

/**
 * The radiochemical plant window (DESIGN step 13, the author chose B on :8781/nuclear_radiochem_gui.html): top left the
 * plant's water tank and the four shares, each gauge filling up rod by rod to the next whole product; under it the spent
 * rods in the ports, the progress, the empty rods and the products in the ports; the energy; on the right the readout.
 * The progress runs in the chevrons of the Voltex late cores, the energy in his animated fill; the plant's light is
 * orange (DESIGN step 17).
 */
public class RadiochemScreen extends AbstractContainerScreen<RadiochemMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/radiochem.png");
    private static final int TEX_W = 256, TEX_H = 325;

    // layout, in window pixels, as on the mock-up
    /** His tank from the crystallizer, as on the electrolyzer; its glass lies under the window, the screen lays it over the water. */
    private static final int TANK_X = 21, TANK_Y = 21, TANK_W = 16, TANK_H = 50, GLASS_U = 8, GLASS_V = 274;
    private static final int GAUGE_X = 39, GAUGE_STEP = 27, GAUGE_Y = 38, GAUGE_H = 33;
    private static final int IN_X = 17, SLOT_Y = 82;
    /** The chevrons; the JEI click area. */
    public static final int PROG_X = 39, PROG_Y = 87, PROG_W = 56, PROG_H = 8;
    /** The outputs: the empty rods, then the products in the order of {@link Product}. */
    private static final int[][] OUT = {{99, 82}, {135, 82}, {99, 100}, {117, 82}, {117, 100}};
    /** The energy bar with his bolt left of it (EN_LABEL_X, where the label above starts too). */
    private static final int EN_X = 23, EN_Y = 135, EN_W = 120, EN_LABEL_X = 17, EN_LABEL_Y = 126, INFO_X = 158, INFO_R = 242;
    /** The gauges from the left: uranium, plutonium, depleted, palladium. */
    private static final Product[] GAUGES = {Product.LOW_ENRICHED, Product.PLUTONIUM, Product.DEPLETED, Product.PALLADIUM};

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC, C_WARN = 0xFFFFB347, C_BAD = 0xFFFF5A4A;
    /** The orange of plutonium and MOX: light, bright, an empty step of a gauge. */
    private static final int O_L = 0xFFFFE0B0, O_B = 0xFFFFB347, O_OFF = 0xFF2E2420;

    /** Water by its own name, as his FluidGauge names a stack of water. */
    private static final Component WATER = Component.translatable("block.minecraft.water");

    private final VoltexBars bars = new VoltexBars();

    public RadiochemScreen(RadiochemMenu menu, Inventory inv, Component title) {
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
        bars.step();
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        drawShares(g, x, y);
        drawSlots(g, x, y);
        drawBars(g, x, y);
        drawReadout(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    /** The water tank at the left, then a gauge per product under its item, a step for every rod of its share. */
    private void drawShares(GuiGraphics g, int x, int y) {
        VoltexBars.fluid(g, x + TANK_X, y + TANK_Y, TANK_W, TANK_H, menu.get(D_WATER), Radiochem.WATER_TANK, VoltexBars.WATER_TINT);
        VoltexBars.overlay(g, TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
        for (int i = 0; i < GAUGES.length; i++) {
            Product p = GAUGES[i];
            int gx = x + GAUGE_X + i * GAUGE_STEP, have = menu.get(D_SHARES + p.ordinal());
            g.renderItem(new ItemStack(product(p)), gx + 4, y + 20);
            int seg = (GAUGE_H - 1 - (p.per - 1)) / p.per;
            for (int s = 0; s < p.per; s++) {
                int sy = y + GAUGE_Y + GAUGE_H - 1 - (s + 1) * (seg + 1) + 1;
                fill(g, gx + 3, sy, 18, seg, s < have ? (s == have - 1 ? O_L : O_B) : O_OFF);
            }
        }
    }

    /** What the ports hold: the spent part taken next in, the empty rods and the products out; an empty output shows its item faintly. */
    private void drawSlots(GuiGraphics g, int x, int y) {
        for (int size = 0; size < RODS.length; size++) {
            int n = menu.get(D_SPENT + size);
            if (n > 0) { slot(g, x + IN_X, y + SLOT_Y, spent(size), n, false); break; }
        }
        slot(g, x + OUT[0][0], y + OUT[0][1], emptyRod(), menu.get(D_OUT), true);
        for (Product p : Product.values()) slot(g, x + OUT[p.ordinal() + 1][0], y + OUT[p.ordinal() + 1][1], product(p), menu.get(D_OUT + p.ordinal() + 1), true);
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
        g.drawString(font, Component.translatable("gui.voltex_nuclear.radiochem.energy"), x + EN_LABEL_X, y + EN_LABEL_Y, C_LABEL, false);
        String es = big(energy) + " FE";
        g.drawString(font, es, x + 143 - font.width(es), y + EN_LABEL_Y, C_VALUE, false);
        int ew = (int) ((long) (EN_W - 2) * energy / Radiochem.BUFFER);
        if (ew > 0) bars.energy(g, x + EN_X + 1, y + EN_Y + 1, ew);
    }

    /** The state line from the numbers of the window: the window and Jade say the same. */
    public static StatusLine status(IntUnaryOperator d) {
        Problem problem = Problem.values()[Math.min(Problem.values().length - 1, Math.max(0, d.applyAsInt(D_PROBLEM)))];
        if (d.applyAsInt(D_WORKING) == 1) return new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.running"), O_B);
        return switch (problem) {
            case NO_RODS -> new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.no_rods"), C_LABEL);
            case NO_WATER -> new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.no_water"), C_WARN);
            case NO_ENERGY -> new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.no_energy"), C_WARN);
            case OUTPUT_FULL -> new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.output_full"), C_BAD);
            case NOT_FORMED -> new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.not_formed"), C_BAD);
            default -> new StatusLine(Component.translatable("gui.voltex_nuclear.radiochem.idle"), C_LABEL);
        };
    }

    private void drawReadout(GuiGraphics g, int x, int y) {
        StatusLine status = status(menu::get);
        g.drawString(font, status.text(), x + INFO_X, y + 16, status.color(), true);
        int rods = menu.get(D_CYCLE);
        Component time = rods > 1
                ? Component.translatable("gui.voltex_nuclear.radiochem.per_part", rods, decimal(Radiochem.ticks(rods) / 20.0, 0))
                : Component.translatable("gui.voltex_nuclear.radiochem.per_rod", decimal(Radiochem.ticks(1) / 20.0, 0));
        g.drawString(font, time, x + INFO_X, y + 28, C_VALUE, false);
        g.drawString(font, menu.get(D_FE) + " FE/t", x + INFO_X, y + 38, C_ENERGY, false);
        line(g, x, y + 54, "gui.voltex_nuclear.radiochem.water", big(menu.get(D_WATER)) + " mB", menu.get(D_WATER) < Radiochem.WATER_PER_ROD ? C_WARN : C_VALUE);
        line(g, x, y + 64, "gui.voltex_nuclear.radiochem.use", big(Radiochem.WATER_PER_ROD) + " mB", C_VALUE);
        line(g, x, y + 80, "gui.voltex_nuclear.radiochem.made", String.valueOf(menu.getWide(D_MADE)), C_VALUE);
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
        if (in(px, py, TANK_X, TANK_Y, TANK_W, TANK_H)) {   // his tank tooltip (FluidGauge), water by its own name
            VoltexBars.tank(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H, WATER, menu.get(D_WATER), Radiochem.WATER_TANK);
            return;
        }
        for (int i = 0; i < GAUGES.length; i++) {
            Product p = GAUGES[i];
            if (!in(px, py, GAUGE_X + i * GAUGE_STEP + 2, 20, 20, GAUGE_Y + GAUGE_H - 20)) continue;
            g.renderComponentTooltip(font, List.of(new ItemStack(product(p)).getHoverName(),
                    Component.translatable("gui.voltex_nuclear.radiochem.share_tip", menu.get(D_SHARES + p.ordinal()), p.per)), mx, my);
            return;
        }
        for (int size = 0; size < RODS.length; size++)
            if (menu.get(D_SPENT + size) > 0) {
                if (in(px, py, IN_X, SLOT_Y, 18, 18)) g.renderTooltip(font, new ItemStack(spent(size)), mx, my);
                break;
            }
        if (in(px, py, OUT[0][0], OUT[0][1], 18, 18)) g.renderTooltip(font, new ItemStack(emptyRod()), mx, my);
        for (Product p : Product.values())
            if (in(px, py, OUT[p.ordinal() + 1][0], OUT[p.ordinal() + 1][1], 18, 18)) g.renderTooltip(font, new ItemStack(product(p)), mx, my);
        if (in(px, py, EN_LABEL_X, EN_LABEL_Y, EN_X + EN_W - EN_LABEL_X, 17))   // his tooltip, over the label and the bar
            Bars.energy(g, font, mx, my, leftPos + EN_LABEL_X, topPos + EN_LABEL_Y, EN_X + EN_W - EN_LABEL_X, 17, menu.getWide(D_ENERGY), Radiochem.BUFFER);
        else if (in(px, py, PROG_X, PROG_Y, PROG_W, PROG_H) && menu.get(D_WORK) > 0)   // as on his late cores
            g.renderTooltip(font, Component.literal(menu.get(D_PROGRESS) + " / " + menu.get(D_WORK) + " t"), mx, my);
    }
}
