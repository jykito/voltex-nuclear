package com.jykito.voltexnuclear.press;

import com.jykito.voltexnuclear.content.NuclearBlocks;
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

import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.D_COUNT;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.FUEL;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.OUT;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.ROD;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.SLOTS;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.UPG_FIRST;
import static com.jykito.voltexnuclear.press.PelletPressBlockEntity.UPG_LAST;

/**
 * The pellet press window: the two inputs, the output, the column of upgrade slots and the player inventory, where the
 * window texture has them (tools/gui_gen/nuclear_textures/press_gui.js writes the same positions next to the texture).
 * Shift-click puts fuel and shells (empty rods, empty cells) into their slots, the upgrades the press takes into the
 * upgrade column.
 */
public class PelletPressMenu extends MachineMenu {

    /** Slot corners (the cell plus its 1 pixel rim). */
    public static final int FUEL_X = 66, FUEL_Y = 25, ROD_X = 66, ROD_Y = 49, OUT_X = 146, OUT_Y = 37;

    /** The client side: the slots fill from the server, the numbers too. */
    public PelletPressMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SLOTS), new SimpleContainerData(D_COUNT), ContainerLevelAccess.NULL);
    }

    public PelletPressMenu(int id, Inventory inv, Container press, ContainerData data, ContainerLevelAccess access) {
        super(NuclearMenus.PELLET_PRESS.get(), id, press, SLOTS, data, D_COUNT, access, NuclearBlocks.PELLET_PRESS);
        slot(press, FUEL, FUEL_X, FUEL_Y, PressRecipes::isFuel);
        slot(press, ROD, ROD_X, ROD_Y, PressRecipes::isShell);
        slot(press, OUT, OUT_X, OUT_Y, stack -> false);
        upgradesAndInventory(press, UPG_FIRST, UPG_LAST, PelletPressBlockEntity::takesUpgrade, inv);
    }

    @Nullable
    @Override
    protected int[] target(ItemStack stack) {
        if (PressRecipes.isFuel(stack)) return new int[]{FUEL, FUEL + 1};
        if (PressRecipes.isShell(stack)) return new int[]{ROD, ROD + 1};
        if (PelletPressBlockEntity.takesUpgrade(stack)) return new int[]{UPG_FIRST, UPG_LAST + 1};
        return null;
    }
}
