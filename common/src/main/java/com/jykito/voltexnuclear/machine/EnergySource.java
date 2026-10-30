package com.jykito.voltexnuclear.machine;

/**
 * A multiblock that gives energy out through {@link EnergyOutputPortBlockEntity energy output connectors}: the fission
 * reactor now, the fusion torus later. Cables that draw energy themselves take it through the connector from here.
 */
public interface EnergySource {

    /** Gives up to {@code max} FE out of the buffer; returns how much it gave, or would give when simulating. */
    int giveEnergy(int max, boolean simulate);

    int energyStored();

    int energyCapacity();
}
