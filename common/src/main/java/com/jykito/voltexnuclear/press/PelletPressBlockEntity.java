package com.jykito.voltexnuclear.press;

import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearBlockEntities;
import com.jykito.voltexnuclear.content.NuclearBlocks;
import com.jykito.voltexnuclear.core.press.PelletPress;
import com.jykito.voltexnuclear.machine.MachineEntity;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pellet press at work. Three working slots: the enriched uranium, the empty Voltex rod, the fuel rod that comes out;
 * and four slots for the Voltex upgrades. A cycle takes one of each input and puts out one rod; the cycle itself, the
 * energy, the face light and the saving are the shared {@link MachineEntity}'s. The numbers are in {@link PelletPress}.
 * <p>
 * Upgrades: the set of the Voltex rod fabricator, registered through {@code VoltexUpgrades} so the upgrade tooltips name
 * the press. Accelerators shorten the work and change the energy a tick, a parallel module presses up to its count of
 * rods at once from what the slots hold, a puller takes uranium and empty rods from its side, a pusher sends the rods out
 * to its side. The rules are Voltex's own ({@code MachineUpgrades}, {@code MachineIO}).
 * <p>
 * Pipes and hoppers: the inputs go in from the top and the sides, each into its own slot by what it is; the rods come out
 * from the bottom and the sides; the upgrade slots are not open to them. Energy comes in from any side.
 */
public class PelletPressBlockEntity extends MachineEntity {

    public static final int FUEL = 0, ROD = 1, OUT = 2, UPG_FIRST = 3, UPG_LAST = 6, SLOTS = 7;
    public static final int D_COUNT = D_BASE;
    private static final int[] TOP = {FUEL, ROD}, BOTTOM = {OUT}, SIDES = {FUEL, ROD, OUT};

    public PelletPressBlockEntity(BlockPos pos, BlockState state) {
        super(NuclearBlockEntities.PELLET_PRESS.get(), pos, state, SLOTS, PelletPress.BUFFER, PelletPress.WORK_TICKS, PelletPress.FE_PER_TICK);
    }

    @Override
    protected void serverTick() {
        Platform platform = VoltexNuclear.platform();
        MachineUpgrades upg = platform.upgrades(this, UPG_FIRST, UPG_LAST);
        begin(upg);
        if (upg.itemPull() != null) platform.pullItems(level, worldPosition, upg.itemPull(), this, FUEL, ROD);

        PressRecipes.Recipe recipe = PressRecipes.recipeFor(items.get(FUEL));
        Item rod = recipe == null ? null : recipe.product();
        boolean ready = rod != null && items.get(ROD).is(recipe.shell()) && room(OUT, rod) > 0;
        boolean pressed = cycle(ready, () -> {
            int n = Math.min(Math.min(upg.parallel(), room(OUT, rod)), Math.min(items.get(FUEL).getCount(), items.get(ROD).getCount()));
            items.get(FUEL).shrink(n);
            items.get(ROD).shrink(n);
            put(OUT, rod, n);
        });

        if (upg.itemPush() != null) platform.pushItems(level, worldPosition, upg.itemPush(), this, OUT);
        light(pressed);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // The slots: the window and every pipe see the same ones
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL -> PressRecipes.isFuel(stack);
            case ROD -> PressRecipes.isShell(stack);
            case OUT -> false;
            default -> takesUpgrade(stack);
        };
    }

    /** Whether a Voltex upgrade works in the press, as its tooltip says ({@code VoltexUpgrades} registration). */
    public static boolean takesUpgrade(ItemStack stack) {
        return UpgradeTargets.works(NuclearBlocks.PELLET_PRESS.get(), stack);
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
        return new PelletPressMenu(id, inv, this, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
