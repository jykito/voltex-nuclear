package com.jykito.voltexnuclear.forge.client;

import com.jykito.voltexnuclear.cascade.CascadeMenu;
import com.jykito.voltexnuclear.client.CascadeScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** On 1.20.1 a screen dims the world behind itself; from 1.20.2 the game does it, so only this version needs the call. */
final class ForgeCascadeScreen extends CascadeScreen {

    ForgeCascadeScreen(CascadeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        super.render(g, mx, my, partialTick);
    }
}
