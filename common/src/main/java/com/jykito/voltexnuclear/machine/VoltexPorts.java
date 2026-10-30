package com.jykito.voltexnuclear.machine;

import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Items and fluids in the Voltex ports of an add-on multiblock (the cascade, the fission reactor), the way the Voltex late
 * cores handle them. The port's item handler and tanks are different classes on each loader, but these calls are spelled
 * the same on both, so the shared code compiles against either. Draining and filling a tank need the loader's own
 * types; those are in {@link Platform}, the fluid helpers here go through it.
 */
public final class VoltexPorts {

    private VoltexPorts() {}

    public static int count(List<ItemFluidPortBlockEntity> ports, Item item) {
        long n = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots(); i++) {
                ItemStack s = port.getItems().getStackInSlot(i);
                if (s.is(item)) n += s.getCount();
            }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    public static void take(List<ItemFluidPortBlockEntity> ports, Item item, int amount) {
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots() && amount > 0; i++) {
                ItemStack s = port.getItems().getStackInSlot(i);
                if (s.is(item)) amount -= port.getItems().extractItem(i, Math.min(amount, s.getCount()), false).getCount();
            }
    }

    /** One of the item out of the ports as it lies there, with its data (a worn rod keeps its wear); empty when none. */
    public static ItemStack takeOne(List<ItemFluidPortBlockEntity> ports, Item item) {
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots(); i++)
                if (port.getItems().getStackInSlot(i).is(item)) return port.getItems().extractItem(i, 1, false);
        return ItemStack.EMPTY;
    }

    /**
     * Whether {@code a} of one item and {@code b} of another fit into the outputs together: each tops up its own stacks
     * first, what is left shares the empty slots.
     */
    public static boolean fits(List<ItemFluidPortBlockEntity> ports, Item itemA, int a, Item itemB, int b) {
        return fits(ports, List.of(new ItemStack(itemA, a), new ItemStack(itemB, b)));
    }

    /** The same for any number of stacks, each of its own item (the radiochemical plant gives five); empty ones need nothing. */
    public static boolean fits(List<ItemFluidPortBlockEntity> ports, List<ItemStack> stacks) {
        int empty = 0;
        int[] left = new int[stacks.size()];
        for (int k = 0; k < left.length; k++) left[k] = stacks.get(k).getCount();
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots(); i++) {
                ItemStack s = port.getItems().getStackInSlot(i);
                if (s.isEmpty()) { empty++; continue; }
                for (int k = 0; k < left.length; k++)
                    if (!stacks.get(k).isEmpty() && s.is(stacks.get(k).getItem())) { left[k] -= Math.max(0, s.getMaxStackSize() - s.getCount()); break; }
            }
        int slots = 0;
        for (int k = 0; k < left.length; k++) {
            int max = stacks.get(k).getMaxStackSize();
            slots += (Math.max(0, left[k]) + max - 1) / max;
        }
        return slots <= empty;
    }

    public static void put(List<ItemFluidPortBlockEntity> ports, Item item, int amount) {
        for (ItemFluidPortBlockEntity port : ports) {
            if (amount <= 0) return;
            ItemStack rest = port.insertOutput(new ItemStack(item, amount), false);
            amount = rest.getCount();
        }
    }

    /** Puts a stack into the output ports as far as it fits; returns what is left. */
    public static ItemStack put(List<ItemFluidPortBlockEntity> ports, ItemStack stack) {
        for (ItemFluidPortBlockEntity port : ports) {
            if (stack.isEmpty()) break;
            stack = port.insertOutput(stack, false);
        }
        return stack;
    }

    /** mB of the fluid in the tanks of the ports (an ME input and a pattern port have up to three). */
    public static int fluid(List<ItemFluidPortBlockEntity> ports, Fluid fluid) {
        long n = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (var tank : port.tanks())
                if (tank.getFluid().getFluid().isSame(fluid)) n += tank.getFluidAmount();
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    /** mB the tanks of the ports could hold of the fluid: those holding it and the empty ones, full. */
    public static int capacity(List<ItemFluidPortBlockEntity> ports, Fluid fluid) {
        long n = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (var tank : port.tanks())
                if (tank.isEmpty() || tank.getFluid().getFluid().isSame(fluid)) n += tank.getCapacity();
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    /**
     * A fluid used in fractions of a mB a tick (the reactor's cells, the column's cold tiers): takes from the input tanks
     * what was used beyond what was drained before, whole mB, and pours as much of {@code back} (the coolant comes back
     * hot) into the output tanks; returns what is left drained but unused, for the next tick.
     */
    public static double draw(List<ItemFluidPortBlockEntity> inputs, Fluid fluid, double used, double credit, @Nullable Fluid back,
                              List<ItemFluidPortBlockEntity> outputs) {
        double need = used - credit;
        if (need <= 0) return credit - used;
        Platform platform = VoltexNuclear.platform();
        int want = (int) Math.ceil(need - 1e-9), got = 0;
        for (ItemFluidPortBlockEntity port : inputs) {
            if (got >= want) break;
            got += platform.drainPort(port, fluid, want - got);
        }
        if (back != null && got > 0) put(outputs, back, got);
        return Math.max(0, credit + got - used);
    }

    /** Pours a fluid into the output tanks as far as it goes; returns the mB that did not fit. */
    public static int put(List<ItemFluidPortBlockEntity> outputs, Fluid fluid, int amount) {
        Platform platform = VoltexNuclear.platform();
        for (ItemFluidPortBlockEntity port : outputs) {
            if (amount <= 0) break;
            amount -= platform.fillPort(port, fluid, amount, false);
        }
        return amount;
    }

    /** mB of a fluid the output tanks would take now, asking for at most {@code max}. */
    public static int room(List<ItemFluidPortBlockEntity> outputs, Fluid fluid, int max) {
        Platform platform = VoltexNuclear.platform();
        int room = 0;
        for (ItemFluidPortBlockEntity port : outputs) {
            if (room >= max) break;
            room += platform.fillPort(port, fluid, max - room, true);
        }
        return room;
    }

    /**
     * Gives energy (or heat) to the blocks round a port that are not the machine's own ({@code own}); returns what they
     * took. The add-on's multiblocks push their output this way through the energy output connectors and the heat ports.
     */
    public static int toNeighbours(Level level, BlockPos port, int amount, boolean asHeat, Predicate<BlockPos> own) {
        Platform platform = VoltexNuclear.platform();
        int took = 0;
        for (Direction d : Direction.values()) {
            if (took >= amount) break;
            BlockPos n = port.relative(d);
            if (own.test(n)) continue;
            took += asHeat ? platform.sendHeat(level, n, d.getOpposite(), amount - took) : platform.sendEnergy(level, n, d.getOpposite(), amount - took);
        }
        return took;
    }
}
