package com.jykito.voltexnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/** The add-on's KubeJS events: {@code NuclearEvents.reactor(event => ...)} in a startup script. */
public class NuclearEvents {

    public static final EventGroup GROUP = EventGroup.of("NuclearEvents");

    public static final EventHandler REACTOR = GROUP.startup("reactor", () -> ReactorEventJS.class);
}
