package com.jykito.voltexnuclear.core.reactor;

import java.util.EnumSet;
import java.util.Set;

/**
 * The active zone (DESIGN step 5): a 7 x 7 grid, of which the middle 5 x 5 is always open. An expansion chamber in a
 * socket of the hull opens the row or the column on its side; a corner cell opens with both chambers next to it. A part
 * stands only in an open cell.
 */
public final class Zone {

    public static final int SIZE = 7;

    public enum Side { LEFT, RIGHT, TOP, BOTTOM }

    private final Part[][] parts = new Part[SIZE][SIZE];
    private final EnumSet<Side> chambers = EnumSet.noneOf(Side.class);

    public Set<Side> chambers() {
        return EnumSet.copyOf(chambers);
    }

    /** Puts in or takes out a chamber. A chamber taken out leaves its cells shut: what stood there is the caller's to give back. */
    public void setChamber(Side side, boolean present) {
        if (present) chambers.add(side);
        else chambers.remove(side);
    }

    public boolean open(int row, int col) {
        return row >= (chambers.contains(Side.TOP) ? 0 : 1) && row <= (chambers.contains(Side.BOTTOM) ? 6 : 5)
                && col >= (chambers.contains(Side.LEFT) ? 0 : 1) && col <= (chambers.contains(Side.RIGHT) ? 6 : 5);
    }

    /** The part in the cell; null for an empty cell, a shut one or one off the grid. */
    public Part get(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE && open(row, col) ? parts[row][col] : null;
    }

    public void set(int row, int col, Part part) {
        if (part != null && !open(row, col)) throw new IllegalArgumentException("cell " + row + "," + col + " is shut");
        parts[row][col] = part;
    }

    /** Source of the cell, 0 unless it holds fuel. */
    double source(int row, int col) {
        Part p = get(row, col);
        return p == null ? 0 : p.source();
    }

    Part.Kind kind(int row, int col) {
        Part p = get(row, col);
        return p == null ? null : p.kind();
    }
}
