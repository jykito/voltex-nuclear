package com.jykito.voltexnuclear.cascade;

import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearMenus;
import com.jykito.voltexnuclear.core.cascade.Cascade.Stage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static com.jykito.voltexnuclear.cascade.CascadeCoreBlockEntity.*;

/**
 * The cascade core window. It has no slots of its own: the yellowcake and the uranium live in the Voltex ports, the window
 * shows what is in them. The stage buttons go through the vanilla menu button click, the same on both versions.
 */
public class CascadeMenu extends AbstractContainerMenu {

    /** The player inventory under the frame, where the window texture has it: a step of 19 like the Voltex windows. */
    public static final int INV_X = 44, INV_Y = 182, STEP = 19, HOTBAR_Y = INV_Y + 3 * STEP + 6;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** The client side: the numbers arrive from the server. */
    public CascadeMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainerData(D_COUNT), ContainerLevelAccess.NULL);
    }

    public CascadeMenu(int id, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(NuclearMenus.CASCADE.get(), id);
        checkContainerDataCount(data, D_COUNT);
        this.data = data;
        this.access = access;
        addDataSlots(data);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * STEP, INV_Y + row * STEP));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, INV_X + col * STEP, HOTBAR_Y));
    }

    /** Button ids are stage indices. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= Stage.values().length) return false;
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof CascadeCoreBlockEntity core) core.chooseStage(Stage.values()[id]);
        });
        return true;
    }

    /** Shift-click has nowhere to go: the window holds nothing. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, NuclearBlocks.CASCADE_CORE.get());
    }

    public int get(int index) {
        return data.get(index);
    }

    /** A number sent in two cells. */
    public int getWide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) << 16);
    }
}
