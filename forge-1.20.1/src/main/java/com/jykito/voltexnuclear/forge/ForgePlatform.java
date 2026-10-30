package com.jykito.voltexnuclear.forge;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.MachineIO;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** {@link Platform} on Forge 1.20.1. */
final class ForgePlatform implements Platform {

    private final IEventBus modBus;
    /** One deferred register per game registry, made on first use and hooked to the mod bus right away. */
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new HashMap<>();
    /** The textures of the add-on's fluids, tinted, as Voltex's are. */

    ForgePlatform(IEventBus modBus) {
        this.modBus = modBus;
    }

    @Override
    public String describe() {
        return "Forge 1.20.1";
    }

    @Override
    public ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
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

    @Override
    public Supplier<Fluid> fluid(String name, int tint, ResourceLocation still, ResourceLocation flow) {
        // as Voltex's ModFluids: the client side lives in the type itself on Forge
        Supplier<FluidType> type = register(ForgeRegistries.Keys.FLUID_TYPES, name + "_fluid",
                () -> new FluidType(FluidType.Properties.create().density(1100).viscosity(1000)) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override public ResourceLocation getStillTexture() { return still; }
                            @Override public ResourceLocation getFlowingTexture() { return flow; }
                            @Override public int getTintColor() { return tint; }
                        });
                    }
                });
        ForgeFlowingFluid.Properties[] properties = new ForgeFlowingFluid.Properties[1];
        Supplier<Fluid> source = register(Registries.FLUID, name, () -> new ForgeFlowingFluid.Source(properties[0]));
        Supplier<Fluid> flowing = register(Registries.FLUID, name + "_flowing", () -> new ForgeFlowingFluid.Flowing(properties[0]));
        properties[0] = new ForgeFlowingFluid.Properties(type, source, flowing).bucket(() -> Items.AIR);
        return source;
    }

    @Override
    public void saveItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void loadItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items);
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
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getDouble(key);
    }

    /** Taking the last key out drops the tag itself ({@code removeTagKey}), so the stack matches fresh ones again. */
    @Override
    public void setItemNumber(ItemStack stack, String key, double value) {
        if (value != 0) stack.getOrCreateTag().putDouble(key, value);
        else stack.removeTagKey(key);
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
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || amount <= 0) return 0;
        return be.getCapability(ForgeCapabilities.ENERGY, side).map(e -> e.canReceive() ? e.receiveEnergy(amount, false) : 0).orElse(0);
    }

    @Override
    public int sendHeat(Level level, BlockPos pos, Direction side, int amount) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || amount <= 0) return 0;
        return be.getCapability(HeatCapability.HEAT, side).map(h -> h.receiveHeat(amount, false)).orElse(0);
    }

    @Override
    public Item stellarArmor(ArmorItem.Type type) {
        return new ForgeStellarArmorItem(type);
    }

    @Override
    public boolean hasEnergy(ItemStack stack) {
        return !stack.isEmpty() && stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }
}
