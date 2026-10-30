package com.jykito.voltexnuclear.client;

import net.minecraft.network.chat.Component;

import java.util.function.IntUnaryOperator;

/**
 * A machine's state as its window writes it, the words and their colour. Each window makes it from the numbers it gets
 * (a static {@code status} beside the window), so Jade, which gets the same numbers from the server, says the same.
 */
public record StatusLine(Component text, int color) {

    /** A number the window gets in two cells of 16 bits, as the menus join it (getWide). */
    public static int wide(IntUnaryOperator d, int index) {
        return (d.applyAsInt(index) & 0xFFFF) | (d.applyAsInt(index + 1) << 16);
    }
}
