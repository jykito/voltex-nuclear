package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.screen.Bars;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.core.electrolyzer.Electrolyzer;
import com.jykito.voltexnuclear.electrolyzer.ElectrolyzerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.D_ENERGY;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.D_PROGRESS;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.D_TANK;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.D_WORK;

/**
 * The electrolyzer window (mock-up :8781/nuclear_deuterium_gui.html, the author's picks): the layout of the press window
 * with no title, the frame in heavy water. On the left the heavy water tank, his tank from the crystallizer, the fluid,
 * the glass over it and its tooltip as in his windows (VoltexBars.fluid, VoltexBars.overlay, VoltexBars.tank); the
 * electrolyzer's own sign, uncovered in the blue of deuterium from the left as it works: from the tank into the bath, two
 * electrodes, the gas over the wall down to the empty cell's line and on to the deuterium cell; the upgrade column; the
 * energy in his animated fill with his bolt at its left and his tooltip on it (Bars.energy).
 */
public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/electrolyzer.png");
    /** The window is 191 high; the lit sign and the glass lie under it in the texture. */
    private static final int TEX_W = 256, TEX_H = 244, WINDOW_H = 191;
    // layout, in window pixels, as tools/gui_gen/nuclear_textures/electrolyzer_gui.js draws the texture
    private static final int TANK_X = 61, TANK_Y = 23, TANK_W = 16, TANK_H = 48, GLASS_U = 8, GLASS_V = 195;
    private static final int SIGN_X = 80, SIGN_Y = 25, SIGN_W = 68, SIGN_H = 36, SIGN_SRC_V = 195;
    /** The JEI click area: the sign right of the empty cell's slot, which stands in its bottom left. */
    public static final int CLICK_X = SIGN_X + 20, CLICK_Y = SIGN_Y, CLICK_W = SIGN_W - 20, CLICK_H = SIGN_H;
    private static final int EN_X = 61, EN_Y = 76, EN_W = 112;
    private static final Component HEAVY_WATER = Component.translatable("fluid_type.voltex_nuclear.heavy_water_fluid");

    private final VoltexBars bars = new VoltexBars();

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = TEX_W;
        imageHeight = WINDOW_H;
        inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
        Bars.energy(g, font, mx, my, leftPos + EN_X, topPos + EN_Y, EN_W, 8, menu.getWide(D_ENERGY), Electrolyzer.BUFFER);
        VoltexBars.tank(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H, HEAVY_WATER, menu.get(D_TANK), Electrolyzer.TANK);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        bars.step();
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        VoltexBars.fluid(g, x + TANK_X, y + TANK_Y, TANK_W, TANK_H, menu.get(D_TANK), Electrolyzer.TANK, NuclearFluids.HEAVY_WATER_TINT);
        VoltexBars.overlay(g, TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
        int work = menu.get(D_WORK);
        int pw = work > 0 ? Math.min(SIGN_W, SIGN_W * menu.get(D_PROGRESS) / work) : 0;
        if (pw > 0) VoltexBars.overlay(g, TEXTURE, x + SIGN_X, y + SIGN_Y, SIGN_X, SIGN_SRC_V, pw, SIGN_H, TEX_W, TEX_H);
        int ew = (int) ((long) (EN_W - 2) * menu.getWide(D_ENERGY) / Electrolyzer.BUFFER);
        if (ew > 0) bars.energy(g, x + EN_X + 1, y + EN_Y + 1, ew);
    }

    /** No title, as the author asked; the names stay in the tooltips. */
    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}
}
