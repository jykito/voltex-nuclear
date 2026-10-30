package com.jykito.voltexnuclear.forge;

import com.jykito.industrialcore.api.PowerSuitArmor;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.suit.StellarSuit;
import com.jykito.voltexnuclear.suit.StellarArmor;
import com.jykito.voltexnuclear.suit.StellarPiece;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A piece of the Stellar power suit on Forge 1.20.1, as Voltex's Plasma pieces are made on this version: its own armour
 * material, the energy capability over the shared {@link StellarArmor}, the Voltex power suit model and its 3D icon on the
 * client, a texture of its own; it never wears. What the suit does lives in {@link StellarArmor}.
 */
final class ForgeStellarArmorItem extends ArmorItem implements PowerSuitArmor, StellarPiece {

    private static final String TEXTURE = "voltex_nuclear:textures/models/armor/stellar_suit.png";

    /** A step over Plasma's (helmet 4, chestplate 11, leggings 9, boots 4; toughness 5): drafts until the balance stage. */
    static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        // the vanilla base durability by slot (boots, leggings, chestplate, helmet) times Plasma's multiplier, as Voltex counts it
        @Override public int getDurabilityForType(Type type) { return new int[]{13, 15, 16, 11}[type.getSlot().getIndex()] * 80; }
        @Override public int getDefenseForType(Type type) {
            return switch (type) {
                case HELMET -> 5;
                case CHESTPLATE -> 12;
                case LEGGINGS -> 10;
                case BOOTS -> 5;
            };
        }
        @Override public int getEnchantmentValue() { return 25; }
        @Override public @NotNull SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
        @Override public @NotNull Ingredient getRepairIngredient() { return Ingredient.of(NuclearItems.STAR_ALLOY.get()); }
        @Override public @NotNull String getName() { return "voltex_nuclear:stellar"; }
        @Override public float getToughness() { return 6f; }
        @Override public float getKnockbackResistance() { return 0.5f; }
    };

    ForgeStellarArmorItem(Type type) {
        super(MATERIAL, type, new Properties());
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    /** One texture for every piece of the power suit model; Voltex asks with no entity and no layer for the icon. */
    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return TEXTURE;
    }

    /** The Voltex power suit model and its 3D icon; the model classes are client only and load only from here, on the client. */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity, ItemStack stack,
                                                                                             EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                return com.jykito.industrialcore.client.armor.PowerSuitModel.forSlot(slot);
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.jykito.industrialcore.client.armor.ArmorIconRenderer.get();
            }
        });
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new Energy(stack));

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        StellarArmor.tooltip(stack, lines);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * StellarArmor.energy(stack) / StellarSuit.CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return com.jykito.industrialcore.ModStyle.ENERGY;
    }

    /** A piece's energy for the game: it takes and gives as fast as asked. */
    private record Energy(ItemStack stack) implements IEnergyStorage {
        @Override public int receiveEnergy(int max, boolean simulate) { return StellarArmor.receive(stack, max, simulate); }
        @Override public int extractEnergy(int max, boolean simulate) { return StellarArmor.extract(stack, max, simulate); }
        @Override public int getEnergyStored() { return StellarArmor.energy(stack); }
        @Override public int getMaxEnergyStored() { return StellarSuit.CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }
}
