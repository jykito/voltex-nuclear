package com.jykito.voltexnuclear.suit;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.suit.StellarSuit;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.jykito.voltexnuclear.core.suit.StellarSuit.*;

/**
 * What the Stellar power suit does in the world (DESIGN step 12; the numbers and the sharing in {@link StellarSuit}). Each
 * piece keeps its energy as a number on its stack. The full set flies; while it holds energy it heals a hurt wearer (the
 * Halo), keeps fire and lava off, paying for every tick in lava (the star's heat), flies twice as fast in a sprint (the
 * rush) and stops at once in flight when no key is held (the damping, on the client in StellarFlight). The chestplate takes an isotope pellet out of the inventory when the suit runs under a quarter and burns it an
 * hour into the pieces, pausing while they are full. The damage the suit turns away costs energy, a quarter for each
 * charged piece. The versions call in from their armour items, their energy capabilities and their game events.
 */
public final class StellarArmor {

    private StellarArmor() {}

    /** Numbers on the stacks: a piece's energy, the ticks of the pellet burning in the chestplate. */
    public static final String ENERGY = "Energy", PELLET = "PelletTicks";
    /** Flags on the wearer: the suit gave the flight, the suit sped it up. */
    private static final String FLIGHT = "voltex_nuclear.stellar_flight", RUSH = "voltex_nuclear.stellar_rush";
    /** The game's own flying speed. */
    private static final float FLY_SPEED = 0.05f;
    /** The pellet feeds the suit and the damping is paid once a second, the second's worth at once, so the stacks change less often. */
    private static final int SECOND = 20;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof StellarPiece;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // A piece's energy, for the versions' energy capabilities
    // ------------------------------------------------------------------------------------------------------------------

    public static int energy(ItemStack stack) {
        return (int) VoltexNuclear.platform().itemNumber(stack, ENERGY);
    }

    private static void setEnergy(ItemStack stack, int energy) {
        VoltexNuclear.platform().setItemNumber(stack, ENERGY, Math.max(0, Math.min(CAPACITY, energy)));
    }

    public static int receive(ItemStack stack, int max, boolean simulate) {
        int stored = energy(stack), took = Math.max(0, Math.min(max, CAPACITY - stored));
        if (!simulate && took > 0) setEnergy(stack, stored + took);
        return took;
    }

    public static int extract(ItemStack stack, int max, boolean simulate) {
        int stored = energy(stack), gave = Math.max(0, Math.min(max, stored));
        if (!simulate && gave > 0) setEnergy(stack, stored - gave);
        return gave;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The suit on its wearer, once a tick on the server
    // ------------------------------------------------------------------------------------------------------------------

    /** The pieces the player wears. */
    public static List<ItemStack> worn(Player player) {
        List<ItemStack> out = new ArrayList<>(PIECES);
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = player.getItemBySlot(slot);
            if (is(s)) out.add(s);
        }
        return out;
    }

    private static int[] stored(List<ItemStack> pieces) {
        int[] out = new int[pieces.size()];
        for (int i = 0; i < out.length; i++) out[i] = energy(pieces.get(i));
        return out;
    }

    /** Takes {@code amount} from the pieces, evenly as far as each has, if they hold it together. */
    private static boolean pay(List<ItemStack> pieces, int amount) {
        int[] stored = stored(pieces);
        if (!canPay(amount, stored)) return false;
        int[] gave = drain(amount, stored);
        for (int i = 0; i < gave.length; i++) setEnergy(pieces.get(i), stored[i] - gave[i]);
        return true;
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        List<ItemStack> worn = worn(player);
        boolean full = worn.size() == PIECES;
        feed(player, worn);
        flight(player, full);
        if (!full) {
            rush(player, false);
            return;
        }
        if (player.isInLava()) pay(worn, LAVA_PER_TICK);
        if (player.isOnFire() && holds(worn)) player.clearFire();
        if (player.getHealth() < player.getMaxHealth() && player.tickCount % HEAL_EVERY == 0 && pay(worn, FE_PER_HEAL)) player.heal(HEAL);
        rush(player, player.getAbilities().flying && player.isSprinting() && pay(worn, RUSH_PER_TICK));
        if (player.getAbilities().flying && player.tickCount % SECOND == 0) pay(worn, DAMPING_PER_TICK * SECOND);
    }

    /** Whether the suit damps the flight: the full set, flying, holding energy. Asked on the client, where the player moves. */
    public static boolean damps(Player player) {
        if (!player.getAbilities().flying) return false;
        List<ItemStack> worn = worn(player);
        return worn.size() == PIECES && holds(worn);
    }

