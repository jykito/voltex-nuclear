package com.jykito.voltexnuclear.compat.kubejs;

import com.jykito.voltexnuclear.api.ReactorPartRegistry;
import com.jykito.voltexnuclear.core.reactor.Fission;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * The reactor parts of a KubeJS startup script, read from the plain objects the script passes and handed to
 * {@link ReactorPartRegistry}:
 * <pre>
 * NuclearEvents.reactor(event => {
 *   event.fuelRod('kubejs:thorium_rod', {strength: 1.2, heat: 0.8, rods: 1, life: 108000, cap: 'mox', spent: 'kubejs:spent_thorium_rod'})
 *   event.heatSink('kubejs:copper_vent', {rate: 120})
 *   event.cell('kubejs:nitrogen_cell', {rate: 400, fluid: 'kubejs:liquid_nitrogen', heatPerMb: 100, output: 'kubejs:nitrogen_gas'})
 *   event.channelBlank('kubejs:cobalt_blank', {product: 'kubejs:cobalt_60', work: 120000})
 * })
 * </pre>
 * Shared by both loaders and free of KubeJS: the event classes differ between KubeJS 6 (1.20.1) and 7 (1.21.1), so they
 * live in each loader's folder and only call here.
 */
public final class ReactorScriptParts {

    private ReactorScriptParts() {}

    /** strength (needed), heat 1, rods 1, life 72 000, cap 'low_enriched' ('high_enriched', 'mox'), spent: the add-on's spent rod. */
    public static void fuelRod(String item, Object params) {
        Map<?, ?> m = map("fuelRod", item, params);
        String cap = text(m, "cap", item, "low_enriched").toUpperCase(Locale.ROOT);
        ReactorPartRegistry.Cap look;
        try {
            look = ReactorPartRegistry.Cap.valueOf(cap);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(item + ": cap must be 'low_enriched', 'high_enriched' or 'mox', got " + cap.toLowerCase(Locale.ROOT));
        }
        ReactorPartRegistry.fuelRod(id(item, item), number(m, "strength", item, null), number(m, "heat", item, 1.0),
                (int) number(m, "rods", item, 1.0), number(m, "life", item, Fission.ROD_LIFE), look, optionalId(m, "spent", item));
    }

    /** rate (needed). */
    public static void heatSink(String item, Object params) {
        Map<?, ?> m = map("heatSink", item, params);
        ReactorPartRegistry.heatSink(id(item, item), number(m, "rate", item, null));
    }

    /** rate, fluid and heatPerMb (needed), output: none, the fluid boils away. */
    public static void cell(String item, Object params) {
        Map<?, ?> m = map("cell", item, params);
        ReactorPartRegistry.cell(id(item, item), number(m, "rate", item, null), id(text(m, "fluid", item, null), item),
                number(m, "heatPerMb", item, null), optionalId(m, "output", item));
    }

    /** product and work (needed). */
    public static void channelBlank(String item, Object params) {
        Map<?, ?> m = map("channelBlank", item, params);
        ReactorPartRegistry.channelBlank(id(item, item), id(text(m, "product", item, null), item), number(m, "work", item, null));
    }

    private static Map<?, ?> map(String what, String item, Object params) {
        if (params instanceof Map<?, ?> m) return m;
        throw new IllegalArgumentException(what + "('" + item + "', ...): the second argument must be an object, got " + params);
    }

    private static double number(Map<?, ?> m, String key, String item, @Nullable Double fallback) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v == null && fallback != null) return fallback;
        throw new IllegalArgumentException(item + ": '" + key + "' must be a number, got " + v);
    }

    private static String text(Map<?, ?> m, String key, String item, @Nullable String fallback) {
        Object v = m.get(key);
        if (v instanceof CharSequence s) return s.toString();   // Rhino may hand a joined string as its own CharSequence
        if (v == null && fallback != null) return fallback;
        throw new IllegalArgumentException(item + ": '" + key + "' must be a string, got " + v);
    }

    @Nullable
    private static ResourceLocation optionalId(Map<?, ?> m, String key, String item) {
        return m.get(key) == null ? null : id(text(m, key, item, null), item);
    }

    private static ResourceLocation id(String id, String item) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) throw new IllegalArgumentException(item + ": '" + id + "' is not an id");
        return rl;
    }
}
