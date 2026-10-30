package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.radiochem.Radiochem;
import com.jykito.voltexnuclear.core.radiochem.Radiochem.Product;
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
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

import static com.jykito.voltexnuclear.radiochem.RadiochemCoreBlockEntity.*;

/**
 * The radiochemical plant in JEI, a record a spent part (a rod, the assemblies x2 and x4): its water in his tank with the glass,
 * the chevrons of the Voltex late cores (a multiblock), the empty rods and the four products in the grid of the plant's
 * window. The products come in even shares, not by chance: a slot shows what the part gives whole, at least one, and a
 * grey line where his late cores say the chance tells how many rods one of it takes. The numbers as on his late core
 * categories: the energy a tick and the time in one line under the slots.
 */
final class RadiochemCategory extends NuclearCategory<RadiochemCategory.Part> {

    /** A spent part of this many rods. */
    record Part(int rods, Item spent) {}

    static final RecipeType<Part> TYPE = RecipeType.create(ModInfo.MOD_ID, "radiochem", Part.class);

    // his tank (the glass under the frame), the slots and the chevron bar, as jei_gui.js draws them; the line as
    // LateJeiCategory sets it, 6 under the slots
    private static final int H = 76, TEX_H = 115, TANK_X = 13, TANK_Y = 14, TANK_W = 16, TANK_H = 34, GLASS_U = 236, GLASS_V = H + 4;
    private static final int SPENT_X = 34, SPENT_Y = 22, PROG_X = 56, PROG_Y = 27, PROG_W = 28;
    /** The outputs: the empty rods, then the products in the order of {@link Product}, as in the window. */
    private static final int[][] OUT = {{88, 13}, {124, 13}, {88, 31}, {106, 13}, {106, 31}};
    private static final int LINE_Y = 13 + 2 * 18 + 6, C_TIME = 0xFFB0B0B0;

    private final IDrawable glass;

    RadiochemCategory(IGuiHelper gui) {
        super(gui, "radiochem", H, TEX_H, NuclearBlocks.RADIOCHEM_CORE.get());
        glass = glass(GLASS_U, GLASS_V, TANK_W, TANK_H);
    }

    static List<Part> all() {
        List<Part> parts = new ArrayList<>();
        for (int size = 0; size < RODS.length; size++) parts.add(new Part(RODS[size], spent(size)));
        return parts;
    }

    @Override
    public RecipeType<Part> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Part part, IFocusGroup focuses) {
        int water = Radiochem.water(part.rods());
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y)
                .setFluidRenderer(water, false, TANK_W, TANK_H)
                .setOverlay(glass, 0, 0)
                .addFluidStack(Fluids.WATER, water);
        builder.addSlot(RecipeIngredientRole.INPUT, SPENT_X + 1, SPENT_Y + 1).addItemStack(new ItemStack(part.spent()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT[0][0] + 1, OUT[0][1] + 1).addItemStack(new ItemStack(emptyRod(), part.rods()));
        for (Product p : Product.values()) {
            int[] at = OUT[p.ordinal() + 1];
            builder.addSlot(RecipeIngredientRole.OUTPUT, at[0] + 1, at[1] + 1)
                    .addItemStack(new ItemStack(product(p), Math.max(1, part.rods() / p.per)))
                    .addRichTooltipCallback((view, tip) -> tip.add(
                            Component.translatable("gui.voltex_nuclear.jei.radiochem.share", p.per).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    void drawProgress(Part part, GuiGraphics g) {
        int w = (int) ((PROG_W - 2) * loop(LATE_LOOP_TICKS));
        if (w > 0) VoltexBars.progressNow(g, PROG_X + 1, PROG_Y + 1, w);
    }

    @Override
    void drawInfo(Part part, GuiGraphics g, Font font) {
        String fe = JeiGui.fe(Radiochem.FE_PER_TICK) + " FE/t";
        g.drawString(font, fe, TANK_X, LINE_Y, JeiGui.ENERGY, false);
        g.drawString(font, JeiGui.secs(Radiochem.ticks(part.rods())), TANK_X + font.width(fe) + 8, LINE_Y, C_TIME, false);
    }

    @Override
    List<Component> tooltip(Part part, double mx, double my) {
        return List.of();
    }
}
