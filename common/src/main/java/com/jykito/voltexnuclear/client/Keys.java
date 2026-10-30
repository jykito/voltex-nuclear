package com.jykito.voltexnuclear.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The keys of the reactor window, the look the author chose on the workshop page :8781/nuclear_buttons.html ("keys with a
 * lamp"; the cascade keeps its own stage buttons, which the author liked better there): a raised key in the greys of the Voltex slots, the top and left edge lit and the bottom and right edge in shade,
 * a C2 slot turned inside out. Under the mouse its outline turns uranium green; pressed, the key sinks (the edges swap)
 * and its lamp and label go down a pixel. The lamp at the left shows a state; the label is the game's text with no
 * shadow, which would not fit the key. The knob keys are 9 x 9, odd both ways, with their sign drawn in pixels so it has
 * an exact middle.
 */
public final class Keys {

    private Keys() {}

    /** The side of a knob key. */
    public static final int KNOB = 9;

    private static final int EDGE = 0xFF0F0F0F, HOVER = 0xFF7DDC4B, SHADE = 0xFF191919, LABEL = 0xFFE0E0E0, SOCKET = 0xFF0B0B0B;
    /** The face (upper half, lower half) and its lit edge: plain, under the mouse, pressed. */
    private static final int[][] FACE = {{0xFF363636, 0xFF2E2E2E, 0xFF505050}, {0xFF3C3C3C, 0xFF343434, 0xFF565656},
            {0xFF2A2A2A, 0xFF262626, 0xFF404040}};

    /**
     * A key with a lamp and its label. {@code lamp} is the lamp's colour as it shows now (a dark one while it is off);
     * {@code lit} adds the glint of a lit lamp.
     */
    public static void key(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, int lamp, boolean lit,
                           boolean hover, boolean pressed) {
        int dy = body(g, x, y, w, h, hover, pressed);
        fill(g, x + 2, y + 3 + dy, 3, 6, SOCKET);
        fill(g, x + 3, y + 4 + dy, 1, 4, lamp);
        if (lit) fill(g, x + 3, y + 4 + dy, 1, 1, lighten(lamp));
        int area = w - 7;
        g.drawString(font, label, x + 5 + (area - font.width(label)) / 2, y + 2 + dy, LABEL, false);
    }

    /** A knob key: "-" or "+" in pixels round its middle. */
    public static void knob(GuiGraphics g, int x, int y, boolean plus, boolean hover, boolean pressed) {
        body(g, x, y, KNOB, KNOB, hover, pressed);
        fill(g, x + 2, y + 4, 5, 1, LABEL);
        if (plus) fill(g, x + 4, y + 2, 1, 5, LABEL);
    }

    /** The outline, the face in two halves and the edges; returns how far the content goes down (1 when pressed). */
    private static int body(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean pressed) {
        int[] face = FACE[pressed ? 2 : hover ? 1 : 0];
        int lit = pressed ? SHADE : face[2], shade = pressed ? face[2] : SHADE, upper = (h - 2) / 2;
        fill(g, x, y, w, h, hover ? HOVER : EDGE);
        fill(g, x + 1, y + 1, w - 2, upper, face[0]);
        fill(g, x + 1, y + 1 + upper, w - 2, h - 2 - upper, face[1]);
        fill(g, x + 1, y + 1, w - 2, 1, lit);
        fill(g, x + 1, y + 1, 1, h - 2, lit);
        fill(g, x + 1, y + h - 2, w - 2, 1, shade);
        fill(g, x + w - 2, y + 1, 1, h - 2, shade);
        return pressed ? 1 : 0;
    }

    /** Halfway to white: the glint of a lamp. */
    private static int lighten(int argb) {
        int r = ((argb >> 16 & 0xFF) + 0xFF) / 2, gr = ((argb >> 8 & 0xFF) + 0xFF) / 2, b = ((argb & 0xFF) + 0xFF) / 2;
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }
}
