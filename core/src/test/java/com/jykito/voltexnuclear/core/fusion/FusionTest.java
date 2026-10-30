package com.jykito.voltexnuclear.core.fusion;

import com.jykito.voltexnuclear.core.reactor.Watcher;
import org.junit.jupiter.api.Test;

import static com.jykito.voltexnuclear.core.fusion.Fusion.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FusionTest {

    private static final double E = 1e-9;

    @Test
    void theWindowIs25To75AndAKnowingWatcherWidensItTo15To85() {
        assertEquals(0.25, low(0), E);
        assertEquals(0.75, high(0), E);
        assertEquals(0.20, low(0.5), E);
        assertEquals(0.15, low(1), E);
        assertEquals(0.85, high(1), E);
        assertEquals(0.15, low(7), E);
    }

    @Test
    void theBestMixGivesAllAndTheEdgeHalf() {
        assertEquals(1, yieldAt(0.5, 0), E);
        assertEquals(0.5, yieldAt(0.25, 0), E);
        assertEquals(0.5, yieldAt(0.75, 0), E);
        assertEquals(1 - 0.5 * 0.16, yieldAt(0.4, 0), E);
        assertEquals(0, yieldAt(0.24, 0), E);
        assertEquals(0, yieldAt(Double.NaN, 0), E);
    }

    @Test
    void aKnowingWatcherKeepsTheTorusBurningFurtherOut() {
        assertEquals(0.5, yieldAt(0.15, 1), E);
        assertEquals(0, yieldAt(0.15, 0), E);
        assertTrue(yieldAt(0.4, 1) > yieldAt(0.4, 0));
    }

    @Test
    void aKnowingWatcherBringsTheTorusToTheFusionCeiling() {
        assertEquals(299_000_000, FE_BEST * Watcher.bonus(1), 1e-3);
    }

    @Test
    void atHalfTheTorusTakesMoreTritiumThanOneChannelGivesAndAt40ItTakesOne() {
        // a reactor channel in a strong era 5 layout gives about 2 tritium cells a minute (step 19)
        assertEquals(2.4, cellsPerMinute(BURN * 0.5), E);
        assertEquals(1.92, cellsPerMinute(BURN * 0.4), E);
        assertEquals(2.4, cellsPerMinute(BURN * HELIUM_PER_FUEL), E);
    }

    @Test
    void theIgnitionAndTheCondensateMatchTheDesign() {
        assertEquals(5_000_000, IGNITION / IGNITION_TICKS, E);
        assertEquals(5, CONDENSATE * 20, E);
        // a Voltex T3 energy bank (2B) gives two ignitions; the buffer stays under the Forge energy limit
        assertEquals(2, 2_000_000_000 / IGNITION, E);
        assertTrue(BUFFER <= Integer.MAX_VALUE);
    }
}
