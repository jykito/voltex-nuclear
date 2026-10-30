package com.jykito.voltexnuclear.machine;

import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

/**
 * A tank of one fluid a machine takes in and spends, with no loader types in it, as {@link EnergyIn} is for energy: each
 * loader wraps it into its own fluid handler. Pipes fill it with that fluid only and take nothing back out, as from the
 * input tanks of the Voltex machines.
 */
public final class FluidIn {

    private final Supplier<Fluid> fluid;
    private final int capacity;
    private final Runnable changed;
    private int amount;

    /** {@code fluid} is read when asked, after the fluids are registered; {@code changed} runs whenever the amount changes. */
    public FluidIn(Supplier<Fluid> fluid, int capacity, Runnable changed) {
        this.fluid = fluid;
        this.capacity = capacity;
        this.changed = changed;
    }

    /** Takes up to {@code max} mB of the fluid; another fluid it does not take. Returns how much it took, or would. */
    public int fill(Fluid offered, int max, boolean simulate) {
        if (!takes(offered)) return 0;
        int in = Math.max(0, Math.min(max, capacity - amount));
        if (!simulate && in > 0) {
            amount += in;
            changed.run();
        }
        return in;
    }

    /** Whether this is the fluid of the tank (the flowing kind counts as the source). */
    public boolean takes(Fluid offered) {
        return offered != null && offered.isSame(fluid.get());
    }

    /** Spends {@code mb} if the tank has that much. */
    public boolean use(int mb) {
        if (amount < mb) return false;
        amount -= mb;
        changed.run();
        return true;
    }

    public Fluid fluid() {
        return fluid.get();
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    /** Sets the amount on loading, kept within the tank. */
    public void set(int value) {
        amount = Math.max(0, Math.min(capacity, value));
    }
}
