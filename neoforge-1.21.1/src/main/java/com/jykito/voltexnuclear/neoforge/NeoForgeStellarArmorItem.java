package com.jykito.voltexnuclear.neoforge;

import com.jykito.industrialcore.api.PowerSuitArmor;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.suit.StellarSuit;
import com.jykito.voltexnuclear.suit.StellarArmor;
import com.jykito.voltexnuclear.suit.StellarPiece;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * A piece of the Stellar power suit on NeoForge 1.21.1, as Voltex's Plasma pieces are made on this version: its own armour
 * material in the game's registry, a texture of its own; it never wears. The energy capability and the client hooks (the
 * Voltex power suit model and its 3D icon) are registered by events here, as Voltex registers them for its own items only.
 * What the suit does lives in {@link StellarArmor}.
 */
final class NeoForgeStellarArmorItem extends ArmorItem implements PowerSuitArmor, StellarPiece {

    private static final ResourceLocation TEXTURE = VoltexNuclear.id("textures/models/armor/stellar_suit.png");

    /** A step over Plasma's (helmet 4, chestplate 11, leggings 9, boots 4; toughness 5): drafts until the balance stage. */
    static ArmorMaterial material() {
        return new ArmorMaterial(Util.make(new EnumMap<>(Type.class), m -> {
            m.put(Type.HELMET, 5);
            m.put(Type.CHESTPLATE, 12);
            m.put(Type.LEGGINGS, 10);
            m.put(Type.BOOTS, 5);
            m.put(Type.BODY, 12);
        }), 25, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(NuclearItems.STAR_ALLOY.get()),
                List.of(new ArmorMaterial.Layer(VoltexNuclear.id("stellar"))), 6f, 0.5f);
    }

    NeoForgeStellarArmorItem(Holder<ArmorMaterial> material, Type type) {
        super(material, type, new Properties().durability(type.getDurability(80)));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /** One texture for every piece of the power suit model; Voltex asks with no entity and no layer for the icon. */
    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
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
    record Energy(ItemStack stack) implements IEnergyStorage {
        @Override public int receiveEnergy(int max, boolean simulate) { return StellarArmor.receive(stack, max, simulate); }
        @Override public int extractEnergy(int max, boolean simulate) { return StellarArmor.extract(stack, max, simulate); }
        @Override public int getEnergyStored() { return StellarArmor.energy(stack); }
        @Override public int getMaxEnergyStored() { return StellarSuit.CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }
}
