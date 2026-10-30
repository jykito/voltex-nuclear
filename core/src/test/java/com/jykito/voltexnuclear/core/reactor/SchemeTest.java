package com.jykito.voltexnuclear.core.reactor;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemeTest {

    private static final double E = 1e-6;

    @Test
    void theSchemesGiveWhatTheSearchFound() {
        assertEquals(54_600, Scheme.FIRST.run().energy(), E);
        assertEquals(238_000, Scheme.WATER.run().energy(), E);
        assertEquals(1_493_800, Scheme.POWERFUL.run().energy(), E);
        assertEquals(2_584_400, Scheme.PEAK.run().energy(), E);
    }

    @Test
    void noSchemeHeatsItsHull() {
        for (Scheme s : Scheme.ALL) assertEquals(0, s.run().toHull(), E, s.key());
    }

    @Test
    void everySchemeCanBeThrottledAndReadsWhole() {
        Set<String> keys = new HashSet<>();
        for (Scheme s : Scheme.ALL) {
            assertTrue(keys.add(s.key()), s.key());
            assertEquals(Zone.SIZE, s.rows().length, s.key());
            int controls = 0;
            for (int r = 0; r < Zone.SIZE; r++) {
                assertEquals(Zone.SIZE, s.rows()[r].length(), s.key());
                for (int c = 0; c < Zone.SIZE; c++) {
                    char ch = s.rows()[r].charAt(c);
                    if (ch != '.') assertNotNull(Scheme.CODE.get(ch), s.key() + " " + ch);
                    if (ch == 'C') controls++;
                }
            }
            assertTrue(controls > 0, s.key());
        }
    }

    @Test
    void theLadderClimbsWithinEachEra() {
        // era 4: first, single, water; era 5 from the dual 5 x 5 to the peak (the breeder and the closed cycle trade some power)
        Scheme[] ladder = {Scheme.FIRST, Scheme.SINGLE, Scheme.WATER, Scheme.DUAL_SMALL, Scheme.HIGH, Scheme.POWERFUL, Scheme.QUAD, Scheme.PEAK};
        for (int i = 1; i < ladder.length; i++)
            assertTrue(ladder[i].run().energy() > ladder[i - 1].run().energy(), ladder[i].key());
    }

    @Test
    void assembliesSaveFuel() {
        // a rod in an assembly gives more energy than a single one: the dual 7 x 7 and the water 5 x 5, both low enriched
        double single = Scheme.WATER.run().energy() / Scheme.WATER.rodsPerHour();
        double dual = Scheme.DUAL.run().energy() / Scheme.DUAL.rodsPerHour();
        double quad = Scheme.QUAD.run().energy() / Scheme.QUAD.rodsPerHour();
        assertTrue(dual > 1.3 * single && quad > 1.3 * dual);
        assertEquals(12, Scheme.WATER.rodsPerHour(), E);
        assertEquals(1.5 * Fission.ROD_LIFE, Part.fuel(Fuel.LOW_ENRICHED, 2).life(), E);
        assertEquals(2.5 * Fission.ROD_LIFE, Part.fuel(Fuel.HIGH_ENRICHED, 4).life(), E);
    }

    @Test
    void theClosedCycleRunsOnWhatThePlantGivesBack() {
        // the plant gives a quarter plutonium and a quarter low enriched uranium a spent rod
        double mox = 0, leu = 0, all = 0;
        Zone z = Scheme.CLOSED.zone();
        for (int r = 0; r < Zone.SIZE; r++)
            for (int c = 0; c < Zone.SIZE; c++) {
                Part p = z.get(r, c);
                if (p == null || !p.isFuel()) continue;
                double perHour = p.rods() * Scheme.TICKS_PER_HOUR / p.life();
                all += perHour;
                if (p.fuel() == Fuel.MOX) mox += perHour;
                if (p.fuel() == Fuel.LOW_ENRICHED) leu += perHour;
            }
        assertTrue(mox > 0 && mox <= all / 4 && leu > 0 && leu <= all / 4);
    }

    @Test
    void onlyTheBreedersTurnBlanks() {
        for (Scheme s : Scheme.ALL)
            assertEquals(s == Scheme.BREEDER || s == Scheme.HEAVY_BREEDER, s.blanksPerHour() > 0, s.key());
        assertTrue(Scheme.BREEDER.blanksPerHour() > 144);   // a torus at full burn takes 144 tritium cells an hour
    }

    @Test
    void aSchemeReadsItsCellsAndChambers() {
        assertEquals(Part.CONTROL_ROD, Scheme.FIRST.part(3, 3));
        assertNull(Scheme.FIRST.part(0, 0));
        assertTrue(Scheme.POWERFUL.needs(Zone.Side.TOP));
        assertEquals(Part.CHANNEL, Scheme.BREEDER.part(1, 1));
        assertEquals(Part.fuel(Fuel.MOX, 4), Scheme.CLOSED.part(2, 2));
    }
}
