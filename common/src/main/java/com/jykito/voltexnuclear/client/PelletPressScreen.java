package com.jykito.voltexnuclear.client;

import com.jykito.industrialcore.screen.Bars;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.press.PelletPress;
import com.jykito.voltexnuclear.press.PelletPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.D_ENERGY;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.D_PROGRESS;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.D_WORK;

/**
 * The pellet press window (mock-up :8781/nuclear_press.html): the uranium and the empty rod on the left, the rod on the
 * right, the upgrade column; between them the press's own progress sign, as each one-block Voltex machine has one (the
 * chevrons are for the multiblocks): the lines from the inputs into the punch and the die, the pellet, the line on to
 * the rod, uncovered in green from the left as it works. The energy in his animated fill with his bolt at its left and
 * his tooltip on it (Bars.energy), as on his machine windows.
 * Empty slots stay empty, as on his machine windows. No title, as the author asked (as on the electrolyzer).
 */
public class PelletPressScreen extends AbstractContainerScreen<PelletPressMenu> {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/gui/pellet_press.png");
    /** The window is 191 high; the green sign lies under it in the texture. */
    private static final int TEX_W = 256, TEX_H = 223, WINDOW_H = 191;
    // layout, in window pixels, as tools/gui_gen/nuclear_textures/press_gui.js draws the texture; the sign is the JEI click area
    public static final int SIGN_X = 85, SIGN_Y = 31, SIGN_W = 58, SIGN_H = 27, SIGN_SRC_V = 195;
    private static final int EN_X = 61, EN_Y = 74, EN_W = 112;

    private final VoltexBars bars = new VoltexBars();

    public PelletPressScreen(PelletPressMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = TEX_W;
        imageHeight = WINDOW_H;
        inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
        Bars.energy(g, font, mx, my, leftPos + EN_X, topPos + EN_Y, EN_W, 8, menu.getWide(D_ENERGY), PelletPress.BUFFER);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        bars.step();
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        int work = menu.get(D_WORK);
        int pw = work > 0 ? Math.min(SIGN_W, SIGN_W * menu.get(D_PROGRESS) / work) : 0;
        if (pw > 0) VoltexBars.overlay(g, TEXTURE, x + SIGN_X, y + SIGN_Y, SIGN_X, SIGN_SRC_V, pw, SIGN_H, TEX_W, TEX_H);
        int ew = (int) ((long) (EN_W - 2) * menu.getWide(D_ENERGY) / PelletPress.BUFFER);
        if (ew > 0) bars.energy(g, x + EN_X + 1, y + EN_Y + 1, ew);
    }

    /** No title, as the author asked; the names stay in the tooltips. */
    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}
}
