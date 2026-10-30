package com.jykito.voltexnuclear.machine;

import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.voltexnuclear.VoltexNuclear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What the one-block machines of the add-on share (the pellet press, the electrolyzer): their slots, which the window
 * and every pipe see alike, the energy buffer, the work cycle with the Voltex upgrades, the face that keeps running a
 * little after the last worked tick, the numbers the window reads, and the saving. A machine adds its tick
 * ({@link #serverTick}: what it needs for a cycle and what one makes), which slots take what, and its window.
 * <p>
 * The cycle: every tick with a full set of inputs and room in the output it spends its energy for the tick and adds to
 * the progress; at the end it makes its output. Without the energy for a tick it waits and keeps its progress; without
 * inputs or room the progress drops to zero. Accelerators shorten the work and change the energy a tick.
 * <p>
 * Saving has a different method on each version: {@code saveAdditional(tag)} and {@code load(tag)} on Forge 1.20.1,
 * {@code saveAdditional(tag, registries)} and {@code loadAdditional(tag, registries)} on NeoForge 1.21.1. The class
 * declares both; each version overrides its own.
 */
public abstract class MachineEntity extends BlockEntity implements Machine, MenuProvider, WindowNumbers {

    /** Window numbers: progress and work in ticks, energy in two cells (16 bits each), whether it works, FE a tick; a machine's own follow. */
    public static final int D_PROGRESS = 0, D_WORK = 1, D_ENERGY = 2, D_WORKING = 4, D_RATE = 5, D_BASE = 6;
    /** Ticks the face keeps running after the last worked tick, so it does not blink between items. */
    private static final int LIGHT_HOLD = 10;

    protected final NonNullList<ItemStack> items;
    protected final EnergyIn energy;
    private final int baseTicks, baseRate;
    /** The progress; the work and the energy a tick with the upgrades in the slots, as of the last tick. */
    protected int progress, work, rate;
    /**
     * A cycle ended on the last tick. Its progress was left at the full work for that tick, so the window shows the sign
     * to its end even when the accelerators make the cycle a few ticks short; it starts again from nothing now.
     */
    private boolean done;
    private int light;

    @Override
    public ContainerData windowNumbers() {
        return data;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case D_PROGRESS -> progress;
                case D_WORK -> work;
                case D_ENERGY -> energy.stored() & 0xFFFF;
                case D_ENERGY + 1 -> energy.stored() >>> 16;
                case D_WORKING -> lit() ? 1 : 0;
                case D_RATE -> rate;
                default -> extraData(i);
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return D_BASE + extraDataCount();
        }
    };

    /** {@code ticks} and {@code fePerTick} are a cycle's without upgrades. */
    protected MachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int buffer, int ticks, int fePerTick) {
        super(type, pos, state);
        items = NonNullList.withSize(slots, ItemStack.EMPTY);
        energy = new EnergyIn(buffer, this::setChanged);
        baseTicks = work = ticks;
        baseRate = rate = fePerTick;
    }

    @Override
    public EnergyIn energy() {
        return energy;
    }

    /** A tick on the server; the block's ticker calls it. */
    protected abstract void serverTick();

    /** The machine's own window numbers from {@link #D_BASE} on, and how many there are. */
    protected int extraData(int i) {
        return 0;
    }

    protected int extraDataCount() {
        return 0;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The work cycle
    // ------------------------------------------------------------------------------------------------------------------

    /** The start of a tick of work: the work and the energy a tick by the upgrades; a cycle ended on the last tick starts again. */
    protected void begin(MachineUpgrades upg) {
        work = upg.scaleTime(baseTicks);
        rate = upg.scaleEnergy(baseRate);
        if (done) {
            progress = 0;
            done = false;
        }
    }

    /** A tick of the cycle: {@code finish} makes the output at its end. Returns whether the machine worked this tick. */
    protected boolean cycle(boolean ready, Runnable finish) {
        if (!ready) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return false;
        }
        if (!energy.use(rate)) return false;
        if (++progress >= work) {
            progress = work;
            done = true;
            finish.run();
        }
        setChanged();
        return true;
    }

    /** The face runs while the machine works and {@link #LIGHT_HOLD} ticks after; call once a tick. */
    protected void light(boolean worked) {
        light = worked ? LIGHT_HOLD : Math.max(0, light - 1);
        BlockState state = getBlockState();
        // the state changes only when the light does, so the client is told only then
        if (level != null && state.getValue(MachineBlock.LIT) != light > 0)
            level.setBlock(worldPosition, state.setValue(MachineBlock.LIT, light > 0), Block.UPDATE_CLIENTS);
    }

    protected boolean lit() {
        return light > 0;
    }

    /** How many more of this item the slot takes. */
    protected int room(int slot, Item item) {
        ItemStack out = items.get(slot);
        if (out.isEmpty()) return item.getDefaultInstance().getMaxStackSize();
        return out.is(item) ? out.getMaxStackSize() - out.getCount() : 0;
    }

    /** Puts n of the item into the slot; {@link #room} said they fit. */
    protected void put(int slot, Item item, int n) {
        ItemStack out = items.get(slot);
        if (out.isEmpty()) items.set(slot, new ItemStack(item, n));
        else out.grow(n);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The container
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving, in both spellings
    // ------------------------------------------------------------------------------------------------------------------

    /** The slots, the energy and the progress; a machine adds its own after calling this. */
    protected void write(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        VoltexNuclear.platform().saveItems(tag, items, registries);
        tag.putInt("Energy", energy.stored());
        tag.putInt("Progress", done ? 0 : progress);   // an ended cycle is not to run again on load
    }

    protected void read(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        items.clear();
        VoltexNuclear.platform().loadItems(tag, items, registries);
        energy.set(tag.getInt("Energy"));
        progress = Math.max(0, Math.min(baseTicks, tag.getInt("Progress")));
    }

    protected void saveAdditional(CompoundTag tag) {
        write(tag, null);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag, registries);
    }

    public void load(CompoundTag tag) {
        read(tag, null);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        read(tag, registries);
    }
}
