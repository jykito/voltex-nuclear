package com.jykito.voltexnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * KubeJS 6 entry point (kubejs.plugins.txt): the startup event of a pack's reactor parts, {@code NuclearEvents.reactor}.
 * KubeJS loads it only where it is installed, so the add-on's code never references this package.
 */
public class NuclearKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        NuclearEvents.GROUP.register();
    }

    @Override
    public void initStartup() {
        NuclearEvents.REACTOR.post(ScriptType.STARTUP, new ReactorEventJS());
    }
}
