package com.jykito.voltexnuclear.core;

/**
 * Identity of the add-on, the same for both loaders.
 * Everything in {@code core} is plain Java: no Minecraft classes, so it compiles and is tested once for both versions.
 */
public final class ModInfo {

    private ModInfo() {}

    public static final String MOD_ID = "voltex_nuclear";
    public static final String NAME = "Voltex:Nuclear";

    /** Mod id of Voltex, the required base mod. */
    public static final String VOLTEX_ID = "industrial_core";
}
