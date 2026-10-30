package com.jykito.voltexnuclear.starforge;

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

import static com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity.D_COUNT;
import static com.jykito.voltexnuclear.starforge.StarForgeCoreBlockEntity.SLOTS;

/**
 * The star forge window (DESIGN step 28, the author's O-4 "Panorama" on :8781/nuclear_forge_gui.html): under the dome
 * window a row of the kronium alloy, the palladium and the helium cell in, the star alloy and the empty cell out; the
 * player inventory.
 */
public class StarForgeMenu extends AbstractContainerMenu {

    /** Slot corners (the slot plus its 1 pixel rim), as tools/gui_gen/nuclear_textures/forge_gui.js draws the texture. */
    public static final int[][] SLOT = {{50, 97}, {70, 97}, {90, 97}, {178, 97}, {198, 97}};
    /** The player inventory under the frame: a step of 19 like the Voltex windows. */
    public static final int INV_X = 44, INV_Y = 182, INV_STEP = 19, HOTBAR_Y = INV_Y + 3 * INV_STEP + 6;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** The client side: the slots fill from the server, the numbers too. */
    public StarForgeMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SLOTS), new SimpleContainerData(D_COUNT), ContainerLevelAccess.NULL);
    }

    public StarForgeMenu(int id, Inventory inv, Container core, ContainerData data, ContainerLevelAccess access) {
        super(NuclearMenus.STAR_FORGE.get(), id);
        checkContainerSize(core, SLOTS);
        checkContainerDataCount(data, D_COUNT);
        this.data = data;
        this.access = access;
        for (int i = 0; i < SLOTS; i++) {
            int slot = i;
            addSlot(new Slot(core, i, SLOT[i][0] + 1, SLOT[i][1] + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return StarForgeCoreBlockEntity.accepts(slot, stack);
                }
            });
        }
        addDataSlots(data);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * INV_STEP, INV_Y + row * INV_STEP));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, INV_X + col * INV_STEP, HOTBAR_Y));
    }

    /** Shift-click: a slot of the forge to the inventory; its inputs strain into their slots; the rest between bag and hotbar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        int bag = SLOTS, hotbar = SLOTS + 27, end = SLOTS + 36;
        int target = -1;
        for (int i = 0; i < SLOTS; i++) if (StarForgeCoreBlockEntity.accepts(i, stack)) target = i;
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
        return stillValid(access, player, NuclearBlocks.STAR_FORGE_CORE.get());
    }

    public int get(int index) {
        return data.get(index) & 0xFFFF;
    }

    /** A number sent in two cells. */
    public int getWide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) << 16);
    }
}
