package com.jykito.voltexnuclear.machine;

/**
 * A buffer of FE a machine takes in and spends, with no loader types in it: Forge and NeoForge keep their energy
 * capability in different packages, so each loader wraps this into its own. It takes as much as fits in a tick, as the
 * Voltex machines do, and gives nothing back out.
 */
public final class EnergyIn {

    private final int capacity;
    private final Runnable changed;
    private int stored;

    /** {@code changed} runs whenever the amount changes (the block entity marks itself for saving). */
    public EnergyIn(int capacity, Runnable changed) {
        this.capacity = capacity;
        this.changed = changed;
    }

    /** Takes up to {@code max} FE; returns how much it took, or would take when simulating. */
    public int receive(int max, boolean simulate) {
        int in = Math.max(0, Math.min(max, capacity - stored));
        if (!simulate && in > 0) {
            stored += in;
            changed.run();
        }
        return in;
    }

    /** Spends {@code amount} FE if it has that much; a machine without enough for the tick waits. */
    public boolean use(int amount) {
        if (stored < amount) return false;
        stored -= amount;
        changed.run();
        return true;
    }

    public int stored() {
        return stored;
    }

    public int capacity() {
        return capacity;
    }

    /** Sets the amount on loading, kept within the buffer. */
    public void set(int value) {
        stored = Math.max(0, Math.min(capacity, value));
    }
}
