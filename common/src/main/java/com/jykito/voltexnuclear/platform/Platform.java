package com.jykito.voltexnuclear.platform;

import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * What differs between Forge 1.20.1 and NeoForge 1.21.1, behind one interface.
 * Shared code calls these methods; each loader module implements them. A method goes here only when a shared class
 * needs something the two versions spell differently.
 */
public interface Platform {

    /** Loader and game version for logs, for example "Forge 1.20.1". */
    String describe();

    /** A resource location: the constructor on 1.20.1, {@code ResourceLocation.fromNamespaceAndPath} on 1.21.1. */
    ResourceLocation id(String namespace, String path);

    /**
     * Registers an entry of a game registry (blocks, items, creative tabs, ...) under the add-on namespace: a deferred
     * register on either loader, hooked to the mod event bus. The supplier gives the entry once the registry is filled.
     * The entry may be a narrower type than the registry's, as a block entity type of one block entity class is in the
     * registry of all block entity types.
     */
    <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory);

    /**
     * Registers a fluid the way Voltex has its own: a fluid type {@code <name>_fluid}, the source {@code <name>} and the
     * flowing {@code <name>_flowing}, its still and flowing textures in the tint (ARGB), no bucket and no block (fluids go
     * in Voltex capsules, which take the tint). The type and its client side are classes of their own on each loader.
     * Returns the source.
     */
    Supplier<Fluid> fluid(String name, int tint, ResourceLocation still, ResourceLocation flow);

    /**
     * Saves stacks under "Items" the vanilla way ({@code ContainerHelper.saveAllItems}): 1.20.1 writes a stack on its own,
     * 1.21.1 needs the registries for the components. A block entity passes the registries it got, or null on 1.20.1.
     */
    void saveItems(CompoundTag tag, NonNullList<ItemStack> items, @Nullable HolderLookup.Provider registries);

    /** Reads stacks saved by {@link #saveItems}. */
    void loadItems(CompoundTag tag, NonNullList<ItemStack> items, @Nullable HolderLookup.Provider registries);

    /**
     * What the Voltex upgrades in slots {@code first..last} of a machine give ({@code MachineUpgrades.scan}). Voltex reads
     * them through the loader's item handler, which is a class of its own on each loader.
     */
    MachineUpgrades upgrades(Container machine, int first, int last);

    /** One stack from the neighbour on that side into the first of {@code targets} it fits ({@code MachineIO.pullItems}). */
    boolean pullItems(Level level, BlockPos pos, Direction side, Container into, int... targets);

    /** The contents of {@code slots} out to the neighbour on that side, as much as it takes ({@code MachineIO.pushItems}). */
    boolean pushItems(Level level, BlockPos pos, Direction side, Container from, int... slots);

    /**
     * A number the add-on keeps in an item (the wear of a rod): the stack's tag on 1.20.1, its custom data component on
     * 1.21.1. 0 when there is none.
     */
    double itemNumber(ItemStack stack, String key);

    /** Keeps a number in the item; 0 takes it out, so the item stacks with fresh ones again. */
    void setItemNumber(ItemStack stack, String key, double value);

    /**
     * Takes up to {@code max} mB of the fluid out of the tanks of a Voltex port, straight from the tanks: the port's own
     * fluid handler gives nothing out of an input port. Returns what it took.
     */
    int drainPort(ItemFluidPortBlockEntity port, Fluid fluid, int max);

    /** Pours up to {@code amount} mB of the fluid into the tanks of a Voltex port; returns what went in, or would. */
    int fillPort(ItemFluidPortBlockEntity port, Fluid fluid, int amount, boolean simulate);

    /** Gives up to {@code amount} FE to the energy storage at {@code pos}, seen from {@code side}; returns what it took. */
    int sendEnergy(Level level, BlockPos pos, Direction side, int amount);

    /** Gives up to {@code amount} heat to the Voltex heat handler at {@code pos}, seen from {@code side}; returns what it took. */
    int sendHeat(Level level, BlockPos pos, Direction side, int amount);

    /**
     * A piece of the Stellar power suit ({@link com.jykito.voltexnuclear.suit.StellarArmor}): an armour item of this version
     * on the Voltex power suit model, with its own texture, energy capability and material, that never wears.
     */
    Item stellarArmor(ArmorItem.Type type);

    /** Whether an item stack holds energy the way the game's energy capability sees it. */
    boolean hasEnergy(ItemStack stack);
}
