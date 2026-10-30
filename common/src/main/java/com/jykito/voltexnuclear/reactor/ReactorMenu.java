package com.jykito.voltexnuclear.reactor;

import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.content.NuclearMenus;
import com.jykito.voltexnuclear.content.NuclearNanites;
import com.jykito.voltexnuclear.core.reactor.Zone;
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

import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.B_SCHEME_CELL;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.B_SCHEME_PART;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.CELLS;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.D_CHAMBERS;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.D_COUNT;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.OUT_SPENT;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.SLOTS;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.WATCHER;

/**
 * The reactor window (DESIGN step 17, mock-up :8781/nuclear_reactor.html): the 7 x 7 zone, a part a cell; under it the
 * three outputs; in the right column the Watcher's slot (step 10), one Watcher strain; the player inventory. A cell takes
 * a part of the zone while the chambers keep it open; the outputs take nothing. The buttons go to the core through
 * {@link #clickMenuButton}.
 */
public class ReactorMenu extends AbstractContainerMenu {

    /**
     * Cell corners (the cell plus its 1 pixel rim), as tools/gui_gen/nuclear_textures/reactor_gui.js draws the texture: the
     * zone from 18, 14 in steps of 18; the outputs under its last three columns.
     */
    public static final int GRID_X = 18, GRID_Y = 14, STEP = 18, OUT_X = GRID_X + 4 * STEP, OUT_Y = GRID_Y + Zone.SIZE * STEP + 4;
    /** The Watcher's cell in the right column, between the knob "share into heat" and the keys (mock-up :8781/nuclear_reactor_watcher.html). */
    public static final int WATCHER_X = 150, WATCHER_Y = 111;
    /** The player inventory under the frame: a step of 19 like the Voltex windows. */
    public static final int INV_X = 44, INV_Y = 182, INV_STEP = 19, HOTBAR_Y = INV_Y + 3 * INV_STEP + 6;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** The client side: the slots fill from the server, the numbers too. */
    public ReactorMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(SLOTS), new SimpleContainerData(D_COUNT), ContainerLevelAccess.NULL);
    }

    public ReactorMenu(int id, Inventory inv, Container core, ContainerData data, ContainerLevelAccess access) {
        super(NuclearMenus.REACTOR.get(), id);
        checkContainerSize(core, SLOTS);
        checkContainerDataCount(data, D_COUNT);
        this.data = data;
        this.access = access;
        for (int i = 0; i < CELLS; i++) {
            int r = i / Zone.SIZE, c = i % Zone.SIZE;
            addSlot(new Slot(core, i, GRID_X + 1 + c * STEP, GRID_Y + 1 + r * STEP) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return cellOpen(r, c) && ReactorParts.of(stack) != null;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return cellOpen(r, c);
                }
            });
        }
        for (int k = 0; k < 3; k++)
            addSlot(new Slot(core, OUT_SPENT + k, OUT_X + 1 + k * STEP, OUT_Y + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        addSlot(new Slot(core, WATCHER, WATCHER_X + 1, WATCHER_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return NuclearNanites.watcher(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addDataSlots(data);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * INV_STEP, INV_Y + row * INV_STEP));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, INV_X + col * INV_STEP, HOTBAR_Y));
    }

    /** Whether the chambers keep a cell of the zone open, from the number the window gets. */
    public boolean cellOpen(int row, int col) {
        int bits = data.get(D_CHAMBERS);
        boolean left = (bits & 1 << Zone.Side.LEFT.ordinal()) != 0, right = (bits & 1 << Zone.Side.RIGHT.ordinal()) != 0;
        boolean top = (bits & 1 << Zone.Side.TOP.ordinal()) != 0, bottom = (bits & 1 << Zone.Side.BOTTOM.ordinal()) != 0;
        return row >= (top ? 0 : 1) && row <= (bottom ? 6 : 5) && col >= (left ? 0 : 1) && col <= (right ? 6 : 5);
    }

    /** The cell a JEI scheme picked last; its part comes in the next button (ReactorCoreBlockEntity B_SCHEME_*). */
    private int schemeCell = -1;

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= B_SCHEME_CELL && id < B_SCHEME_PART) {
            schemeCell = id - B_SCHEME_CELL;
            return true;
        }
        int part = id - B_SCHEME_PART;
        access.execute((level, pos) -> {
            if (!(level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity core)) return;
            if (part >= 0 && part < ReactorParts.items().size()) core.schemeCell(schemeCell, ReactorParts.items().get(part), player);
            else core.press(id);
        });
        return true;
    }

    /**
     * Shift-click: a cell or an output to the inventory (a rod's wear goes into it first); a part of the zone into the
     * first open empty cell; the rest between bag and hotbar.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (slot.container instanceof ReactorCoreBlockEntity.Cells cells) cells.syncWear(slot.getContainerSlot());
        ItemStack stack = slot.getItem(), before = stack.copy();
        int bag = SLOTS, hotbar = SLOTS + 27, end = SLOTS + 36;
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, bag, end, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, before);
        } else if (NuclearNanites.watcher(stack) != null) {
            if (!moveItemStackTo(stack, WATCHER, WATCHER + 1, false)) return ItemStack.EMPTY;
        } else if (ReactorParts.of(stack) != null) {
            if (!moveItemStackTo(stack, 0, CELLS, false)) return ItemStack.EMPTY;
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
        return stillValid(access, player, NuclearBlocks.REACTOR_CORE.get());
    }

    public int get(int index) {
        return data.get(index) & 0xFFFF;
    }

    /** A number sent in two cells. */
    public int getWide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) << 16);
    }
}
