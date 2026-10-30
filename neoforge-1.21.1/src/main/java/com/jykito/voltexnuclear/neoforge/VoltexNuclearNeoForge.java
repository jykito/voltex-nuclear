package com.jykito.voltexnuclear.neoforge;

import net.neoforged.neoforge.common.NeoForge;
import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.core.ModInfo;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** NeoForge 1.21.1 entry point: hands the shared code its platform, gives the machines their capabilities, hooks up the Stellar suit. */
@Mod(ModInfo.MOD_ID)
public final class VoltexNuclearNeoForge {

    public VoltexNuclearNeoForge(IEventBus modBus) {
        VoltexNuclear.init(new NeoForgePlatform(modBus));
        modBus.addListener(NeoForgeMachineCaps::register);
        NeoForge.EVENT_BUS.addListener(NeoForgeSuitEvents::tick);
        NeoForge.EVENT_BUS.addListener(NeoForgeSuitEvents::damage);
    }
}
