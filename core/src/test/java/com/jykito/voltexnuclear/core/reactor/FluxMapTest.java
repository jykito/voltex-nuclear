package com.jykito.voltexnuclear.core.reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluxMapTest {

    private static final double E = 1e-9;

    private static FluxMap map(String... rows) {
        return FluxMap.of(Layouts.zone("", rows));
    }

    @Test
    void aLoneRodGivesItsSourceAndABundleAQuarterMorePerExtraRod() {
        assertEquals(1.0, map(".......", ".......", ".......", "...L...").flux(3, 3), E);
        // dual high enriched: 2 rods x 2, times 1.25
        assertEquals(5.0, map(".......", ".......", ".......", "...H...").flux(3, 3), E);
        // quad low enriched: 4 x 1, times 1.75
        assertEquals(7.0, map(".......", ".......", ".......", "...Q...").flux(3, 3), E);
    }

    @Test
    void fuelNextToFuelGetsHalfItsSource() {
        FluxMap m = map(".......", ".......", ".......", "...LH..");
        assertEquals(1 + 0.5 * 4, m.flux(3, 3), E);
        assertEquals(5 + 0.5 * 1, m.flux(3, 4), E);
    }

    @Test
    void graphitePassesOnTheFuelAroundItAndGivesBackSomeOfTheCellsOwn() {
        FluxMap m = map(".......", ".......", ".......", ".LGL...");
        assertEquals(1 + 0.8 * 1 + 0.4 * 1, m.flux(3, 1), E);
        FluxMap heavy = map(".......", ".......", ".......", ".LDL...");
        assertEquals(1 + 1.0 * 1 + 0.5 * 1, heavy.flux(3, 1), E);
    }

    @Test
    void aReflectorGivesBackAndAnAbsorberOrAChannelTakesAway() {
        assertEquals(1.6, map(".......", ".......", ".......", "..RL...").flux(3, 3), E);
        assertEquals(0.75, map(".......", ".......", ".......", "..AL...").flux(3, 3), E);
        assertEquals(0.75, map(".......", ".......", ".......", "..cL...").flux(3, 3), E);
        // flux never goes below nothing
        assertEquals(0.0, map(".......", ".......", "...A...", "..ALA..", "...A...").flux(3, 3), E);
    }

    @Test
    void aChannelSeesTheFuelNextToItAndThroughAModeratorTheFuelAroundIt() {
        FluxMap m = map(".......", ".......", "...L...", "..LcG..", "....L..");
        // two rods next to it, and through the graphite the rod below it at 0.8
        assertEquals(1 + 1 + 0.8 * 1, m.irradiation(3, 3), E);
    }

    @Test
    void eachControlRodLowersTheFloorOfTheKnobByAFifth() {
        assertEquals(1.0, map().minPower(), E);
        assertEquals(0.8, map(".......", ".......", ".......", "...C...").minPower(), E);
        assertEquals(0.0, map(".......", ".CCC...", ".CCC...", ".......").minPower(), E);
        // a control rod does not damp its neighbours
        assertEquals(1.0, map(".......", ".......", ".......", "..CL...").flux(3, 3), E);
    }

    @Test
    void theFirstPresetOfTheMockUpHasItsFlux() {
        FluxMap m = FluxMap.of(Layouts.first());
        for (int[] corner : new int[][]{{2, 1}, {2, 5}, {4, 1}, {4, 5}}) assertEquals(2.2, m.flux(corner[0], corner[1]), E);
        assertEquals(3.4, m.flux(2, 3), E);
        assertEquals(3.4, m.flux(4, 3), E);
        assertEquals(0.8, m.minPower(), E);
    }

    @Test
    void thePowerfulPresetOfTheMockUpHasItsFluxAndItsChannel() {
        FluxMap m = FluxMap.of(Layouts.powerful());
        assertEquals(14.6, m.flux(1, 1), E);
        assertEquals(16.2, m.flux(1, 3), E);
        assertEquals(16.2, m.flux(3, 1), E);
        assertEquals(12.8, m.irradiation(3, 3), E);
        assertEquals(0.2, m.minPower(), E);
    }

    @Test
    void chambersOpenTheirRowsAndCorners() {
        Zone zone = new Zone();
        assertFalse(zone.open(0, 3));
        assertThrows(IllegalArgumentException.class, () -> zone.set(0, 3, Part.HEAT_SINK));
        zone.setChamber(Zone.Side.TOP, true);
        assertTrue(zone.open(0, 3));
        assertFalse(zone.open(0, 0));
        zone.setChamber(Zone.Side.LEFT, true);
        assertTrue(zone.open(0, 0));
        // a part in a shut cell is not seen
        zone.set(0, 0, Part.HEAT_SINK);
        zone.setChamber(Zone.Side.LEFT, false);
        assertNull(zone.get(0, 0));
    }
}
