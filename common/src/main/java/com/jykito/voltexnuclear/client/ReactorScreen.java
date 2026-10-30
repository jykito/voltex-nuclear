package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.ModStyle;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.reactor.FluxMap;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.Status;
import com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.WatcherState;
import com.jykito.voltexnuclear.reactor.ReactorMenu;
import com.jykito.voltexnuclear.reactor.ReactorParts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.jykito.voltexnuclear.client.VoltexBars.big;
import static com.jykito.voltexnuclear.client.VoltexBars.decimal;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.*;

/**
 * The reactor window (DESIGN step 17; texture and positions by tools/gui_gen/nuclear_textures/reactor_gui.js, shown on
 * :8781/nuclear_reactor_gui.html): the zone with its map of flux or heat under the parts, the shut cells dark, a ghost
 * pale in an empty cell that remembers its part; under the zone the buffer in his animated fill and the outputs; on the
 * right the readout, the hull, the knobs of power and of the share into heat, the buttons. The flux and the floor of the
 * power knob come from the zone in the slots, through the core module's {@link FluxMap}, so the server sends no map.
 * The buttons and the knobs go to the core as menu buttons; the map is the window's own choice.
 */
public class ReactorScreen extends AbstractContainerScreen<ReactorMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/reactor.png");
    private static final int TEX_W = 256, TEX_H = 265;
    // layout, in window pixels, as reactor_gui.js draws the texture
    private static final int EN_X = 24, EN_Y = 149, EN_W = 64;
    private static final int SIDE_X = 150, SIDE_W = 88, L_STATUS = 14, L_FE = 25, L_HEAT = 35, L_HULL = 46, HULL_Y = 56, HULL_H = 6;
    // the knob's bar keeps the place the texture gives it (10 px keys and 2 of air); the keys are 9 x 9, "+" at the right edge
    private static final int KNOB_POWER = 64, KNOB_SPLIT = 86, KNOB_BTN = 10, KNOB_BAR_X = SIDE_X + KNOB_BTN + 2, KNOB_BAR_W = 64,
            KNOB_PLUS_X = SIDE_X + SIDE_W - Keys.KNOB;
    private static final int RUN_X = 150, SCRAM_X = 195, BTN_Y = 135, BTN_W = 43, BTN_H = 12, MAP_Y = 150;
    /** What {@link #buttonAt} answers for the map key and for no key. */
    private static final int MAP = -2, NONE = -1;

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC, C_HEAT = 0xFFFF9A3C, C_OK = 0xFF6BE06B,
            C_BAD = 0xFFFF5A4A, C_POWER = 0xFF7DDC4B;
    /** The lamps while off: dark green on the run key, dark red on SCRAM; the grey of the map with none. */
    private static final int LAMP_OFF_GREEN = 0xFF24401A, LAMP_OFF_RED = 0xFF3A1414, LAMP_NONE = 0xFF6A6A6A;

    /** The map under the parts. */
    private enum Map { FLUX, HEAT, NONE }

    private final VoltexBars bars = new VoltexBars();
    /** The window's own choice, kept while the game runs, so a window opened again shows the map it showed. */
    private static Map map = Map.FLUX;
    // the zone of the slots, made again whenever the slots or the chambers change
    private final Zone zone = new Zone();
    private FluxMap flux = FluxMap.of(zone);
    private final Item[] seen = new Item[CELLS];
    private int seenChambers = -1;
    /** The key held down since the click, drawn sunk while the mouse stays on it. */
    private int pressed = NONE;

    public ReactorScreen(ReactorMenu menu, Inventory inv, Component title) {
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

    /** The flux of the zone in the slots, made again only when a cell or a chamber changed. */
    private void updateZone() {
        int chambers = menu.get(D_CHAMBERS);
        boolean changed = chambers != seenChambers;
        for (int i = 0; i < CELLS && !changed; i++) changed = menu.getSlot(i).getItem().getItem() != seen[i];
        if (!changed) return;
        seenChambers = chambers;
        for (Zone.Side s : Zone.Side.values()) zone.setChamber(s, (chambers & 1 << s.ordinal()) != 0);
        for (int i = 0; i < CELLS; i++) {
            ItemStack s = menu.getSlot(i).getItem();
            seen[i] = s.getItem();
            int r = i / Zone.SIZE, c = i % Zone.SIZE;
            Part p = ReactorParts.of(s);
            zone.set(r, c, p != null && zone.open(r, c) ? p : null);
        }
        flux = FluxMap.of(zone);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        bars.step();
        updateZone();
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        drawZone(g, x, y);
        drawBuffer(g, x, y);
        drawReadout(g, x, y, mx - x, my - y);
    }

    /** Under the parts: the shut cells, the map, the load of the cooling cells, the ghosts. */
    private void drawZone(GuiGraphics g, int x, int y) {
        double fmax = 0, lmax = 0;
        for (int i = 0; i < CELLS; i++) {
            fmax = Math.max(fmax, flux.flux(i / Zone.SIZE, i % Zone.SIZE));
            lmax = Math.max(lmax, menu.get(D_LOAD + i));
        }
        for (int i = 0; i < CELLS; i++) {
            int r = i / Zone.SIZE, c = i % Zone.SIZE;
            int cx = x + ReactorMenu.GRID_X + 1 + c * ReactorMenu.STEP, cy = y + ReactorMenu.GRID_Y + 1 + r * ReactorMenu.STEP;
            if (!menu.cellOpen(r, c)) {
                fill(g, cx - 1, cy - 1, 18, 18, 0xC70D0D0D);
                fill(g, cx + 6, cy + 7, 4, 2, 0xFF2A2A2A);
                continue;
            }
            double f = flux.flux(r, c);
            int load = menu.get(D_LOAD + i);
            if (map == Map.FLUX && f > 0) fill(g, cx, cy, 16, 16, alpha(0.25 + 0.55 * f / fmax) | 0x00FFCC);
            if (map == Map.HEAT && load > 0) fill(g, cx, cy, 16, 16, alpha(0.25 + 0.55 * load / lmax) | 0xFF7A3C);
            ItemStack stack = menu.getSlot(i).getItem();
            Part part = ReactorParts.of(stack);
            if (part != null && part.cooling() != null && part.cooling().drinks()) {
                double rate = part.cooling().rate();
                fill(g, cx, cy + 15, 16, 1, 0xFF111111);
                fill(g, cx, cy + 15, (int) Math.round(16 * Math.min(1, load / rate)), 1, part.kind() == Part.Kind.WATER_CELL ? 0xFF5AA7E8 : 0xFF7FD8F0);
            }
            int ghost = menu.get(D_GHOST + i);
            if (!stack.isEmpty() && otherThanGhost(stack, ghost)) {   // a scheme wants another part here
                fill(g, cx - 1, cy - 1, 18, 1, C_BAD);
                fill(g, cx - 1, cy + 16, 18, 1, C_BAD);
                fill(g, cx - 1, cy, 1, 16, C_BAD);
                fill(g, cx + 16, cy, 1, 16, C_BAD);
            }
            if (stack.isEmpty() && ghost > 0 && ghost <= ReactorParts.items().size()) {
                g.renderFakeItem(new ItemStack(ReactorParts.items().get(ghost - 1)), cx, cy);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                fill(g, cx, cy, 16, 16, 0xB0262626);
                g.pose().popPose();
            }
        }
    }

    /** The buffer in his fill; a red edge while energy is being lost. */
    private void drawBuffer(GuiGraphics g, int x, int y) {
        int ew = (int) ((long) (EN_W - 2) * menu.getWide(D_ENERGY) / BUFFER);
        if (ew > 0) bars.energy(g, x + EN_X + 1, y + EN_Y + 1, ew);
        if (menu.getWide(D_LOST) > 0) fill(g, x + EN_X + EN_W - 2, y + EN_Y + 1, 1, 6, C_BAD);
    }

    private void drawReadout(GuiGraphics g, int x, int y, int px, int py) {
        Status st = status();
        Component s = Component.translatable("gui.voltex_nuclear.reactor.status." + st.name().toLowerCase());
        g.drawString(font, s, x + SIDE_X, y + L_STATUS, switch (st) { case RUNNING -> C_OK; case SCRAM, HELD -> C_BAD; default -> C_LABEL; }, true);
        g.drawString(font, big(menu.getWide(D_FE)) + " FE/t", x + SIDE_X, y + L_FE, C_ENERGY, false);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.reactor.heat", big(menu.get(D_ZONE_HEAT))), x + SIDE_X, y + L_HEAT, C_HEAT, false);
        double hull = menu.get(D_HULL) / 1000.0;
        g.drawString(font, Component.translatable("gui.voltex_nuclear.reactor.hull", Math.round(hull * 100)), x + SIDE_X, y + L_HULL, hull > 0.8 ? C_BAD : C_VALUE, false);
        int hw = (int) Math.round((SIDE_W - 2) * Math.min(1, hull));
        if (hw > 0) fill(g, x + SIDE_X + 1, y + HULL_Y + 1, hw, HULL_H - 2, hull > 0.8 ? C_BAD : C_HEAT);

        knob(g, x, y, px, py, KNOB_POWER, Component.translatable("gui.voltex_nuclear.reactor.power", menu.get(D_KNOB)), menu.get(D_KNOB), C_POWER,
                flux.minPower(), B_POWER_DOWN, B_POWER_UP);
        knob(g, x, y, px, py, KNOB_SPLIT, Component.translatable("gui.voltex_nuclear.reactor.split", menu.get(D_SPLIT)), menu.get(D_SPLIT), C_HEAT,
                0, B_SPLIT_DOWN, B_SPLIT_UP);
        drawWatcher(g, x, y);

        // the keys: the lamp of the run key is lit while the reactor runs, the SCRAM lamp blinks after SCRAM, the map's shows its colour
        boolean running = st == Status.RUNNING || st == Status.HELD, scram = st == Status.SCRAM;
        boolean blink = System.currentTimeMillis() / 250 % 2 == 0, hRun = in(px, py, RUN_X, BTN_Y, BTN_W, BTN_H),
                hScram = in(px, py, SCRAM_X, BTN_Y, BTN_W, BTN_H), hMap = in(px, py, SIDE_X, MAP_Y, SIDE_W, BTN_H);
        Keys.key(g, font, x + RUN_X, y + BTN_Y, BTN_W, BTN_H, Component.translatable(running ? "gui.voltex_nuclear.reactor.stop" : "gui.voltex_nuclear.reactor.start"),
                running ? C_POWER : LAMP_OFF_GREEN, running, hRun, hRun && pressed == B_RUN);
        Keys.key(g, font, x + SCRAM_X, y + BTN_Y, BTN_W, BTN_H, Component.translatable(scram ? "gui.voltex_nuclear.reactor.reset" : "gui.voltex_nuclear.reactor.scram"),
                scram && blink ? C_BAD : LAMP_OFF_RED, scram && blink, hScram, hScram && pressed == B_SCRAM);
        Keys.key(g, font, x + SIDE_X, y + MAP_Y, SIDE_W, BTN_H, Component.translatable("gui.voltex_nuclear.reactor.map." + map.name().toLowerCase()),
                switch (map) { case FLUX -> C_ENERGY; case HEAT -> C_HEAT; case NONE -> LAMP_NONE; }, true, hMap, hMap && pressed == MAP);
    }

    /**
     * The Watcher's cell, dark as the nanite cells of the Voltex machines (the industrial miner, the late core), its name
     * and what it does now: nothing in the slot, watching, or easing the power off to what the reactor runs at.
     */
    private void drawWatcher(GuiGraphics g, int x, int y) {
        int cx = x + ReactorMenu.WATCHER_X, cy = y + ReactorMenu.WATCHER_Y;
        fill(g, cx, cy, 18, 18, 0xFF1B1B1B);
        fill(g, cx + 1, cy + 1, 17, 17, 0xFF6E6E6E);
        fill(g, cx + 1, cy + 1, 16, 16, 0xFF2C2C2C);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.reactor.watcher"), cx + 22, cy + 1, C_LABEL, false);
        WatcherState w = watcherState();
        Component line = switch (w) {
            case NONE -> Component.translatable("gui.voltex_nuclear.reactor.watcher.empty");
            case WATCHING -> Component.translatable("gui.voltex_nuclear.reactor.watcher.watching");
            case EASED -> Component.translatable("gui.voltex_nuclear.reactor.watcher.eased", Math.round(menu.get(D_POWER) / 10.0));
        };
        g.drawString(font, line, cx + 22, cy + 10, switch (w) { case NONE -> LAMP_NONE; case WATCHING -> C_OK; case EASED -> C_HEAT; }, false);
    }

    /** A knob: its line, the - and + keys, the fill of its bar; the power knob shows the floor the control rods allow. */
    private void knob(GuiGraphics g, int x, int y, int px, int py, int top, Component label, int value, int color, double floor, int down, int up) {
        g.drawString(font, label, x + SIDE_X, y + top, C_LABEL, false);
        boolean hDown = in(px, py, SIDE_X, top + 10, Keys.KNOB, Keys.KNOB), hUp = in(px, py, KNOB_PLUS_X, top + 10, Keys.KNOB, Keys.KNOB);
        Keys.knob(g, x + SIDE_X, y + top + 10, false, hDown, hDown && pressed == down);
        Keys.knob(g, x + KNOB_PLUS_X, y + top + 10, true, hUp, hUp && pressed == up);
        int w = Math.round((KNOB_BAR_W - 2) * value / 100f);
        if (w > 0) fill(g, x + KNOB_BAR_X + 1, y + top + 12, w, 5, color);
        if (floor > 0) fill(g, x + KNOB_BAR_X + 1 + (int) Math.round((KNOB_BAR_W - 2) * floor) - 1, y + top + 11, 1, 7, C_BAD);
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
            int px = (int) mx - leftPos, py = (int) my - topPos;
            int id = buttonAt(px, py);
            if (id == MAP) {
                pressed = MAP;
                map = Map.values()[(map.ordinal() + 1) % Map.values().length];
                click();
                return true;
            }
            if (id >= 0) {
                pressed = id;
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                click();
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

    /** The menu button under the mouse; {@link #MAP} for the map key, {@link #NONE} for none. A click on a knob's bar sets it to the nearest 5 %. */
    private int buttonAt(int px, int py) {
        if (in(px, py, RUN_X, BTN_Y, BTN_W, BTN_H)) return B_RUN;
        if (in(px, py, SCRAM_X, BTN_Y, BTN_W, BTN_H)) return B_SCRAM;
        if (in(px, py, SIDE_X, MAP_Y, SIDE_W, BTN_H)) return MAP;
        for (int[] k : new int[][]{{KNOB_POWER, B_POWER_DOWN, B_POWER_UP, B_POWER_SET}, {KNOB_SPLIT, B_SPLIT_DOWN, B_SPLIT_UP, B_SPLIT_SET}}) {
            int top = k[0] + 10;
            if (in(px, py, SIDE_X, top, Keys.KNOB, Keys.KNOB)) return k[1];
            if (in(px, py, KNOB_PLUS_X, top, Keys.KNOB, Keys.KNOB)) return k[2];
            if (in(px, py, KNOB_BAR_X, top, KNOB_BAR_W, 9)) {
                double f = Math.max(0, Math.min(1, (px - KNOB_BAR_X - 1) / (double) (KNOB_BAR_W - 2)));
                return k[3] + (int) Math.round(f * KNOB_STEPS);
            }
        }
        return -1;
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** A part in a cell: its own lines, then the flux and the heat it sees. */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && hoveredSlot.index < CELLS) {
            int i = hoveredSlot.index, r = i / Zone.SIZE, c = i % Zone.SIZE;
            lines.add(Component.translatable("gui.voltex_nuclear.reactor.cell_flux", decimal(flux.flux(r, c), 2)).withStyle(s -> s.withColor(C_ENERGY)));
            int load = menu.get(D_LOAD + i);
            if (load > 0) lines.add(Component.translatable("gui.voltex_nuclear.reactor.cell_heat", load).withStyle(s -> s.withColor(C_HEAT)));
            int ghost = menu.get(D_GHOST + i);
            if (otherThanGhost(stack, ghost))
                lines.add(Component.translatable("gui.voltex_nuclear.reactor.ghost_other", new ItemStack(ReactorParts.items().get(ghost - 1)).getHoverName())
                        .withStyle(s -> s.withColor(C_BAD)));
        }
        return lines;
    }

    /** Whether a part stands in a cell whose ghost (set by a scheme) is another part. */
    private static boolean otherThanGhost(ItemStack stack, int ghost) {
        return !stack.isEmpty() && ghost > 0 && ghost <= ReactorParts.items().size() && ReactorParts.items().get(ghost - 1) != stack.getItem();
    }

    private void tooltips(GuiGraphics g, int px, int py, int mx, int my) {
        if (in(px, py, EN_X - 6, EN_Y, EN_W + 6, 8)) {
            List<Component> l = new ArrayList<>();
            // his bar line first (Bars.energy), then what a generator's buffer adds: given out, and lost when nothing takes it
            l.add(Component.translatable("gui.industrial_core.bar.energy", VoltexBars.num(menu.getWide(D_ENERGY)), VoltexBars.num(BUFFER)).withStyle(ModStyle.ENERGY_STYLE));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.buffer_out", big(menu.getWide(D_GIVEN))));
            if (menu.getWide(D_LOST) > 0) l.add(Component.translatable("gui.voltex_nuclear.reactor.buffer_lost", big(menu.getWide(D_LOST))).withStyle(s -> s.withColor(C_BAD)));
            g.renderComponentTooltip(font, l, mx, my);
        } else if (in(px, py, SIDE_X, L_STATUS, SIDE_W, HULL_Y + HULL_H - L_STATUS)) {
            List<Component> l = new ArrayList<>();
            l.add(Component.translatable("gui.voltex_nuclear.reactor.power_now", Math.round(menu.get(D_POWER) / 10.0)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.heat_removed", big(menu.get(D_REMOVED))).withStyle(s -> s.withColor(C_HEAT)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.heat_hull", big(menu.get(D_TO_HULL))).withStyle(s -> s.withColor(menu.get(D_TO_HULL) > 0 ? C_BAD : C_LABEL)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.heat_pipes", big(menu.getWide(D_HEAT))).withStyle(s -> s.withColor(C_HEAT)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.water", decimal(menu.get(D_WATER) * 20 / 100.0, 1)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.coolant", decimal(menu.get(D_COOLANT) * 20 / 100.0, 1)));
            l.add(Component.translatable("gui.voltex_nuclear.reactor.hull_tip"));
            g.renderComponentTooltip(font, l, mx, my);
        } else if (in(px, py, SIDE_X, KNOB_POWER, SIDE_W, 20)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.voltex_nuclear.reactor.power_tip",
                    Math.round(flux.minPower() * 100), flux.controlRods())), mx, my);
        } else if (in(px, py, SIDE_X, KNOB_SPLIT, SIDE_W, 20)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.voltex_nuclear.reactor.split_tip", FE_PER_HEAT)), mx, my);
        } else {
            for (int i = 0; i < CELLS; i++) {
                int r = i / Zone.SIZE, c = i % Zone.SIZE, ghost = menu.get(D_GHOST + i);
                if (ghost <= 0 || !menu.getSlot(i).getItem().isEmpty() || !menu.cellOpen(r, c) || ghost > ReactorParts.items().size()) continue;
                if (!in(px, py, ReactorMenu.GRID_X + 1 + c * ReactorMenu.STEP, ReactorMenu.GRID_Y + 1 + r * ReactorMenu.STEP, 16, 16)) continue;
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.voltex_nuclear.reactor.ghost",
                        new ItemStack(ReactorParts.items().get(ghost - 1)).getHoverName())), mx, my);
                break;
            }
        }
    }

    private Status status() {
        int s = menu.get(D_STATUS);
        return s >= 0 && s < Status.values().length ? Status.values()[s] : Status.NOT_FORMED;
    }

    private WatcherState watcherState() {
        int s = menu.get(D_WATCHER);
        return s >= 0 && s < WatcherState.values().length ? WatcherState.values()[s] : WatcherState.NONE;
    }

    private static int alpha(double a) {
        return (int) Math.round(Math.max(0, Math.min(1, a)) * 255) << 24;
    }

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private static boolean in(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
