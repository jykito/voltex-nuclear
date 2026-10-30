package com.jykito.voltexnuclear.compat.jei;

import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.core.reactor.Reactor;
import com.jykito.voltexnuclear.core.reactor.Scheme;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.reactor.ReactorParts;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The reactor schemes in JEI (DESIGN step 24): a ready layout of the zone on the 7 x 7 of the reactor window, the cells
 * the scheme does not open dark, and under it what it gives at its knob, worked out by the reactor model itself. Every
 * cell is an input slot in the order of the window's cells, so the "+" of an open reactor window lays the scheme in
 * ({@link ReactorTransferHandler}); a part is a recipe ingredient, so its uses show the schemes it stands in.
 */
final class SchemeCategory extends NuclearCategory<Scheme> {

    static final RecipeType<Scheme> TYPE = RecipeType.create(ModInfo.MOD_ID, "reactor_scheme", Scheme.class);

    // the grid and the lines, as jei_gui.js draws the background
    private static final int H = 178, GRID_X = 14, GRID_Y = 10, STEP = 18, LINE_X = 14, LINE_Y = 140, LINE = 10;
    private static final int C_NAME = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC, C_LABEL = 0xFF9AA0A6, C_BAD = 0xFFFF5A4A;

    /** What a scheme gives, worked out once. */
    private final Map<Scheme, Reactor.Tick> ticks = new HashMap<>();

    SchemeCategory(IGuiHelper gui) {
        super(gui, "reactor", H, H, NuclearItems.REACTOR_CORE.get());
    }

    @Override
    public RecipeType<Scheme> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.voltex_nuclear.reactor_schemes");
    }

    private Reactor.Tick tick(Scheme scheme) {
        return ticks.computeIfAbsent(scheme, Scheme::run);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Scheme scheme, IFocusGroup focuses) {
        for (int i = 0; i < Zone.SIZE * Zone.SIZE; i++) {
            int r = i / Zone.SIZE, c = i % Zone.SIZE;
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, GRID_X + 1 + c * STEP, GRID_Y + 1 + r * STEP);
            Part part = scheme.part(r, c);
            if (part != null) slot.addItemStack(new ItemStack(ReactorParts.item(part)));
        }
    }

    /** The cells the scheme's chambers leave shut, dark as in the reactor window. */
    @Override
    void drawProgress(Scheme scheme, GuiGraphics g) {
        Zone zone = scheme.zone();
        for (int i = 0; i < Zone.SIZE * Zone.SIZE; i++) {
            int r = i / Zone.SIZE, c = i % Zone.SIZE;
            if (zone.open(r, c)) continue;
            int x = GRID_X + c * STEP, y = GRID_Y + r * STEP;
            g.fill(x, y, x + STEP, y + STEP, 0xC70D0D0D);
            g.fill(x + 7, y + 8, x + 11, y + 10, 0xFF2A2A2A);
        }
    }

    @Override
    void drawInfo(Scheme scheme, GuiGraphics g, Font font) {
        Reactor.Tick t = tick(scheme);
        g.drawString(font, Component.translatable("jei.voltex_nuclear.scheme." + scheme.key()), LINE_X, LINE_Y, C_NAME, false);
        g.drawString(font, Component.translatable("jei.voltex_nuclear.scheme.output", VoltexBars.big((int) Math.round(t.energy())),
                VoltexBars.big((int) Math.round(t.heat()))), LINE_X, LINE_Y + LINE, C_ENERGY, false);
        g.drawString(font, Component.translatable("gui.voltex_nuclear.reactor.power", Math.round(scheme.knob() * 100)), LINE_X, LINE_Y + 2 * LINE,
                t.toHull() > 0 ? C_BAD : C_LABEL, false);
    }

    /** Over the lines: the rods it burns and the blanks it turns an hour, where the heat goes and what the cells drink. */
    @Override
    List<Component> tooltip(Scheme scheme, double mx, double my) {
        if (!in(mx, my, LINE_X, LINE_Y, W - 2 * LINE_X, 3 * LINE)) return List.of();
        Reactor.Tick t = tick(scheme);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.voltex_nuclear.scheme.rods", VoltexBars.decimal(scheme.rodsPerHour(), 1)));
        double blanks = scheme.blanksPerHour();
        if (blanks > 0) lines.add(Component.translatable("jei.voltex_nuclear.scheme.blanks", Math.round(blanks)));
        lines.add(Component.translatable("gui.voltex_nuclear.reactor.heat_removed", VoltexBars.big((int) Math.round(t.removed()))));
        lines.add(Component.translatable("gui.voltex_nuclear.reactor.heat_hull", VoltexBars.big((int) Math.round(t.toHull()))));
        if (t.waterMb() > 0) lines.add(Component.translatable("gui.voltex_nuclear.reactor.water", VoltexBars.decimal(t.waterMb() * 20, 1)));
        if (t.coolantMb() > 0) lines.add(Component.translatable("gui.voltex_nuclear.reactor.coolant", VoltexBars.decimal(t.coolantMb() * 20, 1)));
        lines.add(Component.translatable("jei.voltex_nuclear.scheme.chambers", scheme.chambers().length()));
        return lines;
    }
}
