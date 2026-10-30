package com.jykito.voltexnuclear.core.cascade;

import com.jykito.voltexnuclear.core.cascade.Cascade.Run;
import com.jykito.voltexnuclear.core.cascade.Cascade.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CascadeTest {

    @Test
    void lowStageRunsFromTwoColumnsAtTheirShareOfFullSpeed() {
        assertFalse(Stage.LOW.open(1));
        assertEquals(0.5, Stage.LOW.speed(2));
        assertEquals(1.0, Stage.LOW.speed(4));
        assertEquals(3.0, Stage.LOW.speed(12));
        assertEquals(800, Stage.LOW.ticksAt(2));
        assertEquals(400, Stage.LOW.ticksAt(4));
        assertEquals(134, Stage.LOW.ticksAt(12), "1600 column ticks over 12 columns, rounded up");
    }

    @Test
    void highStageIsShutBelowTenColumns() {
        assertFalse(Stage.HIGH.open(9));
        assertEquals(0, Stage.HIGH.ticksAt(9));
        assertTrue(Stage.HIGH.open(10));
        assertEquals(800, Stage.HIGH.ticksAt(10));
        assertEquals(1.2, Stage.HIGH.speed(12), 1e-9);
    }

    @Test
    void noStageRunsOnARowLongerThanTwelve() {
        assertFalse(Stage.LOW.open(13));
        assertFalse(Stage.HIGH.open(13));
    }

    @Test
    void energyForOneUraniumDoesNotDependOnTheLength() {
        for (Stage s : Stage.values()) {
            long exact = (long) Cascade.FE_PER_COLUMN * s.work();
            for (int n = 2; n <= Cascade.MAX_COLUMNS; n++) {
                if (!s.open(n)) continue;
                long spent = (long) Cascade.energyPerTick(n) * s.ticksAt(n);
                // the last tick of a cycle may run part empty: never more than one tick of the row over
                assertTrue(spent >= exact && spent - exact < Cascade.energyPerTick(n), s + " on " + n + " columns spends " + spent);
            }
        }
        assertEquals(64_000, Stage.LOW.energy());
        assertEquals(320_000, Stage.HIGH.energy());
    }

    @Test
    void shortestRowIsTwoForTheLowStageAndTenForTheHigh() {
        assertEquals(2, Stage.LOW.minColumns());
        assertEquals(10, Stage.HIGH.minColumns());
        assertTrue(Stage.LOW.open(Stage.LOW.minColumns()));
        assertFalse(Stage.HIGH.open(Stage.HIGH.minColumns() - 1));
    }

    @Test
    void feedSplitsIntoOneEnrichedAndTheRestDepleted() {
        assertEquals(3, Stage.LOW.depleted());
        assertEquals(11, Stage.HIGH.depleted());
    }

    @Test
    void theTailsTakeEightDepletedForOneLowEnrichedAndLeaveNothing() {
        assertEquals(8, Run.TAILS.feed());
        assertEquals(0, Run.TAILS.depleted());
        assertEquals(2 * Stage.LOW.work(), Run.TAILS.work());
        assertEquals(800, Run.TAILS.ticksAt(4));   // 40 s on four columns
        assertEquals(Stage.LOW.feed, Run.LOW.feed());
        assertEquals(Stage.HIGH.depleted(), Run.HIGH.depleted());
    }

    @Test
    void theRunsOnYellowcakeKeepTheIndexOfTheirStage() {
        // the save of a cascade from before the tails keeps its cycle
        for (Stage s : Stage.values()) assertEquals(Run.of(s), Run.byIndex(s.ordinal()));
        assertEquals(Run.TAILS, Run.byIndex(Run.TAILS.index()));
    }

    @Test
    void stageIndexWraps() {
        assertEquals(Stage.LOW, Stage.byIndex(0));
        assertEquals(Stage.HIGH, Stage.byIndex(1));
        assertEquals(Stage.LOW, Stage.byIndex(2));
    }
}
