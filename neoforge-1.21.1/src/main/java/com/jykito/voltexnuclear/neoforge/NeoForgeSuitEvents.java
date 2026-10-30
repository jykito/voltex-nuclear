package com.jykito.voltexnuclear.neoforge;

import com.jykito.voltexnuclear.suit.StellarArmor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The Stellar power suit's game events on NeoForge 1.21.1: the wearer's tick, fire kept off whole, damage turned away. */
final class NeoForgeSuitEvents {

    private NeoForgeSuitEvents() {}

    static void tick(PlayerTickEvent.Post event) {
        StellarArmor.tick(event.getEntity());
    }

    static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (StellarArmor.keepsOff(p, event.getSource())) event.setCanceled(true);
        else if (!p.level().isClientSide) event.setAmount(StellarArmor.absorb(p, event.getSource(), event.getAmount()));
    }
}
