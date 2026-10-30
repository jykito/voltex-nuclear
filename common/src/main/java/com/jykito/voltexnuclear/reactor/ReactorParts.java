package com.jykito.voltexnuclear.reactor;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.api.ReactorPartRegistry;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.reactor.Fission;
import com.jykito.voltexnuclear.core.reactor.Fuel;
import com.jykito.voltexnuclear.core.reactor.Part;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Which part of the active zone an item is (DESIGN step 7): the add-on's fuel rods and components, the Voltex items
 * that serve in the grid as they are (the graphite rod is the moderator, the heat vent the heat sink, the heat exchanger
 * the exchanger, the condensator the water cell), and a pack's parts from {@link ReactorPartRegistry}.
 */
public final class ReactorParts {

    private ReactorParts() {}

    /** Key of the wear a rod carries between reactors (DESIGN step 17). */
    private static final String WEAR = "voltex_nuclear_wear";

    private static Map<Item, Part> parts;
    /** The part items in a fixed order: the window names the ghost of a cell by its place here. */
    private static List<Item> order;
    /** A pack's rods: the cap of their fuel on the lid and what they leave burnt out; a pack's blanks: their product. */
    private static final Map<Fuel, ReactorPartRegistry.Cap> caps = new IdentityHashMap<>();
    private static final Map<Fuel, Item> spentOf = new IdentityHashMap<>();
    private static final Map<Item, Item> products = new HashMap<>();

    /** How many of the part items are the add-on's and Voltex's; a pack's come after them. */
    private static int builtIn;

    /** The part items in a fixed order (the order of {@link #all}). */
    public static List<Item> items() {
        all();
        return order;
    }

    /** The item that stands for a part in a cell (the first one, for a scheme to show and to lay), or air when none does. */
    public static Item item(Part part) {
        for (Map.Entry<Item, Part> e : all().entrySet()) if (e.getValue().equals(part)) return e.getKey();
        return Items.AIR;
    }

    /** The place of a part item in {@link #items}, or -1 when it is none. */
    public static int index(@Nullable Item item) {
        return item == null ? -1 : items().indexOf(item);
    }

    /** The wear a rod carries, in ticks at full power, or the irradiation a channel blank has gathered; 0 for a fresh one. */
    public static double wear(ItemStack stack) {
        return VoltexNuclear.platform().itemNumber(stack, WEAR);
    }

    public static void setWear(ItemStack stack, double wear) {
        VoltexNuclear.platform().setItemNumber(stack, WEAR, wear);
    }

    /** The part this item stands for in a cell, or null when it has no place in the grid. */
    @Nullable
    public static Part of(ItemStack stack) {
        return stack.isEmpty() ? null : all().get(stack.getItem());
    }

    /** What a burnt-out fuel part leaves in the output: the spent rod of its size, whatever the fuel was, or what a pack's rod names. */
    public static ItemStack spent(Part fuel) {
        if (!fuel.isFuel()) throw new IllegalArgumentException("not a fuel part: " + fuel);
        all();
        Item own = spentOf.get(fuel.fuel());
        if (own != null) return new ItemStack(own);
        return new ItemStack(switch (fuel.rods()) {
            case 1 -> NuclearItems.SPENT_FUEL_ROD.get();
            case 2 -> NuclearItems.SPENT_FUEL_ROD_DUAL.get();
            default -> NuclearItems.SPENT_FUEL_ROD_QUAD.get();
        });
    }

    /**
     * What a channel blank becomes once irradiated (DESIGN steps 14 and 20): the lithium blank a tritium cell, the isotope
     * blank the isotope pellet of the third power suit, the four blanks of step 15 the restructor's isotopes, a pack's
     * blank what it names; a blank with no product here leaves nothing.
     */
    public static ItemStack product(Item blank) {
        if (blank == NuclearItems.LITHIUM_BLANK.get()) return new ItemStack(NuclearItems.TRITIUM_CELL.get());
        if (blank == NuclearItems.ISOTOPE_BLANK.get()) return new ItemStack(NuclearItems.ISOTOPE_PELLET.get());
        if (blank == NuclearItems.GRAPHITE_BLANK.get()) return new ItemStack(NuclearItems.CARBON_14.get());
        if (blank == NuclearItems.GOLD_BLANK.get()) return new ItemStack(NuclearItems.GOLD_198.get());
        if (blank == NuclearItems.PLUTONIUM_BLANK.get()) return new ItemStack(NuclearItems.AMERICIUM_241.get());
        if (blank == NuclearItems.SODIUM_BLANK.get()) return new ItemStack(NuclearItems.SODIUM_24.get());
        all();
        Item own = products.get(blank);
        return own == null ? ItemStack.EMPTY : new ItemStack(own);
    }

