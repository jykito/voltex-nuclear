package com.jykito.voltexnuclear.forge;

import com.jykito.voltexnuclear.suit.StellarArmor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** The Stellar power suit's game events on Forge 1.20.1: the wearer's tick, fire kept off whole, damage turned away. */
final class ForgeSuitEvents {

    private ForgeSuitEvents() {}

    static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) StellarArmor.tick(event.player);
    }

    static void attack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player p && StellarArmor.keepsOff(p, event.getSource())) event.setCanceled(true);
    }

    static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player p) event.setAmount(StellarArmor.absorb(p, event.getSource(), event.getAmount()));
    }
}
