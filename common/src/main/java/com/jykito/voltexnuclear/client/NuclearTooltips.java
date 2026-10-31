package com.jykito.voltexnuclear.client;

import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.core.reactor.Cooling;
import com.jykito.voltexnuclear.core.reactor.Fission;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.reactor.ReactorParts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Extra tooltip lines of the add-on items. The item tooltip method has a different signature on 1.20.1 and 1.21.1,
 * so each loader passes its tooltip event here instead.
 */
public final class NuclearTooltips {

    private NuclearTooltips() {}

    private static final double TICKS_PER_MINUTE = 1200;

    public static void append(ItemStack stack, List<Component> lines) {
        if (stack.is(NuclearItems.GEIGER_COUNTER.get())) {
            lines.add(Component.translatable("item.voltex_nuclear.geiger_counter.tooltip").withStyle(ChatFormatting.GRAY));
        }
        Part part = ReactorParts.of(stack);
        if (part == null) return;
        // the add-on's own reactor parts point to the guide: how the zone works is a chapter, not a tooltip
        boolean own = stack.getDescriptionId().startsWith("item." + ModInfo.MOD_ID + ".");
        if (part.isFuel()) {
            int minutes = (int) Math.ceil((part.life() - ReactorParts.wear(stack)) / TICKS_PER_MINUTE);
            lines.add(Component.translatable("tooltip.voltex_nuclear.rod_left", Math.max(0, minutes)).withStyle(ChatFormatting.GRAY));
            if (own) lines.add(Component.translatable("tooltip.voltex_nuclear.see_guide").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (ReactorParts.fromPack(stack.getItem())) {   // a pack's part has no line of its own in the lang files
            Cooling k = part.cooling();
            if (k != null) lines.add((k.drinks() ? Component.translatable("tooltip.voltex_nuclear.pack_cell", (int) k.rate(), (int) k.heatPerMb())
                    : Component.translatable("tooltip.voltex_nuclear.pack_sink", (int) k.rate())).withStyle(ChatFormatting.GRAY));
            return;
        }
        // what the component does in the grid; the numbers come from the simulation, so the text follows the balance
        Object[] args = switch (part.kind()) {
            case REFLECTOR -> new Object[]{percent(Fission.REFLECTOR)};
            // the boron absorber has no line (the author on 2026-10-30): no layout gains from it, its job is the hot cell's shield
            case CONTROL_ROD -> new Object[]{percent(Fission.CONTROL_ROD_THROTTLE)};
            case CRYO_CELL -> new Object[]{(int) part.cooling().rate(), (int) part.cooling().heatPerMb()};
            case HEAVY_WATER -> new Object[0];
            default -> null;   // the Voltex parts
        };
        if (args != null) lines.add(Component.translatable(stack.getDescriptionId() + ".tooltip", args).withStyle(ChatFormatting.GRAY));
        // the two things a cryo cell needs that its numbers do not say: rods beside it, and room for the hot coolant
        if (part.kind() == Part.Kind.CRYO_CELL) {
            lines.add(Component.translatable("tooltip.voltex_nuclear.outlet_near").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("item.voltex_nuclear.cryo_cell.tooltip_out").withStyle(ChatFormatting.GRAY));
        }
        if (own) lines.add(Component.translatable("tooltip.voltex_nuclear.see_guide").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static int percent(double share) {
        return (int) Math.round(share * 100);
    }
}