    /** The look of a fuel's glowing cap on the lid: the add-on's fuels their own, a pack's fuel the one it chose. */
    public static ReactorPartRegistry.Cap cap(Fuel fuel) {
        if (fuel == Fuel.HIGH_ENRICHED) return ReactorPartRegistry.Cap.HIGH_ENRICHED;
        if (fuel == Fuel.MOX) return ReactorPartRegistry.Cap.MOX;
        all();
        return caps.getOrDefault(fuel, ReactorPartRegistry.Cap.LOW_ENRICHED);
    }

    /** Whether the item is a pack's part (from {@link ReactorPartRegistry}), not the add-on's or a Voltex one. */
    public static boolean fromPack(Item item) {
        return index(item) >= builtIn;
    }

    /** Made on first use, once the registries are filled. */
    private static Map<Item, Part> all() {
        if (parts != null) return parts;
        Map<Item, Part> m = new LinkedHashMap<>();
        m.put(NuclearItems.LOW_ENRICHED_FUEL_ROD.get(), Part.fuel(Fuel.LOW_ENRICHED, 1));
        m.put(NuclearItems.LOW_ENRICHED_FUEL_ROD_DUAL.get(), Part.fuel(Fuel.LOW_ENRICHED, 2));
        m.put(NuclearItems.LOW_ENRICHED_FUEL_ROD_QUAD.get(), Part.fuel(Fuel.LOW_ENRICHED, 4));
        m.put(NuclearItems.HIGH_ENRICHED_FUEL_ROD.get(), Part.fuel(Fuel.HIGH_ENRICHED, 1));
        m.put(NuclearItems.HIGH_ENRICHED_FUEL_ROD_DUAL.get(), Part.fuel(Fuel.HIGH_ENRICHED, 2));
        m.put(NuclearItems.HIGH_ENRICHED_FUEL_ROD_QUAD.get(), Part.fuel(Fuel.HIGH_ENRICHED, 4));
        m.put(NuclearItems.MOX_FUEL_ROD.get(), Part.fuel(Fuel.MOX, 1));
        m.put(NuclearItems.MOX_FUEL_ROD_DUAL.get(), Part.fuel(Fuel.MOX, 2));
        m.put(NuclearItems.MOX_FUEL_ROD_QUAD.get(), Part.fuel(Fuel.MOX, 4));
        m.put(NuclearItems.HEAVY_WATER_MODERATOR.get(), Part.HEAVY_WATER);
        m.put(NuclearItems.BERYLLIUM_REFLECTOR.get(), Part.REFLECTOR);
        m.put(NuclearItems.BORON_ABSORBER.get(), Part.ABSORBER);
        m.put(NuclearItems.CONTROL_ROD.get(), Part.CONTROL_ROD);
        m.put(NuclearItems.LITHIUM_BLANK.get(), Part.CHANNEL);
        m.put(NuclearItems.CRYO_CELL.get(), Part.CRYO_CELL);
        voltex(m, "graphite_rod", Part.GRAPHITE);
        voltex(m, "heat_vent", Part.HEAT_SINK);
        voltex(m, "heat_exchanger", Part.HEAT_EXCHANGER);
        voltex(m, "condensator", Part.WATER_CELL);
        // last, after the older parts: the client learns the parts by their place
        m.put(NuclearItems.ISOTOPE_BLANK.get(), Part.channel(Fission.ISOTOPE_WORK));
        for (Supplier<Item> blank : List.of(NuclearItems.GRAPHITE_BLANK, NuclearItems.GOLD_BLANK, NuclearItems.PLUTONIUM_BLANK, NuclearItems.SODIUM_BLANK))
            m.put(blank.get(), Part.channel(Fission.CATALYST_WORK));
        builtIn = m.size();
        // then a pack's parts, in the order they were registered
        for (ReactorPartRegistry.Entry e : ReactorPartRegistry.read()) {
            Item item = item(e.item());
            if (item == null || m.containsKey(item)) {
                VoltexNuclear.LOGGER.warn("Reactor part {} left out: {}", e.item(), item == null ? "no such item" : "it is a part already");
                continue;
            }
            Item leaves = e.leaves() == null ? null : item(e.leaves());
            if (e.leaves() != null && leaves == null) {
                VoltexNuclear.LOGGER.warn("Reactor part {} left out: no such item {}", e.item(), e.leaves());
                continue;
            }
            m.put(item, e.part());
            if (e.part().isFuel()) {
                caps.put(e.part().fuel(), e.cap());
                if (leaves != null) spentOf.put(e.part().fuel(), leaves);
            } else if (leaves != null) products.put(item, leaves);
        }
        order = List.copyOf(m.keySet());
        return parts = m;
    }

    /** The item of an id, or null when there is none. */
    private static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    private static void voltex(Map<Item, Part> m, String name, Part part) {
        m.put(VoltexNuclear.voltexItem(name), part);
    }
}
