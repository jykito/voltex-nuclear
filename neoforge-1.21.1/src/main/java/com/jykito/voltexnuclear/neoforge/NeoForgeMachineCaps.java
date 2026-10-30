package com.jykito.voltexnuclear.neoforge;

import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.machine.EnergyIn;
import com.jykito.voltexnuclear.machine.EnergyOutputPortBlockEntity;
import com.jykito.voltexnuclear.machine.FluidIn;
import com.jykito.voltexnuclear.machine.Machine;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/**
 * The add-on machines as NeoForge 1.21.1 sees them: the energy buffer as energy storage, the slots as an item handler per
 * side, a tank as a fluid handler.
 */
final class NeoForgeMachineCaps {

    private NeoForgeMachineCaps() {}

    static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new NeoForgeStellarArmorItem.Energy(stack), NuclearItems.stellarPieces());
        machine(event, NuclearBlockEntities.PELLET_PRESS.get());
        machine(event, NuclearBlockEntities.ELECTROLYZER.get());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NuclearBlockEntities.ENERGY_OUTPUT_PORT.get(), (be, side) -> new Out(be));
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

    private static <B extends BlockEntity & Machine> void machine(RegisterCapabilitiesEvent event, BlockEntityType<B> type) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> new Energy(be.energy()));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> side == null ? new InvWrapper(be) : new SidedInvWrapper(be, side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> be.fluid() == null ? null : new Tank(be.fluid()));
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
