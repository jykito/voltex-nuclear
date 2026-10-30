package com.jykito.voltexnuclear.compat.jei;

import com.jykito.voltexnuclear.content.NuclearMenus;
import com.jykito.voltexnuclear.core.reactor.Part;
import com.jykito.voltexnuclear.core.reactor.Scheme;
import com.jykito.voltexnuclear.core.reactor.Zone;
import com.jykito.voltexnuclear.reactor.ReactorMenu;
import com.jykito.voltexnuclear.reactor.ReactorParts;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.B_SCHEME_CELL;
import static com.jykito.voltexnuclear.reactor.ReactorCoreBlockEntity.B_SCHEME_PART;

/**
 * The "+" of a scheme in an open reactor window (DESIGN step 24): every cell of the scheme the reactor has open remembers
 * its part as a ghost, and an empty one takes the part from the inventory. Parts the inventory lacks do not stop it: JEI
 * marks their cells red (as it marks the cells a missing chamber keeps shut), and the cells wait for the input ports with
 * their ghosts. A cell already holding a part keeps it; the window marks one that differs. The cells go to the core as
 * menu buttons, a cell and then its part, so the add-on needs no packets of its own.
 */
final class ReactorTransferHandler implements IRecipeTransferHandler<ReactorMenu, Scheme> {

    @Override
    public Class<? extends ReactorMenu> getContainerClass() {
        return ReactorMenu.class;
    }

    @Override
    public Optional<MenuType<ReactorMenu>> getMenuType() {
        return Optional.of(NuclearMenus.REACTOR.get());
    }

    @Override
    public RecipeType<Scheme> getRecipeType() {
        return SchemeCategory.TYPE;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(ReactorMenu menu, Scheme scheme, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        // what the inventory holds, less what the empty cells take
        Map<Item, Integer> have = new HashMap<>();
        for (ItemStack s : player.getInventory().items) if (!s.isEmpty()) have.merge(s.getItem(), s.getCount(), Integer::sum);
        List<Integer> missing = new ArrayList<>();
        List<int[]> lay = new ArrayList<>();   // {cell, the part's place in ReactorParts.items}
        for (int i = 0; i < Zone.SIZE * Zone.SIZE; i++) {
            int r = i / Zone.SIZE, c = i % Zone.SIZE;
            Part part = scheme.part(r, c);
            if (part == null) continue;
            if (!menu.cellOpen(r, c)) {   // the reactor lacks the chamber that opens it
                missing.add(i);
                continue;
            }
            Item item = ReactorParts.item(part);
            lay.add(new int[]{i, ReactorParts.index(item)});
            if (!menu.getSlot(i).getItem().isEmpty()) continue;
            int n = have.getOrDefault(item, 0);
            if (n > 0) have.put(item, n - 1);
            else missing.add(i);
        }
        if (doTransfer) {
            var mode = Minecraft.getInstance().gameMode;
            if (mode != null)
                for (int[] cell : lay) {
                    mode.handleInventoryButtonClick(menu.containerId, B_SCHEME_CELL + cell[0]);
                    mode.handleInventoryButtonClick(menu.containerId, B_SCHEME_PART + cell[1]);
                }
            return null;
        }
        return missing.isEmpty() ? null : new Missing(missing);
    }

    /** Parts the inventory lacks, or cells a missing chamber keeps shut: marked red, and the transfer still goes. */
    private record Missing(List<Integer> cells) implements IRecipeTransferError {

        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public List<Component> getTooltip() {
            return List.of(Component.translatable("jei.voltex_nuclear.scheme.missing"));
        }

        @Override
        public void showError(GuiGraphics g, int mouseX, int mouseY, IRecipeSlotsView slots, int recipeX, int recipeY) {
            List<IRecipeSlotView> views = slots.getSlotViews();
            g.pose().pushPose();
            g.pose().translate(recipeX, recipeY, 0);
            for (int i : cells) if (i < views.size()) views.get(i).drawHighlight(g, 0x66FF3030);
            g.pose().popPose();
        }
    }
}
