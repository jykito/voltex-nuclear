package com.jykito.voltexnuclear.reactor;

import com.jykito.voltexnuclear.core.reactor.Part;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A channel blank (DESIGN step 17): it stands in a cell of the zone itself, the flux of its neighbours irradiates it, and
 * at the work of its part ({@link Part#work}) it becomes its product ({@link ReactorParts#product}). The irradiation lives
 * in the item ({@link ReactorParts#wear}), so a blank taken out goes on where it stopped; a bar under it fills up as it
 * is irradiated, in the colour of what it becomes.
 */
public class ChannelBlankItem extends Item {

    /**
     * The bars' colours: the poison green of tritium (step 19), the ember of the isotope pellet; the restructor's isotopes in
     * the colours of what they come from: graphite, gold, plutonium, the yellow of a sodium flame.
     */
    public static final int TRITIUM = 0x9CFF3A, ISOTOPE = 0xFF8A2E, GRAPHITE = 0x8FA3B8, GOLD = 0xFFCF40, PLUTONIUM = 0xE0603A,
            SODIUM = 0xFFB347;

    private final int bar;

    public ChannelBlankItem(Properties properties, int bar) {
        super(properties);
        this.bar = bar;
    }

    /** Share of the irradiation done: 0 for a fresh blank, 1 for one about to turn. */
    public static double done(ItemStack stack) {
        Part part = ReactorParts.of(stack);
        return part == null || part.kind() != Part.Kind.CHANNEL ? 0 : Mth.clamp(ReactorParts.wear(stack) / part.work(), 0, 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return ReactorParts.wear(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * (float) done(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return bar;
    }
}
