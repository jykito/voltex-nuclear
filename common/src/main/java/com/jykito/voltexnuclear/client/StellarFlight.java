package com.jykito.voltexnuclear.client;

import com.jykito.voltexnuclear.suit.StellarArmor;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Stellar suit's inertia damping (the author: "fast flight and inertia damping"). The player's own movement runs on
 * the client, so the damping is here: in flight with no key held the suit stops at once instead of drifting on, across
 * and up and down alike. The server pays for it ({@link StellarArmor}). Each loader calls {@link #tick} once per client
 * tick, after the player has moved, so the next move starts from rest.
 */
public final class StellarFlight {

    private StellarFlight() {}

    public static void tick(LocalPlayer player) {
        if (player == null || player.input == null || !StellarArmor.damps(player)) return;
        Input in = player.input;
        boolean across = in.forwardImpulse != 0 || in.leftImpulse != 0, upDown = in.jumping != in.shiftKeyDown;
        if (across && upDown) return;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(across ? v.x : 0, upDown ? v.y : 0, across ? v.z : 0);
    }
}
