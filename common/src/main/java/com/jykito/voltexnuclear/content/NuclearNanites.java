package com.jykito.voltexnuclear.content;

import com.jykito.industrialcore.api.VoltexNanites;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.voltexnuclear.core.ModInfo;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The add-on's nanite class, the Watcher (DESIGN step 10), through the Voltex nanite API: the Voltex evolution chamber
 * breeds it on core samples, which only a working fission reactor gives off, and eats them; its affinity reads as its
 * knowledge of the reactor, as a scientist's does of its topic. It works in the reactor's Watcher slot.
 */
public final class NuclearNanites {

    private NuclearNanites() {}

    /** The one target the Watcher selects for. */
    public static final String TARGET = "reactor";
    /** The add-on's uranium green: the strain's tinted parts and its colour in the atlas. */
    public static final int COLOR = 0x7DDC4B;

    public static NaniteGenome.Kind WATCHER;

    static void register(Platform p) {
        NaniteGenome.Kind scientist = NaniteGenome.Kind.SCIENTIST;
        WATCHER = VoltexNanites.kind(p.id(ModInfo.MOD_ID, "watcher"))
                .cap(scientist.cap())
                .step(scientist.step(), scientist.stepHeat())
                .knowledge()
                .feed(item -> item == NuclearItems.CORE_SAMPLE.get() ? TARGET : null)
                .targetName(t -> Component.translatable("nanite_theme.voltex_nuclear." + t))
                .atlas(() -> List.of(TARGET), t -> NuclearItems.CORE_SAMPLE.get())
                .register();
        VoltexNanites.color(TARGET, COLOR);
    }

    /** The Watcher strain in a stack, or null when the stack holds none. */
    @Nullable
    public static NaniteGenome watcher(ItemStack stack) {
        if (stack.isEmpty() || WATCHER == null) return null;
        NaniteGenome g = NaniteGenome.read(stack);
        return g.kind() == WATCHER ? g : null;
    }

    /** What the strain knows of the reactor, 0 to 1. */
    public static double knowledge(NaniteGenome g) {
        return Math.max(0, Math.min(1, g.effect(TARGET)));
    }
}
