package com.jykito.voltexnuclear.core.column;

import com.jykito.voltexnuclear.core.column.HeavyWaterColumn.Flow;
import com.jykito.voltexnuclear.core.electrolyzer.Electrolyzer;
import org.junit.jupiter.api.Test;

import static com.jykito.voltexnuclear.core.column.HeavyWaterColumn.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HeavyWaterColumnTest {

    private static final double EPS = 1e-9;

    @Test
    void theLowerHalfIsHotAndTheSpareTierToo() {
        assertEquals(2, hot(4));
        assertEquals(2, cold(4));
        assertEquals(3, hot(5));
        assertEquals(2, cold(5));
        assertEquals(4, hot(8));
        assertEquals(4, cold(8));
    }

    @Test
    void aFullColumnTurnsTwentyFiveWaterIntoHalfAHeavyWater() {
        double v = speed(8, HEAT_THRESHOLD, coolantNeed(8));
        assertEquals(1, v, EPS);
        assertEquals(25, water(v), EPS);
        assertEquals(0.5, heavyWater(v), EPS);
        assertEquals(1, coolant(8, v), EPS);
        assertEquals(160, energyPerTick(8));
    }

    @Test
    void fourTiersRunAtHalfTheSpeed() {
        double v = speed(4, 5_000, 100);
        assertEquals(0.5, v, EPS);
        assertEquals(0.25, heavyWater(v), EPS);
        assertEquals(0.5, coolant(4, v), EPS);
    }

    @Test
    void belowTheHeatThresholdItStands() {
        assertEquals(0, speed(8, HEAT_THRESHOLD - 1, 100), EPS);
        assertEquals(0, speed(8, 0, 100), EPS);
    }

    @Test
    void theHeatIsAGateNotASpeed() {
        assertEquals(speed(6, HEAT_THRESHOLD, 100), speed(6, 10_000, 100), EPS);
    }

    @Test
    void shortOfCoolantItRunsSlowerAndTakesLess() {
        double v = speed(8, HEAT_THRESHOLD, coolantNeed(8) / 2);
        assertEquals(0.5, v, EPS);
        assertEquals(0.5, coolant(8, v), EPS);
        assertEquals(0, speed(8, HEAT_THRESHOLD, 0), EPS);
    }

    @Test
    void tooFewTiersDoNotRun() {
        assertEquals(0, speed(3, 10_000, 100), EPS);
    }

    @Test
    void aFlowMovesWholeMillibucketsAndKeepsTheRest() {
        Flow f = new Flow();
        int moved = 0;
        for (int i = 0; i < 40; i++) moved += f.add(0.0625);
        assertEquals(2, moved);
        assertEquals(0.5, f.owed(), EPS);
        f.giveBack(2);
        assertEquals(2.5, f.owed(), EPS);
        f.load(Double.NaN);
        assertEquals(0, f.owed(), EPS);
    }

    @Test
    void anHourOfAFullColumnFeedsTheElectrolyzerWith144Cells() {
        double perHour = heavyWater(1) * 20 * 3600;
        assertEquals(144, (int) (perHour / Electrolyzer.HEAVY_WATER_PER_CELL));
        assertEquals(160_000, Electrolyzer.energyPerCell());
    }
}
