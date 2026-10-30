package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.fusion.Fusion;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The fusion torus in JEI (stage 12), as it burns a pair of fuel cells: the superfluid condensate the coils take while the
 * pair burns in his tank with the glass, the deuterium and the tritium cell, the chevrons of the Voltex late cores (a
 * multiblock), the helium cell the pair gives and the empty one left over. Two lines under them: what the torus gives
 * at its best mix and the window of tritium it burns in; the guide tells the rest (JEI the minimum, the author). Laid out
 * by jei_gui.js, in the frame of its window (the plasma pink).
 */
final class TorusCategory extends NuclearCategory<TorusCategory.Burn> {

    /** The torus burns one way; JEI wants an object for it. */
    record Burn() {}

    static final RecipeType<Burn> TYPE = RecipeType.create(ModInfo.MOD_ID, "torus", Burn.class);

    // his tank (the glass under the frame), the slots, the chevron bar and the lines, as jei_gui.js draws them
    private static final int H = 84, TANK_X = 19, TANK_Y = 12, TANK_W = 16, TANK_H = 36, TEX_H = H + 4 + TANK_H + 1, GLASS_U = 256 - TANK_W - 4, GLASS_V = H + 4;
    private static final int CELLS_X = 39, DEUTERIUM_Y = 13, TRITIUM_Y = 31, OUT_Y = 22, HELIUM_X = 97, EMPTY_X = 115;
    private static final int PROG_X = 63, PROG_Y = 26, PROG_W = 28, LINE_Y = 54, LINE = 10, C_NEED = 0xFFFFA347;
    /** Ticks a pair of cells burns at the best mix, and the condensate the coils take meanwhile. */
    private static final int PAIR_TICKS = (int) (2 * Fusion.CELL / Fusion.BURN), PAIR_CONDENSATE = (int) Math.round(PAIR_TICKS * Fusion.CONDENSATE);

    private final IDrawable glass;

    TorusCategory(IGuiHelper gui) {
        super(gui, "torus", H, TEX_H, NuclearBlocks.TORUS_CORE.get());
        glass = glass(GLASS_U, GLASS_V, TANK_W, TANK_H);
    }

    @Override
    public RecipeType<Burn> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Burn burn, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y)
                .setFluidRenderer(PAIR_CONDENSATE, false, TANK_W, TANK_H)
                .setOverlay(glass, 0, 0)
                .addFluidStack(VoltexNuclear.voltexFluid("superfluid_condensate"), PAIR_CONDENSATE);
        builder.addSlot(RecipeIngredientRole.INPUT, CELLS_X + 1, DEUTERIUM_Y + 1).addItemStack(new ItemStack(NuclearItems.DEUTERIUM_CELL.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, CELLS_X + 1, TRITIUM_Y + 1).addItemStack(new ItemStack(NuclearItems.TRITIUM_CELL.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, HELIUM_X + 1, OUT_Y + 1).addItemStack(new ItemStack(NuclearItems.HELIUM_CELL.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, EMPTY_X + 1, OUT_Y + 1).addItemStack(new ItemStack(NuclearItems.EMPTY_CELL.get()));
    }

    @Override
    void drawProgress(Burn burn, GuiGraphics g) {
        int w = (int) ((PROG_W - 2) * loop(LATE_LOOP_TICKS));
        if (w > 0) VoltexBars.progressNow(g, PROG_X + 1, PROG_Y + 1, w);
    }

    @Override
    void drawInfo(Burn burn, GuiGraphics g, Font font) {
        g.drawString(font, Component.translatable("gui.voltex_nuclear.jei.torus.output", JeiGui.fe((long) Fusion.FE_BEST)), TANK_X, LINE_Y, JeiGui.ENERGY, false);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.jei.torus.window", percent(Fusion.low(0)), percent(Fusion.high(0))),
                TANK_X, LINE_Y + LINE, C_NEED, false);
    }

    @Override
    List<Component> tooltip(Burn burn, double mx, double my) {
        return List.of();
    }

    private static String percent(double share) {
        return Long.toString(Math.round(share * 100));
    }

}
