package com.jykito.voltexnuclear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The scale of the Geiger counter over the hotbar (DESIGN step 1): ten cells lit in steps by the signal, in the uranium
 * green of the add-on on the dark steel of the Voltex electronics. Each loader draws it as a layer above the hotbar.
 */
public final class GeigerHud {

    private GeigerHud() {}

    private static final int CELLS = 10, CELL_W = 4, CELL_H = 5, GAP = 1;
    private static final int W = 2 + CELLS * (CELL_W + GAP) + 1, H = CELL_H + 4;
    private static final int EDGE = 0xFF0C0F15, BACK = 0xFF181E2A, OFF = 0xFF242C3E;
    /** Lit cells grow brighter towards the strong end, in four steps. */
    private static final int[] LIT = { 0xFF52742B, 0xFF52742B, 0xFF52742B, 0xFF6A9136, 0xFF6A9136, 0xFF86AE42, 0xFF86AE42,
            0xFFA6CB58, 0xFFA6CB58, 0xFFCBE68A };

    public static void render(GuiGraphics g, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !GeigerClient.holding(mc.player)) return;
        // over the hearts and the food in survival, right over the hotbar where there are none
        boolean bars = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        int x = width / 2 - W / 2, y = height - (bars ? 60 : 36);
        int lit = (int) Math.ceil(GeigerClient.strength() * CELLS);
        g.fill(x, y, x + W, y + H, EDGE);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, BACK);
        for (int i = 0; i < CELLS; i++) {
            int cx = x + 2 + i * (CELL_W + GAP);
            g.fill(cx, y + 2, cx + CELL_W, y + 2 + CELL_H, i < lit ? LIT[i] : OFF);
        }
    }
}
