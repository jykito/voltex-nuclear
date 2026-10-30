package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.starforge.StarForge;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * The star forge in JEI (stage 12), its one melt: two Voltex kronium alloys, a palladium and a helium cell, the chevrons
 * of the Voltex late cores (a multiblock), the star alloy and the empty cell that comes back. The forge takes no energy,
 * only the heat of its port, so the lines under the slots are the heat it melts at and the time it takes over it; the
 * guide tells the rest (JEI the minimum, the author). Laid out by jei_gui.js, in the frame of its window (the star alloy's gold).
 */
final class StarForgeCategory extends NuclearCategory<StarForgeCategory.Melt> {

    /** The forge has one melt; JEI wants an object for it. */
    record Melt() {}

    static final RecipeType<Melt> TYPE = RecipeType.create(ModInfo.MOD_ID, "star_forge", Melt.class);

    // the slots, the chevron bar and the lines, as jei_gui.js draws them
    private static final int H = 74, SLOT_Y = 15, KRONIUM_X = 12, PALLADIUM_X = 30, HELIUM_X = 48, ALLOY_X = 104, CELL_X = 122;
    private static final int PROG_X = 70, PROG_Y = 20, PROG_W = 28, LINE_Y = 39, LINE = 11, C_NEED = 0xFFFFA347, C_TIME = 0xFFB0B0B0;

    StarForgeCategory(IGuiHelper gui) {
        super(gui, "star_forge", H, H, NuclearBlocks.STAR_FORGE_CORE.get());
    }

    @Override
    public RecipeType<Melt> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Melt melt, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, KRONIUM_X + 1, SLOT_Y + 1)
                .addItemStack(new ItemStack(VoltexNuclear.voltexItem("kronium_alloy"), StarForge.KRONIUM));
        builder.addSlot(RecipeIngredientRole.INPUT, PALLADIUM_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(NuclearItems.PALLADIUM.get(), StarForge.PALLADIUM));
        builder.addSlot(RecipeIngredientRole.INPUT, HELIUM_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(NuclearItems.HELIUM_CELL.get(), StarForge.HELIUM));
        builder.addSlot(RecipeIngredientRole.OUTPUT, ALLOY_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(NuclearItems.STAR_ALLOY.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, CELL_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(NuclearItems.EMPTY_CELL.get(), StarForge.HELIUM));
    }

    @Override
    void drawProgress(Melt melt, GuiGraphics g) {
        int w = (int) ((PROG_W - 2) * loop(LATE_LOOP_TICKS));
        if (w > 0) VoltexBars.progressNow(g, PROG_X + 1, PROG_Y + 1, w);
    }

    @Override
    void drawInfo(Melt melt, GuiGraphics g, Font font) {
        g.drawString(font, Component.translatable("gui.voltex_nuclear.jei.star_forge.heat", heat(StarForge.STAR_HEAT), heat(StarForge.TOP_HEAT)),
                KRONIUM_X, LINE_Y, C_NEED, false);
        g.drawString(font, JeiGui.secs(StarForge.SLOW_TICKS) + "–" + JeiGui.secs(StarForge.FAST_TICKS), KRONIUM_X, LINE_Y + LINE, C_TIME, false);
    }

    @Override
    List<Component> tooltip(Melt melt, double mx, double my) {
        return List.of();
    }

    /** A heat level as the windows write it, the thousands apart. */
    static String heat(int level) {
        return String.format(Locale.ROOT, "%,d", level).replace(',', ' ');
    }
}
