package com.jykito.voltexnuclear.fusion;

import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity.D_COUNT;
import static com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity.SLOTS;
import static com.jykito.voltexnuclear.fusion.TorusCoreBlockEntity.WATCHER;

/**
 * The torus window (DESIGN step 19, mock-up :8781/nuclear_torus.html): next to the ring the cells in (deuterium and
 * tritium) and out (helium, the empty cells), in the right column the Watcher's slot; the player inventory. The buttons
 * go to the core through {@link #clickMenuButton}.
 */
public class TorusMenu extends AbstractContainerMenu {

    /** Cell corners (the cell plus its 1 pixel rim), as tools/gui_gen/nuclear_textures/torus_gui.js draws the texture. */
    public static final int[][] CELLS = {{93, 17}, {113, 17}, {93, 41}, {113, 41}, {152, 108}};
    /** The player inventory under the frame: a step of 19 like the Voltex windows. */
    public static final int INV_X = 44, INV_Y = 182, INV_STEP = 19, HOTBAR_Y = INV_Y + 3 * INV_STEP + 6;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** The client side: the slots fill from the server, the numbers too. */
    public TorusMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SLOTS), new SimpleContainerData(D_COUNT), ContainerLevelAccess.NULL);
    }

    public TorusMenu(int id, Inventory inv, Container core, ContainerData data, ContainerLevelAccess access) {
        super(NuclearMenus.TORUS.get(), id);
        checkContainerSize(core, SLOTS);
        checkContainerDataCount(data, D_COUNT);
        this.data = data;
        this.access = access;
        for (int i = 0; i < SLOTS; i++) {
            int slot = i;
            addSlot(new Slot(core, i, CELLS[i][0] + 1, CELLS[i][1] + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TorusCoreBlockEntity.accepts(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return slot == WATCHER ? 1 : super.getMaxStackSize();
                }
            });
        }
        addDataSlots(data);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * INV_STEP, INV_Y + row * INV_STEP));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, INV_X + col * INV_STEP, HOTBAR_Y));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof TorusCoreBlockEntity core) core.press(id);
        });
        return true;
    }

    /** Shift-click: a slot of the torus to the inventory; cells and a Watcher strain into their slots; the rest between bag and hotbar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        int bag = SLOTS, hotbar = SLOTS + 27, end = SLOTS + 36;
        int target = -1;
        for (int i = 0; i < SLOTS; i++) if (TorusCoreBlockEntity.accepts(i, stack)) target = i;
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, bag, end, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, before);
        } else if (target >= 0) {
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
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
        return stillValid(access, player, NuclearBlocks.TORUS_CORE.get());
    }

    public int get(int index) {
        return data.get(index) & 0xFFFF;
    }

    /** A number sent in two cells. */
    public int getWide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) << 16);
    }
}
