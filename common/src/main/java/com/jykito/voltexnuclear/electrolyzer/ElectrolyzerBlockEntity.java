package com.jykito.voltexnuclear.electrolyzer;

import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearFluids;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.electrolyzer.Electrolyzer;
import com.jykito.voltexnuclear.machine.FluidIn;
import com.jykito.voltexnuclear.machine.MachineEntity;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The electrolyzer at work (DESIGN step 13): heavy water and an empty cell make a deuterium cell. Two working slots, the
 * empty cell and the deuterium cell that comes out, four slots for the Voltex upgrades, and a tank of heavy water that
 * pipes fill from any side (it takes heavy water only and gives nothing back). A cycle takes an empty cell and
 * {@link Electrolyzer#HEAVY_WATER_PER_CELL} mB; the cycle itself, the energy, the face light and the saving are the
 * shared {@link MachineEntity}'s. The numbers are in {@link Electrolyzer}.
 * <p>
 * Upgrades: the set of the press (registered through {@code VoltexUpgrades}): accelerators, a parallel module fills up to
 * its count of cells at once from what the slot and the tank hold, a puller takes empty cells from its side, a pusher
 * sends the deuterium cells out to its side.
 * <p>
 * Pipes and hoppers: empty cells go in from the top and the sides, deuterium cells come out from the bottom and the
 * sides; the upgrade slots are not open to them. Energy comes in from any side.
 */
public class ElectrolyzerBlockEntity extends MachineEntity {

    public static final int CELL = 0, OUT = 1, UPG_FIRST = 2, UPG_LAST = 5, SLOTS = 6;
    /** The heavy water in the tank, after the shared window numbers. */
    public static final int D_TANK = D_BASE, D_COUNT = D_BASE + 1;
    private static final int[] TOP = {CELL}, BOTTOM = {OUT}, SIDES = {CELL, OUT};

    private final FluidIn tank = new FluidIn(() -> NuclearFluids.HEAVY_WATER.get(), Electrolyzer.TANK, this::setChanged);

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.ELECTROLYZER.get(), pos, state, SLOTS, Electrolyzer.BUFFER, Electrolyzer.WORK_TICKS, Electrolyzer.FE_PER_TICK);
    }

    @Override
    public FluidIn fluid() {
        return tank;
    }

    @Override
    protected void serverTick() {
        Platform platform = VoltexNuclear.platform();
        MachineUpgrades upg = platform.upgrades(this, UPG_FIRST, UPG_LAST);
        begin(upg);
        if (upg.itemPull() != null) platform.pullItems(level, worldPosition, upg.itemPull(), this, CELL);

        Item cell = NuclearItems.DEUTERIUM_CELL.get();
        boolean ready = items.get(CELL).is(NuclearItems.EMPTY_CELL.get()) && tank.amount() >= Electrolyzer.HEAVY_WATER_PER_CELL && room(OUT, cell) > 0;
        boolean worked = cycle(ready, () -> {
            int n = Math.min(Math.min(upg.parallel(), room(OUT, cell)),
                    Math.min(items.get(CELL).getCount(), tank.amount() / Electrolyzer.HEAVY_WATER_PER_CELL));
            items.get(CELL).shrink(n);
            tank.use(n * Electrolyzer.HEAVY_WATER_PER_CELL);
            put(OUT, cell, n);
        });

        if (upg.itemPush() != null) platform.pushItems(level, worldPosition, upg.itemPush(), this, OUT);
        light(worked);
    }

    @Override
    protected int extraData(int i) {
        return i == D_TANK ? tank.amount() : 0;
    }

    @Override
    protected int extraDataCount() {
        return D_COUNT - D_BASE;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The slots: the window and every pipe see the same ones
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case CELL -> stack.is(NuclearItems.EMPTY_CELL.get());
            case OUT -> false;
            default -> takesUpgrade(stack);
        };
    }

    /** Whether a Voltex upgrade works in the electrolyzer, as its tooltip says ({@code VoltexUpgrades} registration). */
    public static boolean takesUpgrade(ItemStack stack) {
        return UpgradeTargets.works(NuclearBlocks.ELECTROLYZER.get(), stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUT;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ElectrolyzerMenu(id, inv, this, data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Saving: the heavy water beside the slots, the energy and the progress
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    protected void write(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.write(tag, registries);
        tag.putInt("HeavyWater", tank.amount());
    }

    @Override
    protected void read(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.read(tag, registries);
        tank.set(tag.getInt("HeavyWater"));
    }
}
