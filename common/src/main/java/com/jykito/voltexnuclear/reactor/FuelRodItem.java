package com.jykito.voltexnuclear.reactor;

import com.jykito.voltexnuclear.core.reactor.Fission;
import com.jykito.voltexnuclear.core.reactor.Part;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A fuel rod, single or assembled. Its wear lives in the item ({@link ReactorParts#wear}), so a rod taken out of a
 * reactor goes on where it stopped; a bar under it shows the life left, as durability does (DESIGN step 17).
 */
public class FuelRodItem extends Item {

    public FuelRodItem(Properties properties) {
        super(properties);
    }

    /** Share of the life left: 1 for a fresh rod, 0 for one about to burn out. */
    public static double left(ItemStack stack) {
        Part part = ReactorParts.of(stack);
        return Mth.clamp(1 - ReactorParts.wear(stack) / (part != null && part.isFuel() ? part.life() : Fission.ROD_LIFE), 0, 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return ReactorParts.wear(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * (float) left(stack));
    }

    /** Green to red as the life runs out, the colours of durability. */
    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) left(stack) / 3, 1, 1);
    }
}
