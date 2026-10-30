package com.jykito.voltexnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * KubeJS 7 entry point (kubejs.plugins.txt): the startup event of a pack's reactor parts, {@code NuclearEvents.reactor}.
 * KubeJS loads it only where it is installed, so the add-on's code never references this package.
 */
public class NuclearKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(NuclearEvents.GROUP);
    }

    @Override
    public void initStartup() {
        NuclearEvents.REACTOR.post(ScriptType.STARTUP, new ReactorEventJS());
    }
}
