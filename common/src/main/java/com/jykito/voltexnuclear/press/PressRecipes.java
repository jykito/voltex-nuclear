package com.jykito.voltexnuclear.press;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.content.NuclearItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the pellet press makes (DESIGN steps 2, 13, 20 and 22): enriched uranium pressed into pellets and loaded into an
 * empty Voltex rod gives a fuel rod of the same enrichment, MOX fuel a MOX rod; boron carbide pressed the same way gives a
 * control rod, as real control rods are carbide pellets in a cladding; Voltex lithium loaded into an empty cell gives the
 * lithium blank of the irradiation channel (the author chose variant V on 2026-10-27), depleted uranium the isotope blank
 * of the third power suit's pellet (variant Z-A on 2026-10-29); graphite, gold, plutonium and dried kelp the blanks of the
 * restructor's isotopes (step 15, set I-A). The press and its JEI category read the same list. The empty rod, the lithium
 * and the graphite rod are Voltex items, found by their ids.
 */
public final class PressRecipes {

    private PressRecipes() {}

    /** One kind of fuel (or boron carbide, or lithium), the shell it is loaded into and what comes out. */
    public record Recipe(Item fuel, Item shell, Item product) {}

    private static List<Recipe> all;

    /** Made on first use, once the registries are filled. */
    public static List<Recipe> all() {
        if (all == null) {
            Item emptyRod = VoltexNuclear.voltexItem("empty_fuel_rod");   // tin plates and glass fibre at the workbench, epoch 2
            all = List.of(
                    new Recipe(NuclearItems.LOW_ENRICHED_URANIUM.get(), emptyRod, NuclearItems.LOW_ENRICHED_FUEL_ROD.get()),
                    new Recipe(NuclearItems.HIGH_ENRICHED_URANIUM.get(), emptyRod, NuclearItems.HIGH_ENRICHED_FUEL_ROD.get()),
                    new Recipe(NuclearItems.MOX_FUEL.get(), emptyRod, NuclearItems.MOX_FUEL_ROD.get()),
                    new Recipe(NuclearItems.BORON_CARBIDE.get(), emptyRod, NuclearItems.CONTROL_ROD.get()),
                    new Recipe(VoltexNuclear.voltexItem("lithium"), NuclearItems.EMPTY_CELL.get(), NuclearItems.LITHIUM_BLANK.get()),
                    new Recipe(NuclearItems.DEPLETED_URANIUM.get(), NuclearItems.EMPTY_CELL.get(), NuclearItems.ISOTOPE_BLANK.get()),
                    new Recipe(VoltexNuclear.voltexItem("graphite_rod"), NuclearItems.EMPTY_CELL.get(), NuclearItems.GRAPHITE_BLANK.get()),
                    new Recipe(Items.GOLD_INGOT, NuclearItems.EMPTY_CELL.get(), NuclearItems.GOLD_BLANK.get()),
                    new Recipe(NuclearItems.PLUTONIUM.get(), NuclearItems.EMPTY_CELL.get(), NuclearItems.PLUTONIUM_BLANK.get()),
                    new Recipe(Items.DRIED_KELP_BLOCK, NuclearItems.EMPTY_CELL.get(), NuclearItems.SODIUM_BLANK.get()));
        }
        return all;
    }

    /** The recipe this fuel goes into, or null when it is not fuel the press takes. */
    @Nullable
    public static Recipe recipeFor(ItemStack fuel) {
        for (Recipe r : all()) if (fuel.is(r.fuel())) return r;
        return null;
    }

    public static boolean isFuel(ItemStack stack) {
        return recipeFor(stack) != null;
    }

    /** Whether the stack is a shell some recipe loads: the empty rod or the empty cell. */
    public static boolean isShell(ItemStack stack) {
        for (Recipe r : all()) if (stack.is(r.shell())) return true;
        return false;
    }
}
