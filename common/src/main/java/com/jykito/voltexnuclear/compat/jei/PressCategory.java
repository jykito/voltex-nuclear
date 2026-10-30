package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.ModStyle;
import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.press.PelletPress;
import com.jykito.voltexnuclear.press.PressRecipes.Recipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The pellet press in JEI, a record a fuel: the fuel and its shell (the empty Voltex rod, the empty cell for lithium), the
 * press's own progress sign (a one-block machine: the lines from both inputs into the punch and the die, uncovered in
 * green from the left, as in its window), the product. The numbers as on his one-block machines (his rod fabricator category): the energy a tick and the
 * time in two small lines under the progress, the energy bar under the slots, its tooltip in his three lines.
 */
final class PressCategory extends NuclearCategory<Recipe> {

    static final RecipeType<Recipe> TYPE = RecipeType.create(ModInfo.MOD_ID, "pellet_press", Recipe.class);

    // slots, the sign and the energy bar, as jei_gui.js draws them; the green sign lies under the window in the texture
    private static final int H = 84, IN_X = 28, FUEL_Y = 12, ROD_Y = 36, OUT_X = 108, OUT_Y = 24;
    private static final int SIGN_X = 48, SIGN_Y = 19, SIGN_W = 58, SIGN_H = 27, SIGN_SRC_V = H + 4, TEX_H = H + 4 + SIGN_H + 1;
    private static final int EN_X = 28, EN_Y = 65, EN_W = 98;
    private static final int INFO_X = SIGN_X + 2, INFO_Y = SIGN_Y + SIGN_H + 1, C_TIME = 0xB0B0B0;

    PressCategory(IGuiHelper gui) {
        super(gui, "pellet_press", H, TEX_H, NuclearBlocks.PELLET_PRESS.get());
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, IN_X + 1, FUEL_Y + 1).addItemStack(new ItemStack(recipe.fuel()));
        builder.addSlot(RecipeIngredientRole.INPUT, IN_X + 1, ROD_Y + 1).addItemStack(new ItemStack(recipe.shell()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1, OUT_Y + 1).addItemStack(new ItemStack(recipe.product()));
    }

    /** The sign fills in the time of the recipe, as the press takes it without upgrades. */
    @Override
    void drawProgress(Recipe recipe, GuiGraphics g) {
        int w = (int) (SIGN_W * loop(PelletPress.WORK_TICKS));
        if (w > 0) VoltexBars.overlay(g, texture, SIGN_X, SIGN_Y, SIGN_X, SIGN_SRC_V, w, SIGN_H, 256, textureHeight);
    }

    @Override
    void drawInfo(Recipe recipe, GuiGraphics g, Font font) {
        VoltexBars.energyNow(g, EN_X + 1, EN_Y, EN_W - 2);
        JeiGui.smallText(g, font, JeiGui.fe(PelletPress.FE_PER_TICK) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(PelletPress.WORK_TICKS), INFO_X, INFO_Y + JeiGui.SMALL_LINE, C_TIME);
    }

    @Override
    List<Component> tooltip(Recipe recipe, double mx, double my) {
        if (!in(mx, my, EN_X - 6, EN_Y - 1, EN_W + 6, 8)) return List.of();
        return List.of(
                Component.translatable("jei.industrial_core.energy").withStyle(ModStyle.ENERGY_STYLE),
                Component.literal(JeiGui.fe(PelletPress.FE_PER_TICK) + " FE/t").withStyle(ChatFormatting.GRAY),
                Component.translatable("jei.industrial_core.time", PelletPress.WORK_TICKS / 20).withStyle(ChatFormatting.DARK_GRAY));
    }
}
