package com.jykito.voltexnuclear.client;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.suit.StellarArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The isotope pellet burning in a Stellar chestplate, in Voltex's armour HUD: Voltex draws a row for every worn piece
 * with energy, from the boots up, the energy on its first line; this writes the pellet's minutes on the chestplate's
 * second line, where the Plasma chestplate shows its burst charge, in the same place and size.
 */
public final class StellarHud {

    private StellarHud() {}

    /** Voltex's rows (ArmorEnergyHudOverlay): their order, their height, where the first stands, the text's place and size. */
    private static final EquipmentSlot[] ROWS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static final int ROW_H = 22, X = 5, FROM_BOTTOM = 55, TEXT_X = 19, TEXT_Y = 1, LINE2_Y = 9;
    private static final float TEXT_SCALE = 0.75f;
    private static final int C_PELLET = 0xFFFFC24A;

    public static void render(GuiGraphics g, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!StellarArmor.is(chest)) return;
        int burn = StellarArmor.pelletTicks(chest);
        if (burn <= 0) return;
        int row = 0;
        for (EquipmentSlot slot : ROWS) {
            if (slot == EquipmentSlot.CHEST) break;
            if (VoltexNuclear.platform().hasEnergy(player.getItemBySlot(slot))) row++;
        }
        int y = height - FROM_BOTTOM - row * ROW_H;
        g.pose().pushPose();
        g.pose().translate(X + TEXT_X, y + TEXT_Y, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
        g.drawString(mc.font, Component.translatable("hud.voltex_nuclear.stellar.pellet", StellarArmor.minutes(burn)), 0, LINE2_Y, C_PELLET, true);
        g.pose().popPose();
    }
}