    private static boolean holds(List<ItemStack> pieces) {
        for (ItemStack s : pieces) if (energy(s) > 0) return true;
        return false;
    }

    /** The full set flies; a flight the suit gave goes with it, unless the game gives it anyway. */
    private static void flight(Player player, boolean full) {
        CompoundTag data = player.getPersistentData();
        Abilities a = player.getAbilities();
        if (full && !a.mayfly) {
            a.mayfly = true;
            data.putBoolean(FLIGHT, true);
            player.onUpdateAbilities();
        } else if (!full && data.getBoolean(FLIGHT)) {
            data.remove(FLIGHT);
            if (!player.isCreative() && !player.isSpectator()) {
                a.mayfly = false;
                a.flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    /** The rush: twice the flying speed while it is paid for, the game's own again after. */
    private static void rush(Player player, boolean on) {
        CompoundTag data = player.getPersistentData();
        if (on == data.getBoolean(RUSH)) return;
        if (on) data.putBoolean(RUSH, true);
        else data.remove(RUSH);
        player.getAbilities().setFlyingSpeed(on ? FLY_SPEED * RUSH_SPEED : FLY_SPEED);
        player.onUpdateAbilities();
    }

    /** The pellet in the chestplate: a new one out of the inventory when the suit runs low, burning a second at a time. */
    private static void feed(Player player, List<ItemStack> worn) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!is(chest) || player.tickCount % SECOND != 0) return;
        int burn = pelletTicks(chest);
        long stored = 0;
        for (ItemStack s : worn) stored += energy(s);
        if (wantsPellet(stored, (long) CAPACITY * worn.size(), burn) && takePellet(player)) burn = PELLET_TICKS;
        if (burn <= 0) return;
        int[] room = new int[worn.size()];
        for (int i = 0; i < room.length; i++) room[i] = CAPACITY - energy(worn.get(i));
        int[] got = share(PELLET_PER_TICK * SECOND, room);
        int sum = 0;
        for (int i = 0; i < got.length; i++) {
            if (got[i] > 0) setEnergy(worn.get(i), energy(worn.get(i)) + got[i]);
            sum += got[i];
        }
        if (sum == 0 && burn < PELLET_TICKS) return;   // all full: the pellet waits
        VoltexNuclear.platform().setItemNumber(chest, PELLET, Math.max(0, burn - SECOND));
    }

    public static int pelletTicks(ItemStack chest) {
        return (int) VoltexNuclear.platform().itemNumber(chest, PELLET);
    }

    private static boolean takePellet(Player player) {
        for (ItemStack s : player.getInventory().items)
            if (s.is(NuclearItems.ISOTOPE_PELLET.get())) {
                s.shrink(1);
                return true;
            }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Damage
    // ------------------------------------------------------------------------------------------------------------------

    /** Whether the suit keeps this damage off whole: fire and lava, while the full set holds energy. */
    public static boolean keepsOff(Player player, DamageSource source) {
        if (!source.is(DamageTypeTags.IS_FIRE)) return false;
        List<ItemStack> worn = worn(player);
        return worn.size() == PIECES && holds(worn);
    }

    /** The damage left after the suit turned its share away: each piece that can pay its quarter of the cost takes a quarter. */
    public static float absorb(Player player, DamageSource source, float amount) {
        if (amount <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        List<ItemStack> worn = worn(player);
        if (worn.isEmpty()) return amount;
        int each = (int) Math.ceil(damageCost(amount) / (double) PIECES), charged = 0;
        for (ItemStack s : worn) {
            if (energy(s) < each) continue;
            setEnergy(s, energy(s) - each);
            charged++;
        }
        return amount * (1 - absorbed(charged));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Tooltip
    // ------------------------------------------------------------------------------------------------------------------

    /** The energy as Voltex's suits show it, and on the chestplate the pellet burning. */
    public static void tooltip(ItemStack stack, List<Component> lines) {
        lines.add(Component.literal(String.format("%,d / %,d FE", energy(stack), CAPACITY)).withStyle(ChatFormatting.AQUA));
        int burn = pelletTicks(stack);
        if (burn > 0) lines.add(Component.translatable("tooltip.voltex_nuclear.stellar.pellet", minutes(burn)).withStyle(ChatFormatting.GOLD));
    }

    /** Whole minutes left of a pellet, at least one while it burns. */
    public static int minutes(int ticks) {
        return Math.max(1, (ticks + 1199) / 1200);
    }
}
