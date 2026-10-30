package com.jykito.voltexnuclear.content;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/** Sound events of the add-on; the files and subtitles are in assets/voltex_nuclear/sounds.json. */
public final class NuclearSounds {

    private NuclearSounds() {}

    /** One click of the Geiger counter. */
    public static Supplier<SoundEvent> GEIGER_CLICK;

    static void register(Platform p) {
        GEIGER_CLICK = p.register(Registries.SOUND_EVENT, "geiger_click",
                () -> SoundEvent.createVariableRangeEvent(VoltexNuclear.id("geiger_click")));
    }
}
