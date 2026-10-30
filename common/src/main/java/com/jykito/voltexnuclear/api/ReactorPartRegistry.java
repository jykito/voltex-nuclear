package com.jykito.voltexnuclear.api;

import com.jykito.voltexnuclear.core.reactor.Cooling;
import com.jykito.voltexnuclear.core.reactor.Fuel;
import com.jykito.voltexnuclear.core.reactor.Part;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A pack's own parts for the active zone of the fission reactor (PLAN stage 12, step 6): fuel rods, heat sinks, cooling
 * cells and channel blanks, each bound to an item by its id. The reactor, its window, the lid, the schemes in JEI and the
 * irradiation channel category take them as they take the add-on's own.
 * <p>
 * Register before a world loads: from a mod's constructor or common setup, or from a KubeJS startup script
 * ({@code NuclearEvents.reactor(e => ...)}). The add-on reads the list once, on first use, after the registries are
 * filled; an item that does not exist then, or is a part already, is logged and left out. Client and server must
 * register the same parts in the same order: the client learns a cell's part by its place in the list.
 * <p>
 * The numbers are checked here and a wrong one throws, with the item named, so a script sees its mistake at startup.
 */
public final class ReactorPartRegistry {

    private ReactorPartRegistry() {}

    /** The look of a fuel's glowing cap on the reactor's lid: one of the add-on's three fuels. */
    public enum Cap { LOW_ENRICHED, HIGH_ENRICHED, MOX }

    /** A registered part: its item, what it is in the zone, and for a rod its cap and what it leaves, for a blank its product. */
    public record Entry(ResourceLocation item, Part part, @Nullable Cap cap, @Nullable ResourceLocation leaves) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean read;

    /**
     * A fuel rod: {@code strength} is its neutron source (the add-on's low enriched fuel 1, high enriched 2, MOX 1.5),
     * {@code heat} its heat factor (1, 1.4, 1.2), {@code rods} 1, 2 or 4 for a single, dual or quad assembly,
     * {@code lifeTicks} how long it lasts at full power (the add-on's rods 72 000, an hour). A burnt-out rod leaves
     * {@code spent}, or the add-on's spent rod of its size when null.
     */
    public static void fuelRod(ResourceLocation item, double strength, double heat, int rods, double lifeTicks, Cap cap,
                               @Nullable ResourceLocation spent) {
        Fuel fuel = new Fuel(item.toString(), strength, heat, lifeTicks);
        if (rods != 1 && rods != 2 && rods != 4) throw new IllegalArgumentException(item + ": rods must be 1, 2 or 4, got " + rods);
        add(new Entry(item, Part.fuel(fuel, rods), Objects.requireNonNull(cap, "cap"), spent));
    }

    /** A heat sink: takes up to {@code rate} heat a tick from the fuel next to it (the Voltex heat vent 80). */
    public static void heatSink(ResourceLocation item, double rate) {
        add(new Entry(item, Part.outlet(new Cooling(rate, null, 0, null)), null, null));
    }

    /**
     * A cooling cell: takes up to {@code rate} heat a tick while the input ports give it {@code fluid}, 1 mB per
     * {@code heatPerMb} heat, and pours as much {@code output} into the output ports, working only while it fits; with no
     * output the fluid boils away (the add-on's water cell: 150, water, 20; its cryo cell: 600, Voltex coolant, 600, hot
     * coolant).
     */
    public static void cell(ResourceLocation item, double rate, ResourceLocation fluid, double heatPerMb, @Nullable ResourceLocation output) {
        Cooling cooling = new Cooling(rate, Objects.requireNonNull(fluid, "fluid").toString(), heatPerMb, output == null ? null : output.toString());
        add(new Entry(item, Part.outlet(cooling), null, null));
    }

    /**
     * A channel blank: stands in a cell of the zone and becomes {@code product} once it has gathered {@code work} of
     * irradiation (the add-on's lithium blank 6 000, about 30 seconds in a good channel; its catalyst blanks 240 000).
     */
    public static void channelBlank(ResourceLocation item, ResourceLocation product, double work) {
        if (!(work > 0)) throw new IllegalArgumentException(item + ": work must be above 0, got " + work);
        add(new Entry(item, Part.channel(work), null, Objects.requireNonNull(product, "product")));
    }

    /** What was registered, in order; the add-on reads it once and the list closes. */
    public static List<Entry> read() {
        read = true;
        return List.copyOf(ENTRIES);
    }

    private static void add(Entry entry) {
        if (read) throw new IllegalStateException(entry.item() + ": reactor parts are read already, register them before the world loads");
        ENTRIES.add(entry);
    }
}
