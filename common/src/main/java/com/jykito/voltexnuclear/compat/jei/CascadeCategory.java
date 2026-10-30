package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.cascade.Cascade.Run;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The centrifuge cascade in JEI, a record a run (the low stage, the high one, the low one on tails): the yellowcake of one
 * uranium, the chevrons of the Voltex late cores (a multiblock), the enriched and the depleted uranium. The numbers as on
 * his late core categories, in full lines from the left edge of the slots: the energy in his energy colour, the condition
 * under it in his colour for conditions. They say only the energy of one uranium, which does not depend on the length of
 * the row, and how many centrifuges open the stage (the author: "just how many centrifuges and the total energy"); full
 * speed is in the tooltip. The tails show the depleted uranium in the yellowcake's slot and nothing more: a third line
 * ran into the frame, and when they run is the guide's to say.
 */
final class CascadeCategory extends NuclearCategory<Run> {

    static final RecipeType<Run> TYPE = RecipeType.create(ModInfo.MOD_ID, "cascade", Run.class);

    // slots and the chevron bar, as jei_gui.js draws them; the lines as LateJeiCategory sets them under the slots
    private static final int H = 74, KEK_X = 21, PRODUCT_X = 93, DEPLETED_X = 115, SLOT_Y = 15, PROG_X = 43, PROG_Y = 20, PROG_W = 44;
    private static final int LINE_Y = SLOT_Y + 18 + 6, LINE = 11, C_NEED = 0xFFFFA347;

    CascadeCategory(IGuiHelper gui) {
        super(gui, "cascade", H, H, NuclearBlocks.CASCADE_CORE.get());
    }

    @Override
    public RecipeType<Run> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Run run, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, KEK_X + 1, SLOT_Y + 1)
                .addItemStack(new ItemStack((run.tails() ? NuclearItems.DEPLETED_URANIUM : NuclearItems.YELLOWCAKE).get(), run.feed()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, PRODUCT_X + 1, SLOT_Y + 1)
                .addItemStack(new ItemStack(run == Run.HIGH ? NuclearItems.HIGH_ENRICHED_URANIUM.get() : NuclearItems.LOW_ENRICHED_URANIUM.get()));
        if (run.depleted() > 0)
            builder.addSlot(RecipeIngredientRole.OUTPUT, DEPLETED_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(NuclearItems.DEPLETED_URANIUM.get(), run.depleted()));
    }

    @Override
    void drawProgress(Run run, GuiGraphics g) {
        int w = (int) ((PROG_W - 2) * loop(LATE_LOOP_TICKS));
        if (w > 0) VoltexBars.progressNow(g, PROG_X + 1, PROG_Y + 1, w);
    }

    @Override
    void drawInfo(Run run, GuiGraphics g, Font font) {
        g.drawString(font, energy(run), KEK_X, LINE_Y, JeiGui.ENERGY, false);
        g.drawString(font, columns(run), KEK_X, LINE_Y + LINE, C_NEED, false);
    }

    @Override
    List<Component> tooltip(Run run, double mx, double my) {
        Font font = Minecraft.getInstance().font;
        if (in(mx, my, KEK_X, LINE_Y - 1, font.width(energy(run)), LINE - 1))
            return List.of(Component.translatable("gui.voltex_nuclear.jei.cascade.energy", run.energy()));
        if (in(mx, my, KEK_X, LINE_Y + LINE - 1, font.width(columns(run)), LINE - 1))
            return List.of(Component.translatable("gui.voltex_nuclear.jei.cascade.speed", run.stage().need));
        return List.of();
    }

    private static String energy(Run run) {
        return JeiGui.fe(run.energy()) + " FE";
    }

    private static Component columns(Run run) {
        return Component.translatable("gui.voltex_nuclear.jei.cascade.columns", run.stage().minColumns());
    }
}
