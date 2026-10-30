package com.jykito.voltexnuclear.neoforge;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.core.Holder;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.MachineIO;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** {@link Platform} on NeoForge 1.21.1. */
final class NeoForgePlatform implements Platform {

    private final IEventBus modBus;
    /** One deferred register per game registry, made on first use and hooked to the mod bus right away. */
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new HashMap<>();

    /** The Stellar suit's armour material, in the game's registry before the items that use it. */
    private final Supplier<ArmorMaterial> stellarMaterial;

    NeoForgePlatform(IEventBus modBus) {
        this.modBus = modBus;
        stellarMaterial = register(Registries.ARMOR_MATERIAL, "stellar", NeoForgeStellarArmorItem::material);
    }

    @Override
    public String describe() {
        return "NeoForge 1.21.1";
    }

    @Override
    public ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) registers.get(registry);
        if (register == null) {
            register = DeferredRegister.create(registry, ModInfo.MOD_ID);
            register.register(modBus);
            registers.put(registry, register);
        }
        return register.register(name, factory);
    }

    /** As Voltex's ModFluids on this version; the tint is set up on the client by NeoForgeClientEvents from NuclearFluids. */
    @Override
    public Supplier<Fluid> fluid(String name, int tint, ResourceLocation still, ResourceLocation flow) {
        Supplier<FluidType> type = register(NeoForgeRegistries.Keys.FLUID_TYPES, name + "_fluid",
                () -> new FluidType(FluidType.Properties.create().density(1100).viscosity(1000)));
        BaseFlowingFluid.Properties[] properties = new BaseFlowingFluid.Properties[1];
        Supplier<Fluid> source = register(Registries.FLUID, name, () -> new BaseFlowingFluid.Source(properties[0]));
        Supplier<Fluid> flowing = register(Registries.FLUID, name + "_flowing", () -> new BaseFlowingFluid.Flowing(properties[0]));
        properties[0] = new BaseFlowingFluid.Properties(type, source, flowing).bucket(() -> Items.AIR);
        return source;
    }

    /** The block entity always has the registries here: this version saves through the method that passes them. */
    @Override
    public void saveItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public void loadItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public MachineUpgrades upgrades(Container machine, int first, int last) {
        return MachineUpgrades.scan(new InvWrapper(machine), first, last);
    }

    @Override
    public boolean pullItems(Level level, BlockPos pos, Direction side, Container into, int... targets) {
        return MachineIO.pullItems(level, pos, side, new InvWrapper(into), targets);
    }

    @Override
    public boolean pushItems(Level level, BlockPos pos, Direction side, Container from, int... slots) {
        return MachineIO.pushItems(level, pos, side, new InvWrapper(from), slots);
    }

    @Override
    public double itemNumber(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getDouble(key);
    }

    /** {@code CustomData.update} drops the component once its tag is empty, so the stack matches fresh ones again. */
    @Override
    public void setItemNumber(ItemStack stack, String key, double value) {
        if (value != 0) CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble(key, value));
        else if (stack.has(DataComponents.CUSTOM_DATA)) CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(key));
    }

    @Override
    public int drainPort(ItemFluidPortBlockEntity port, Fluid fluid, int max) {
        int took = 0;
        for (FluidTank tank : port.tanks()) {
            if (took >= max) break;
            if (tank.getFluid().getFluid().isSame(fluid)) took += tank.drain(max - took, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        return took;
    }

    @Override
    public int fillPort(ItemFluidPortBlockEntity port, Fluid fluid, int amount, boolean simulate) {
        int put = 0;
        for (FluidTank tank : port.tanks()) {
            if (put >= amount) break;
            if (tank.isEmpty() || tank.getFluid().getFluid().isSame(fluid))
                put += tank.fill(new FluidStack(fluid, amount - put), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }
        return put;
    }

    @Override
    public int sendEnergy(Level level, BlockPos pos, Direction side, int amount) {
        if (amount <= 0) return 0;
        IEnergyStorage e = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        return e != null && e.canReceive() ? e.receiveEnergy(amount, false) : 0;
    }

    @Override
    public int sendHeat(Level level, BlockPos pos, Direction side, int amount) {
        if (amount <= 0) return 0;
        IHeatHandler h = level.getCapability(HeatCapability.HEAT, pos, side);
        return h != null ? h.receiveHeat(amount, false) : 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Item stellarArmor(ArmorItem.Type type) {
        return new NeoForgeStellarArmorItem((Holder<ArmorMaterial>) stellarMaterial, type);
    }

    @Override
    public boolean hasEnergy(ItemStack stack) {
        return !stack.isEmpty() && stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }
}
