package com.jykito.voltexnuclear.machine;

import net.minecraft.world.WorldlyContainer;
import org.jetbrains.annotations.Nullable;

/**
 * A machine of the add-on that takes energy from cables, items from pipes and, if it has a tank, a fluid from pipes. The
 * items go through the vanilla {@link WorldlyContainer}, which hoppers use as it is and both loaders wrap for their pipes;
 * the energy through {@link EnergyIn}, the fluid through {@link FluidIn}. Each loader exposes them as its own
 * capabilities.
 */
public interface Machine extends WorldlyContainer {

    EnergyIn energy();

    /** The tank pipes fill, or null for a machine without one. */
    @Nullable
    default FluidIn fluid() {
        return null;
    }
}
