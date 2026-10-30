package com.jykito.voltexnuclear.core.starforge;

import org.junit.jupiter.api.Test;

import static com.jykito.voltexnuclear.core.starforge.StarForge.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarForgeTest {

    private static final double E = 1e-12;

    @Test
    void belowTheStarHeatNothingMelts() {
        assertEquals(0, rate(0), E);
        assertEquals(0, rate(4_000), E);   // all a Voltex heat pipe holds
        assertEquals(0, rate(STAR_HEAT - 1), E);
    }

    @Test
    void anIngotTakes30SecondsAtTheStarHeatAnd15AtTheTop() {
        assertEquals(1.0 / 600, rate(STAR_HEAT), E);
        assertEquals(1.0 / 450, rate(9_000), E);
        assertEquals(1.0 / 300, rate(TOP_HEAT), E);
        assertEquals(1.0 / 300, rate(20_000), E);
    }

    @Test
    void aMeltFinishesAfterItsTicksAndStops() {
        Melt m = new Melt();
        assertFalse(m.tick(TOP_HEAT));   // nothing started
        m.start();
        for (int i = 1; i < 300; i++) assertFalse(m.tick(TOP_HEAT));
        assertTrue(m.tick(TOP_HEAT));
        assertFalse(m.running());
        assertEquals(0, m.done(), E);
    }

    @Test
    void aMeltThatLosesItsHeatWaitsAndGoesOn() {
        Melt m = new Melt();
        m.start();
        for (int i = 0; i < 150; i++) m.tick(TOP_HEAT);
        assertEquals(0.5, m.done(), 1e-9);
        for (int i = 0; i < 1_000; i++) assertFalse(m.tick(STAR_HEAT - 1));
        assertEquals(0.5, m.done(), 1e-9);
        assertTrue(m.running());
        for (int i = 1; i < 150; i++) assertFalse(m.tick(TOP_HEAT));
        assertTrue(m.tick(TOP_HEAT));
    }

    @Test
    void aSavedMeltIsTakenBackSafely() {
        Melt m = new Melt();
        m.load(true, 0.25);
        assertTrue(m.running());
        assertEquals(0.25, m.done(), E);
        m.load(true, Double.NaN);
        assertEquals(0, m.done(), E);
        m.load(true, 7);
        assertEquals(1, m.done(), E);
        assertFalse(m.tick(0));   // a full share finishes on its next hot tick, not before
        assertTrue(m.tick(STAR_HEAT));
        m.load(false, 0.9);
        assertFalse(m.running());
        assertEquals(0, m.done(), E);
    }
}
