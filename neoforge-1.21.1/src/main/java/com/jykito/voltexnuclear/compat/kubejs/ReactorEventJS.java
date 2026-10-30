package com.jykito.voltexnuclear.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeStartupEvent;

/** A pack's reactor parts in a startup script; the parameters are read by {@link ReactorScriptParts}, shared with 1.20.1. */
public class ReactorEventJS implements KubeStartupEvent {

    public void fuelRod(String item, Object params) {
        ReactorScriptParts.fuelRod(item, params);
    }

    public void heatSink(String item, Object params) {
        ReactorScriptParts.heatSink(item, params);
    }

    public void cell(String item, Object params) {
        ReactorScriptParts.cell(item, params);
    }

    public void channelBlank(String item, Object params) {
        ReactorScriptParts.channelBlank(item, params);
    }
}
