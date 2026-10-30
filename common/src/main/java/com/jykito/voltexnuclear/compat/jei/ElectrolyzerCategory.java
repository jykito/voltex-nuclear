package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.ModStyle;
import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.electrolyzer.Electrolyzer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The electrolyzer in JEI: the heavy water in his tank with the glass over it, full as his tanks are in his categories,
 * the empty cell, the electrolyzer's own sign uncovered in the blue of deuterium from the left, the deuterium cell. The
 * numbers as on his one-block machines: the energy a tick and the time in two small lines, here under the output (the
 * cell's slot stands under the sign), the energy bar under the slots, its tooltip in his three lines.
 */
final class ElectrolyzerCategory extends NuclearCategory<ElectrolyzerCategory.Recipe> {

    /** Heavy water and an empty cell into a deuterium cell; one record. */
    record Recipe(int heavyWater, Item empty, Item cell) {}

    static final RecipeType<Recipe> TYPE = RecipeType.create(ModInfo.MOD_ID, "electrolyzer", Recipe.class);

    // the tank, the slots, the sign and the energy bar, as jei_gui.js draws them; the lit sign and the glass lie under the frame
    private static final int H = 84, TEX_H = 137, TANK_X = 24, TANK_Y = 11, TANK_W = 16, TANK_H = 48, GLASS_U = 236, GLASS_V = 88;
    private static final int CELL_X = 45, CELL_Y = 39, OUT_X = 113, OUT_Y = 25;
    private static final int SIGN_X = 43, SIGN_Y = 13, SIGN_W = 68, SIGN_H = 36, SIGN_SRC_V = H + 4;
    private static final int EN_X = 23, EN_Y = 65, EN_W = 108;
    private static final int INFO_X = OUT_X, INFO_Y = OUT_Y + 20, C_TIME = 0xB0B0B0;

    private final IDrawable glass;

    ElectrolyzerCategory(IGuiHelper gui) {
        super(gui, "electrolyzer", H, TEX_H, NuclearBlocks.ELECTROLYZER.get());
        glass = glass(GLASS_U, GLASS_V, TANK_W, TANK_H);
    }

    static List<Recipe> all() {
        return List.of(new Recipe(Electrolyzer.HEAVY_WATER_PER_CELL, NuclearItems.EMPTY_CELL.get(), NuclearItems.DEUTERIUM_CELL.get()));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y)
                .setFluidRenderer(recipe.heavyWater(), false, TANK_W, TANK_H)
                .setOverlay(glass, 0, 0)
                .addFluidStack(NuclearFluids.HEAVY_WATER.get(), recipe.heavyWater());
        builder.addSlot(RecipeIngredientRole.INPUT, CELL_X + 1, CELL_Y + 1).addItemStack(new ItemStack(recipe.empty()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1, OUT_Y + 1).addItemStack(new ItemStack(recipe.cell()));
    }

    /** The sign fills in the time of the recipe, as the electrolyzer takes it without upgrades. */
    @Override
    void drawProgress(Recipe recipe, GuiGraphics g) {
        int w = (int) (SIGN_W * loop(Electrolyzer.WORK_TICKS));
        if (w > 0) VoltexBars.overlay(g, texture, SIGN_X, SIGN_Y, SIGN_X, SIGN_SRC_V, w, SIGN_H, 256, textureHeight);
    }

    @Override
    void drawInfo(Recipe recipe, GuiGraphics g, Font font) {
        VoltexBars.energyNow(g, EN_X + 1, EN_Y, EN_W - 2);
        JeiGui.smallText(g, font, JeiGui.fe(Electrolyzer.FE_PER_TICK) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(Electrolyzer.WORK_TICKS), INFO_X, INFO_Y + JeiGui.SMALL_LINE, C_TIME);
    }

    @Override
    List<Component> tooltip(Recipe recipe, double mx, double my) {
        if (!in(mx, my, EN_X - 6, EN_Y - 1, EN_W + 6, 8)) return List.of();
        return List.of(
                Component.translatable("jei.industrial_core.energy").withStyle(ModStyle.ENERGY_STYLE),
                Component.literal(JeiGui.fe(Electrolyzer.FE_PER_TICK) + " FE/t").withStyle(ChatFormatting.GRAY),
                Component.translatable("jei.industrial_core.time", Electrolyzer.WORK_TICKS / 20).withStyle(ChatFormatting.DARK_GRAY));
    }
}
