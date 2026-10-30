package com.jykito.voltexnuclear.compat.jei;

import com.jykito.industrialcore.jei.JeiGui;
import com.jykito.voltexnuclear.client.VoltexBars;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.reactor.Fission;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.reactor.ReactorParts;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The reactor's irradiation channel in JEI (stage 12, DESIGN step 17): a blank stands in a cell of the zone and becomes
 * its product (ReactorParts.product): the lithium blank a tritium cell, the isotope blank the third suit's pellet, the
 * four blanks of step 15 the restructor's isotopes, a pack's blanks theirs. One line: about how long it takes in a good
 * channel; the guide tells what makes a channel good (JEI the minimum, the author). The reactor core opens it. Laid out
 * by jei_gui.js, in the reactor's frame.
 */
final class ChannelCategory extends NuclearCategory<ChannelCategory.Turn> {

    /** A blank, what it becomes, and about how many ticks it takes in a good channel. */
    record Turn(Item blank, Item product, long ticks) {}

    static final RecipeType<Turn> TYPE = RecipeType.create(ModInfo.MOD_ID, "irradiation_channel", Turn.class);

    // the slots, the chevron bar and the lines, as jei_gui.js draws them
    private static final int H = 54, SLOT_Y = 11, BLANK_X = 40, PRODUCT_X = 96, PROG_X = 62, PROG_Y = 16, PROG_W = 28;
    private static final int LINE_Y = 35, C_TIME = 0xFFB0B0B0;
    /** A good channel, rods on all four sides at full power, gathers the lithium blank's work in about 30 seconds (Fission). */
    private static final double GOOD_PER_TICK = Fission.CHANNEL_WORK / 600;

    ChannelCategory(IGuiHelper gui) {
        super(gui, "channel", H, H, NuclearBlocks.REACTOR_CORE.get());
    }

    /** Every blank with its product, in the order of the parts: the add-on's as the player meets them, then a pack's. */
    static List<Turn> all() {
        List<Turn> out = new ArrayList<>();
        for (Item blank : ReactorParts.items()) {
            Part part = ReactorParts.of(new ItemStack(blank));
            ItemStack product = ReactorParts.product(blank);
            if (part == null || part.kind() != Part.Kind.CHANNEL || product.isEmpty()) continue;
            out.add(new Turn(blank, product.getItem(), Math.round(part.work() / GOOD_PER_TICK)));
        }
        return out;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.voltex_nuclear.channel");
    }

    @Override
    public RecipeType<Turn> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Turn turn, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, BLANK_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(turn.blank()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, PRODUCT_X + 1, SLOT_Y + 1).addItemStack(new ItemStack(turn.product()));
    }

    @Override
    void drawProgress(Turn turn, GuiGraphics g) {
        int w = (int) ((PROG_W - 2) * loop(LATE_LOOP_TICKS));
        if (w > 0) VoltexBars.progressNow(g, PROG_X + 1, PROG_Y + 1, w);
    }

    @Override
    void drawInfo(Turn turn, GuiGraphics g, Font font) {
        Component time = turn.ticks() < 1200 ? Component.literal("≈" + JeiGui.secs((int) turn.ticks()))
                : Component.translatable("gui.voltex_nuclear.jei.channel.minutes", Math.round(turn.ticks() / 1200.0));
        g.drawString(font, time, BLANK_X, LINE_Y, C_TIME, false);
    }

    @Override
    List<Component> tooltip(Turn turn, double mx, double my) {
        return List.of();
    }
}
