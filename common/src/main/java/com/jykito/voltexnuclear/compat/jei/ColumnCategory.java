package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.column.HeavyWaterColumn;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/**
 * The heavy water column in JEI, as his late cores: water and coolant in his tanks with the glass, the chevrons of his
 * late cores (a multiblock), the heavy water and the hot coolant out. One record, for 1 000 mB of heavy water: what goes
 * in and out for it does not depend on the tiers, only the time does. Under the tanks the energy a tick and the time of
 * a full column, and the heat it needs in his words for it; their tooltips say the rest.
 */
final class ColumnCategory extends NuclearCategory<ColumnCategory.Recipe> {

    /** Water and coolant into heavy water and hot coolant, mB, in this many ticks of a full column. */
    record Recipe(int water, int coolant, int heavy, int hot, int ticks) {}

    static final RecipeType<Recipe> TYPE = RecipeType.create(ModInfo.MOD_ID, "heavy_water_column", Recipe.class);

    // the tanks (the glass under the frame) and the chevron bar, as jei_gui.js draws them; the lines 6 under the tanks
    private static final int H = 86, TEX_H = 127, TANK_Y = 12, TANK_W = 16, TANK_H = 36, GLASS_U = 236, GLASS_V = H + 4;
    private static final int WATER_X = 20, COOL_X = 42, HEAVY_X = 96, HOT_X = 118, PROG_X = 63, PROG_Y = 26, PROG_W = 28;
    private static final int LINE_Y = TANK_Y + TANK_H + 1 + 6, LINE = 11, C_TIME = 0xFFB0B0B0, C_HEAT = 0xFFFFA347;
    /** The record's heavy water, mB. */
    private static final int HEAVY = 1_000;

    private final IDrawable glass;

    ColumnCategory(IGuiHelper gui) {
        super(gui, "column", H, TEX_H, NuclearBlocks.COLUMN_CORE.get());
        glass = glass(GLASS_U, GLASS_V, TANK_W, TANK_H);
    }

    static List<Recipe> all() {
        double full = HeavyWaterColumn.fullSpeed(HeavyWaterColumn.MAX_TIERS);
        int ticks = (int) Math.round(HEAVY / HeavyWaterColumn.heavyWater(full));
        return List.of(new Recipe((int) Math.round(HeavyWaterColumn.water(full) * ticks),
                (int) Math.round(HeavyWaterColumn.coolant(HeavyWaterColumn.MAX_TIERS, full) * ticks), HEAVY,
                (int) Math.round(HeavyWaterColumn.coolant(HeavyWaterColumn.MAX_TIERS, full) * ticks), ticks));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        tank(builder, RecipeIngredientRole.INPUT, WATER_X, Fluids.WATER, recipe.water());
        tank(builder, RecipeIngredientRole.INPUT, COOL_X, VoltexNuclear.voltexFluid("coolant"), recipe.coolant());
        tank(builder, RecipeIngredientRole.OUTPUT, HEAVY_X, NuclearFluids.HEAVY_WATER.get(), recipe.heavy());
        tank(builder, RecipeIngredientRole.OUTPUT, HOT_X, VoltexNuclear.voltexFluid("hot_coolant"), recipe.hot());
    }

    /** A full tank, as his tanks are in his categories, the glass over it. */
    private void tank(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, Fluid fluid, int amount) {
        builder.addSlot(role, x, TANK_Y).setFluidRenderer(amount, false, TANK_W, TANK_H).setOverlay(glass, 0, 0).addFluidStack(fluid, amount);
    }

    @Override
    void drawProgress(Recipe recipe, GuiGraphics g) {
        int w = (int) ((PROG_W - 2) * loop(LATE_LOOP_TICKS));
        if (w > 0) VoltexBars.progressNow(g, PROG_X + 1, PROG_Y + 1, w);
    }

    @Override
    void drawInfo(Recipe recipe, GuiGraphics g, Font font) {
        String fe = fe();
        g.drawString(font, fe, WATER_X, LINE_Y, JeiGui.ENERGY, false);
        g.drawString(font, JeiGui.secs(recipe.ticks()), WATER_X + font.width(fe) + 8, LINE_Y, C_TIME, false);
        g.drawString(font, heat(), WATER_X, LINE_Y + LINE, C_HEAT, false);
    }

    @Override
    List<Component> tooltip(Recipe recipe, double mx, double my) {
        Font font = Minecraft.getInstance().font;
        if (in(mx, my, WATER_X, LINE_Y - 1, font.width(fe()) + 8 + font.width(JeiGui.secs(recipe.ticks())), LINE - 1))
            return List.of(Component.translatable("gui.voltex_nuclear.jei.column.tiers", HeavyWaterColumn.MAX_TIERS, HeavyWaterColumn.MIN_TIERS));
        if (in(mx, my, WATER_X, LINE_Y + LINE - 1, font.width(heat()), LINE - 1))
            return List.of(Component.translatable("gui.voltex_nuclear.jei.column.heat"));
        return List.of();
    }

    private static String fe() {
        return JeiGui.fe(HeavyWaterColumn.energyPerTick(HeavyWaterColumn.MAX_TIERS)) + " FE/t";
    }

    /** The heat as his late core categories say it. */
    private static Component heat() {
        return Component.translatable("gui.industrial_core.late_core.heat", VoltexBars.num(HeavyWaterColumn.HEAT_THRESHOLD));
    }
}
