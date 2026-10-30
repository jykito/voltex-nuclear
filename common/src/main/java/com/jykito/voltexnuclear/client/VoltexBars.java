package com.jykito.voltexnuclear.client;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Locale;

/**
 * What every add-on window takes from the Voltex windows: the chevrons of his late cores for progress and his animated
 * energy fill (96 x 6 a frame, 6 frames down the texture), stepped as his FillBar steps them, the energy a frame every
 * 32 renders and the chevrons every 6; and the short numbers the windows print. A screen keeps one and steps it once
 * a render.
 */
public final class VoltexBars {

    public static final ResourceLocation ENERGY = VoltexNuclear.platform().id("industrial_core", "textures/gui/energy_fill_6x_anim.png");
    public static final ResourceLocation PROGRESS = VoltexNuclear.platform().id("industrial_core", "textures/gui/progress_fill_6x_anim.png");
    private static final int FILL_W = 96, FILL_H = 36, FRAMES = 6;
    /** The texture of the fluids in the tanks: water, tinted, as his are. */
    private static final ResourceLocation WATER_STILL = VoltexNuclear.platform().id("minecraft", "block/water_still");
    /** Water in a tank, as his FluidGauge tints it: a stack of water has no biome colour of its own. */
    public static final int WATER_TINT = 0xFF3F76E4;
    /** His coolant: water in its own tint. */
    public static final int COOLANT_TINT = 0xFF00D5FF;

    private int energyTick, energyFrame, chevronTick, chevronFrame;

    /** One render. */
    public void step() {
        if (++energyTick >= 32) { energyTick = 0; energyFrame = (energyFrame + 1) % FRAMES; }
        if (++chevronTick >= 6) { chevronTick = 0; chevronFrame = (chevronFrame + 1) % FRAMES; }
    }

    /** The chevrons, {@code w} pixels of them from the left. */
    public void progress(GuiGraphics g, int x, int y, int w) {
        fill(g, PROGRESS, x, y, w, chevronFrame);
    }

    /** The energy fill, {@code w} pixels of it from the left. */
    public void energy(GuiGraphics g, int x, int y, int w) {
        fill(g, ENERGY, x, y, w, energyFrame);
    }

    /** Another fill in his FillBar format (96 x 36, 6 frames of 6 px), {@code w} pixels of it, at the energy's pace. */
    public void bar(GuiGraphics g, ResourceLocation tex, int x, int y, int w) {
        fill(g, tex, x, y, w, energyFrame);
    }
    /**
     * The chevrons by the clock, for views that draw many records in one render (JEI): a frame every 100 ms, about what
     * 6 renders are at 60 frames a second.
     */
    public static void progressNow(GuiGraphics g, int x, int y, int w) {
        fill(g, PROGRESS, x, y, w, (int) (Util.getMillis() / 100 % FRAMES));
    }

    /** The energy fill by the clock: a frame every 530 ms, about 32 renders. */
    public static void energyNow(GuiGraphics g, int x, int y, int w) {
        fill(g, ENERGY, x, y, w, (int) (Util.getMillis() / 530 % FRAMES));
    }

    /**
     * A fluid in a tank of {@code w} x {@code h} at x, y, as his FluidGauge draws it (the author: "exactly as in the
     * Voltex windows"): the still water texture in the fluid's tint, tiles of 16 from the bottom up to the share it holds.
     * The add-on's fluids are water in a tint, as his are, so the texture is known here and no loader's fluid classes
     * are needed. The screen lays the tank glass over it.
     */
    public static void fluid(GuiGraphics g, int x, int y, int w, int h, int amount, int capacity, int tint) {
        if (amount <= 0 || capacity <= 0) return;
        int fluidH = Math.min(h, (int) ((float) amount / capacity * h));
        if (fluidH <= 0) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_STILL);
        float a = ((tint >> 24) & 0xFF) / 255f;
        RenderSystem.enableBlend();
        g.setColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, a == 0f ? 1f : a);
        for (int i = 0; i < w; i += 16)
            for (int j = 0; j < fluidH; j += 16) {
                int dw = Math.min(w - i, 16), dh = Math.min(fluidH - j, 16);
                g.blit(x + i, y + h - j - dh, 0, dw, dh, sprite);
            }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /** A tank's tooltip as his FluidGauge gives it: "Water: 4000 / 8000 mB", or "Empty"; {@code fluid} is its name. */
    public static void tank(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, int h, Component fluid, int amount, int capacity) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return;
        g.renderTooltip(font, amount <= 0 ? Component.translatable("gui.industrial_core.empty")
                : Component.literal("").append(fluid).append(": " + amount + " / " + capacity + " mB"), mx, my);
    }

    /** A number as his bar tooltips print it (Bars.energy): thousands split by spaces, 64 000. */
    public static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }

    /**
     * A part of a window texture laid over what is drawn, its see-through pixels see-through: the lit sign over the dark
     * one, the glass over the fluid. The plain blit does not blend on 1.20.1, and the faint glass came out solid black.
     */
    public static void overlay(GuiGraphics g, ResourceLocation tex, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        RenderSystem.enableBlend();
        g.blit(tex, x, y, u, v, w, h, texW, texH);
        RenderSystem.disableBlend();
    }

    private static void fill(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int frame) {
        for (int dx = 0; dx < w; dx += FILL_W) g.blit(tex, x + dx, y, 0, frame * 6, Math.min(FILL_W, w - dx), 6, FILL_W, FILL_H);
    }

    /** 0,5 and 33,3 in Russian, 0.5 and 33.3 in English: the decimal mark from the language file. */
    public static String decimal(double v, int places) {
        String s = String.format(Locale.ROOT, "%." + places + "f", v);
        if (places > 0) s = s.replaceAll("\\.?0+$", "");   // only a fraction loses its zeros: 200 stays 200
        return s.replace(".", Component.translatable("gui.voltex_nuclear.decimal_mark").getString());
    }

    /** 9999, 12,3k, 200k, 1,5M, 260M, 1,25B. */
    public static String big(int n) {
        return n < 10_000 ? String.valueOf(n) : n < 1_000_000 ? decimal(n / 1000.0, n < 100_000 ? 1 : 0) + "k"
                : n < 1_000_000_000 ? decimal(n / 1e6, n < 100_000_000 ? 1 : 0) + "M" : decimal(n / 1e9, 2) + "B";
    }
}
