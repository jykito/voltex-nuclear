package com.jykito.voltexnuclear.forge;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.machine.EnergyIn;
import com.jykito.voltexnuclear.machine.EnergyOutputPortBlockEntity;
import com.jykito.voltexnuclear.machine.FluidIn;
import com.jykito.voltexnuclear.machine.Machine;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The add-on machines as Forge 1.20.1 sees them: the energy buffer as Forge energy, the slots as an item handler per
 * side, a tank as a fluid handler. Attached to every block entity that is a {@link Machine}. The event comes inside the block entity's constructor,
 * before its own fields are set, so everything here is made on first use.
 */
final class ForgeMachineCaps {

    private ForgeMachineCaps() {}

    static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof EnergyOutputPortBlockEntity port) {
            LazyOptional<IEnergyStorage> out = LazyOptional.of(() -> new Out(port));
            event.addCapability(VoltexNuclear.id("energy_output"), new ICapabilityProvider() {
                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                    return cap == ForgeCapabilities.ENERGY ? out.cast() : LazyOptional.empty();
                }
            });
            event.addListener(out::invalidate);
            return;
        }
        if (!(event.getObject() instanceof Machine machine)) return;
        Provider provider = new Provider(machine);
        event.addCapability(VoltexNuclear.id("machine"), provider);
        event.addListener(provider::invalidate);
    }

    private static final class Provider implements ICapabilityProvider {
        private final Machine machine;
        private final LazyOptional<IEnergyStorage> energy;
        private final LazyOptional<IFluidHandler> fluid;
        private final LazyOptional<IItemHandler> all;
        private final Map<Direction, LazyOptional<IItemHandler>> sides = new EnumMap<>(Direction.class);

        Provider(Machine machine) {
            this.machine = machine;
            energy = LazyOptional.of(() -> new Energy(machine.energy()));
            fluid = LazyOptional.of(() -> new Tank(machine.fluid()));
            all = LazyOptional.of(() -> new InvWrapper(machine));
            for (Direction side : Direction.values()) sides.put(side, LazyOptional.of(() -> new SidedInvWrapper(machine, side)));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ENERGY) return energy.cast();
            if (cap == ForgeCapabilities.ITEM_HANDLER) return (side == null ? all : sides.get(side)).cast();
            // asked, not attached: the tank is made in the constructor, after this provider
            if (cap == ForgeCapabilities.FLUID_HANDLER && machine.fluid() != null) return fluid.cast();
            return LazyOptional.empty();
        }

        void invalidate() {
            energy.invalidate();
            fluid.invalidate();
            all.invalidate();
            sides.values().forEach(LazyOptional::invalidate);
        }
    }

    /** The energy output connector: gives the energy of its multiblock out, takes none in. */
    private record Out(EnergyOutputPortBlockEntity port) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int max, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return port.extract(max, simulate);
        }

        @Override
        public int getEnergyStored() {
            return port.stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return port.capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

/** Takes its one fluid in from pipes, gives none out, as the input tanks of the Voltex machines. */
    private record Tank(FluidIn in) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return in.amount() == 0 ? FluidStack.EMPTY : new FluidStack(in.fluid(), in.amount());
        }

        @Override
        public int getTankCapacity(int tank) {
            return in.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return in.takes(stack.getFluid());
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return stack.isEmpty() ? 0 : in.fill(stack.getFluid(), stack.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int max, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /** Takes energy in, gives none out. */
    private record Energy(EnergyIn in) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int max, boolean simulate) {
            return in.receive(max, simulate);
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return in.stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return in.capacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
