package com.jykito.voltexnuclear.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The window of a one-block machine of the add-on (the pellet press, the electrolyzer), as their textures lay it out
 * (tools/gui_gen/nuclear_textures/*_gui.js): the machine's working slots, the column of four upgrade slots on the right,
 * the player inventory under the frame. Shift-click sends the machine's slots to the inventory, and a stack from the
 * inventory to the slots the machine names for it, or between bag and hotbar.
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    /** The upgrade column, cells touching as on the Voltex machine windows. */
    public static final int UPG_X = 187, UPG_Y = 17, UPG_STEP = 18;
    /** The player inventory under the frame: a step of 19 like the Voltex windows. */
    public static final int INV_X = 44, INV_Y = 108, STEP = 19, HOTBAR_Y = INV_Y + 3 * STEP + 6;

    private final int machineSlots;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Supplier<Block> block;

    protected MachineMenu(MenuType<?> type, int id, Container machine, int slots, ContainerData data, int dataCount,
                          ContainerLevelAccess access, Supplier<Block> block) {
        super(type, id);
        checkContainerSize(machine, slots);
        checkContainerDataCount(data, dataCount);
        this.machineSlots = slots;
        this.data = data;
        this.access = access;
        this.block = block;
    }

    /** A working slot at its cell's corner plus the rim (x + 1, y + 1 of the texture's cell), taking what the test lets in. */
    protected void slot(Container machine, int index, int x, int y, Predicate<ItemStack> takes) {
        addSlot(new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return takes.test(stack);
            }
        });
    }

    /** The upgrade column and the player inventory; called after the working slots, so the indices stay in order. */
    protected void upgradesAndInventory(Container machine, int first, int last, Predicate<ItemStack> upgrade, Inventory inv) {
        for (int s = first; s <= last; s++) slot(machine, s, UPG_X, UPG_Y + (s - first) * UPG_STEP, upgrade);
        addDataSlots(data);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * STEP, INV_Y + row * STEP));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, INV_X + col * STEP, HOTBAR_Y));
    }

    /** The machine slots {from, to (exclusive)} a stack from the inventory goes to on a shift-click, or null for none. */
    @Nullable
    protected abstract int[] target(ItemStack stack);

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        int bag = machineSlots, hotbar = machineSlots + 27, end = machineSlots + 36;
        int[] to = index < machineSlots ? null : target(stack);
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, bag, end, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, before);
        } else if (to != null) {
            if (!moveItemStackTo(stack, to[0], to[1], false)) return ItemStack.EMPTY;
        } else if (index < hotbar) {
            if (!moveItemStackTo(stack, hotbar, end, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, bag, hotbar, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block.get());
    }

    public int get(int index) {
        return data.get(index);
    }

    /** A number sent in two cells. */
    public int getWide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) << 16);
    }
}
