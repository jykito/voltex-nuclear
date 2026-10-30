package com.jykito.voltexnuclear.electrolyzer;

import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.content.NuclearMenus;
import com.jykito.voltexnuclear.machine.MachineMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.CELL;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.D_COUNT;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.OUT;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.SLOTS;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.UPG_FIRST;
import static com.jykito.voltexnuclear.electrolyzer.ElectrolyzerBlockEntity.UPG_LAST;

/**
 * The electrolyzer window: the empty cell under the sign, the deuterium cell at its end, the column of upgrade slots and
 * the player inventory, where the window texture has them (tools/gui_gen/nuclear_textures/electrolyzer_gui.js writes the
 * same positions next to the texture). Shift-click puts empty cells into their slot and the upgrades into the column.
 */
public class ElectrolyzerMenu extends MachineMenu {

    /** Slot corners (the cell plus its 1 pixel rim). */
    public static final int CELL_X = 83, CELL_Y = 52, OUT_X = 151, OUT_Y = 38;

    /** The client side: the slots fill from the server, the numbers too. */
    public ElectrolyzerMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SLOTS), new SimpleContainerData(D_COUNT), ContainerLevelAccess.NULL);
    }

    public ElectrolyzerMenu(int id, Inventory inv, Container machine, ContainerData data, ContainerLevelAccess access) {
        super(NuclearMenus.ELECTROLYZER.get(), id, machine, SLOTS, data, D_COUNT, access, NuclearBlocks.ELECTROLYZER);
        slot(machine, CELL, CELL_X, CELL_Y, stack -> stack.is(NuclearItems.EMPTY_CELL.get()));
        slot(machine, OUT, OUT_X, OUT_Y, stack -> false);
        upgradesAndInventory(machine, UPG_FIRST, UPG_LAST, ElectrolyzerBlockEntity::takesUpgrade, inv);
    }

    @Nullable
    @Override
    protected int[] target(ItemStack stack) {
        if (stack.is(NuclearItems.EMPTY_CELL.get())) return new int[]{CELL, CELL + 1};
        if (ElectrolyzerBlockEntity.takesUpgrade(stack)) return new int[]{UPG_FIRST, UPG_LAST + 1};
        return null;
    }
}
