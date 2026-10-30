package com.jykito.voltexnuclear.compat.jei;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.VoltexBars;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * What the add-on's JEI categories share: a small copy of the machine window as background (tools/gui_gen/nuclear_textures/
 * jei_gui.js draws it at the top of its texture), the title and the icon of the machine. Each category adds its slots,
 * its progress and its numbers, set out the way the Voltex category of its kind does it: a one-block machine in two
 * small lines under the progress, a multiblock in full lines under its slots. The background is drawn here rather than
 * given to JEI, the way both JEI versions the add-on builds against draw it.
 */
abstract class NuclearCategory<T> implements IRecipeCategory<T> {

    static final int W = 154;
    /** The chevrons of a multiblock run a short loop, as on his late core categories: its cycles are long. */
    static final int LATE_LOOP_TICKS = 60;

    final ResourceLocation texture;
    final int textureHeight;
    private final int height;
    private final IDrawableStatic background;
    private final IDrawable icon;
    private final Component title;

    NuclearCategory(IGuiHelper gui, String name, int height, int textureHeight, ItemLike machine) {
        texture = VoltexNuclear.id("textures/gui/jei/jei_" + name + ".png");
        this.height = height;
        this.textureHeight = textureHeight;
        background = gui.drawableBuilder(texture, 0, 0, W, height).setTextureSize(256, textureHeight).build();
        icon = gui.createDrawableItemStack(new ItemStack(machine));
        title = Component.translatable(machine.asItem().getDescriptionId());
    }

    /** The progress of the machine, drawn over the background. */
    abstract void drawProgress(T recipe, GuiGraphics g);

    /** The numbers of the recipe. */
    abstract void drawInfo(T recipe, GuiGraphics g, Font font);

    /** What the category says on hover at this point, or nothing. */
    abstract List<Component> tooltip(T recipe, double mx, double my);

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return W;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        background.draw(g);
        drawProgress(recipe, g);
        drawInfo(recipe, g, Minecraft.getInstance().font);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mx, double my) {
        tooltip.addAll(tooltip(recipe, mx, my));
    }

    /** His glass over a tank of w x h, from u, v of the background texture (under the frame), for a fluid slot's overlay. */
    IDrawable glass(int u, int v, int w, int h) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return w;
            }

            @Override
            public int getHeight() {
                return h;
            }

            @Override
            public void draw(GuiGraphics g, int x, int y) {
                VoltexBars.overlay(g, texture, x, y, u, v, w, h, 256, textureHeight);
            }
        };
    }

    /** A share of a loop of this many ticks, for the progress the category plays. */
    static float loop(int ticks) {
        long ms = ticks * 50L;
        return (Util.getMillis() % ms) / (float) ms;
    }

    static boolean in(double px, double py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }
}
