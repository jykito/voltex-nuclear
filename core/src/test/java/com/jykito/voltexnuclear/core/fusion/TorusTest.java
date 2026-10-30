package com.jykito.voltexnuclear.core.fusion;

import com.jykito.voltexnuclear.core.fusion.Torus.Phase;
import com.jykito.voltexnuclear.core.fusion.Torus.Tick;
import com.jykito.voltexnuclear.core.fusion.Torus.Why;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorusTest {

    private static final double E = 1e-6;
    private static final DoubleSupplier NEVER = () -> 0.99;

    /** Gives the numbers in turn, then the last one for ever. */
    private static DoubleSupplier draws(double... v) {
        int[] i = {0};
        return () -> v[Math.min(i[0]++, v.length - 1)];
    }

    private static Tick run(Torus t, int ticks) {
        return run(t, ticks, 0, 1, NEVER);
    }

    private static Tick run(Torus t, int ticks, double knowledge, double stability, DoubleSupplier random) {
        Tick last = null;
        for (int i = 0; i < ticks; i++) last = t.tick(knowledge, stability, random);
        return last;
    }

    /** Full tanks, a full reserve, not started. */
    private static Torus ready() {
        Torus t = new Torus();
        t.restoreTanks(Fusion.FUEL_TANK, Fusion.FUEL_TANK, Fusion.CONDENSATE_TANK, 0, Fusion.RESERVE);
        return t;
    }

    /** Started and burning: the ignition is over and the reserve spent. */
    private static Torus lit() {
        Torus t = ready();
        t.start();
        assertEquals(Phase.LIT, run(t, Fusion.IGNITION_TICKS).phase());
        return t;
    }

    private static void setCondensate(Torus t, double mb) {
        t.restoreTanks(t.deuterium(), t.tritium(), mb, t.helium(), t.buffer());
    }

    // ---- ignition

    @Test
    void theIgnitionTakesTenSecondsOfTheReserveAndNoFuel() {
        Torus t = ready();
        t.start();
        Tick k = run(t, Fusion.IGNITION_TICKS - 1);
        assertEquals(Phase.HEAT, k.phase());
        assertEquals(0, k.energy(), E);
        assertEquals(Fusion.FUEL_TANK, t.deuterium(), E);
        assertEquals(Fusion.FUEL_TANK, t.tritium(), E);
        assertEquals(Fusion.CONDENSATE_TANK - 199 * Fusion.CONDENSATE, t.condensate(), E);
        assertEquals(Fusion.RESERVE - 199 * 5_000_000.0, t.buffer(), E);
        // the last tick of the ignition already burns
        k = t.tick(0, 1, NEVER);
        assertEquals(Phase.LIT, k.phase());
        assertEquals(0, t.buffer(), E);
        assertEquals(Fusion.FE_BEST, k.energy(), E);
    }

    @Test
    void itWaitsForWhatItLacks() {
        Torus t = new Torus();
        t.start();
        assertEquals(Why.NO_FUEL, waitFor(t));
        t.fillDeuterium();
        assertEquals(Why.NO_TRITIUM, waitFor(t));
        t.restoreTanks(0, Fusion.CELL, 0, 0, 0);
        assertEquals(Why.NO_DEUTERIUM, waitFor(t));
        t.restoreTanks(Fusion.CELL, Fusion.CELL, 49, 0, 0);
        assertEquals(Why.NO_CONDENSATE, waitFor(t));
        t.addCondensate(1);
        t.setMix(0.1);
        assertEquals(Why.OUT_OF_WINDOW, waitFor(t));
        t.setMix(0.5);
        assertEquals(Why.RESERVE, waitFor(t));
        t.restoreTanks(Fusion.CELL, Fusion.CELL, 50, 0, Fusion.RESERVE);
        assertEquals(Phase.HEAT, t.tick(0, 1, NEVER).phase());
    }

    private static Why waitFor(Torus t) {
        assertEquals(Phase.WAIT, t.tick(0, 1, NEVER).phase());
        return t.why();
    }

    @Test
    void itDoesNotIgniteOnTheScrapsOfAStoppedSupply() {
        Torus t = new Torus();
        t.restoreTanks(Fusion.FUEL_TANK, Fusion.CELL - 1, Fusion.CONDENSATE_TANK, 0, Fusion.RESERVE);
        t.start();
        assertEquals(Why.NO_TRITIUM, waitFor(t));
    }

    @Test
    void anIgnitionCutShortLosesItsEnergy() {
        Torus t = ready();
        t.start();
        run(t, 100);
        t.setMix(0.1);
        Tick k = t.tick(0, 1, NEVER);
        assertEquals(Phase.WAIT, k.phase());
        assertEquals(Why.OUT_OF_WINDOW, t.why());
        assertEquals(Fusion.RESERVE - 100 * 5_000_000.0, t.buffer(), E);
        t.setMix(0.5);
        assertEquals(Why.RESERVE, waitFor(t));
    }

    @Test
    void itReignitesByItselfOnceAllIsBack() {
        Torus t = lit();
        t.setMix(0.1);
        assertEquals(Phase.WAIT, run(t, Fusion.GRACE).phase());
        t.setMix(0.5);
        assertEquals(Why.RESERVE, waitFor(t));
        for (int i = 0; i < 10; i++) assertEquals(Fusion.INTAKE, t.charge(Fusion.IGNITION), E);
        assertEquals(0, t.charge(Fusion.IGNITION), E);
        assertEquals(Phase.HEAT, t.tick(0, 1, NEVER).phase());
        assertEquals(Phase.LIT, run(t, Fusion.IGNITION_TICKS - 1).phase());
    }

    // ---- burning

    @Test
    void theBestMixGives260MAndBurnsFourUnits() {
        Torus t = lit();
        Tick k = t.tick(0, 1, NEVER);
        assertEquals(Phase.LIT, k.phase());
        assertEquals(260_000_000, k.energy(), E);
        assertEquals(0.5, k.mix(), E);
        assertEquals(1, k.yield(), E);
        assertEquals(2, k.deuterium(), E);
        assertEquals(2, k.tritium(), E);
        assertEquals(2, k.helium(), E);
        assertEquals(Fusion.CONDENSATE, k.condensate(), E);
    }

    @Test
    void theEdgeOfTheWindowGivesHalf() {
        Torus t = lit();
        t.setMix(0.25);
        Tick k = t.tick(0, 1, NEVER);
        assertEquals(130_000_000, k.energy(), E);
        assertEquals(3, k.deuterium(), E);
        assertEquals(1, k.tritium(), E);
    }

    @Test
    void theMixMovesByItselfWhenOneFuelRunsShort() {
        Torus t = lit();
        t.restoreTanks(1, t.tritium(), t.condensate(), t.helium(), t.buffer());
        Tick k = t.tick(0, 1, NEVER);
        double x = 2.0 / 3, f = Fusion.yieldAt(x, 0);
        assertEquals(Phase.LIT, k.phase());
        assertEquals(x, k.mix(), E);
        assertEquals(Fusion.FE_BEST * f * 3 / 4, k.energy(), E);
        k = t.tick(0, 1, NEVER);
        assertEquals(Phase.COOL, k.phase());
        assertEquals(Why.NO_DEUTERIUM, t.why());
    }

    // ---- cooling

    @Test
    void outOfTheWindowItCoolsAndBackInTimeItBurnsOn() {
        Torus t = lit();
        t.setMix(0.2);
        Tick k = t.tick(0, 1, NEVER);
        assertEquals(Phase.COOL, k.phase());
        assertEquals(Why.OUT_OF_WINDOW, t.why());
        assertEquals(0, k.energy(), E);
        assertEquals(0, k.deuterium() + k.tritium(), E);
        run(t, 50);
        t.setMix(0.5);
        k = t.tick(0, 1, NEVER);
        assertEquals(Phase.LIT, k.phase());
        assertEquals(Why.NONE, t.why());
        assertEquals(Fusion.FE_BEST, k.energy(), E);
    }

    @Test
    void fiveSecondsOutPutItOutWithoutHarm() {
        Torus t = lit();
        t.setMix(0.1);
        assertEquals(Phase.COOL, run(t, Fusion.GRACE - 1).phase());
        assertEquals(Phase.WAIT, t.tick(0, 1, NEVER).phase());
        assertEquals(Fusion.FUEL_TANK - 2, t.deuterium(), E);
    }

    @Test
    void runningOutOfTritiumCoolsAndACellInTimeSavesIt() {
        Torus t = ready();
        t.restoreTanks(Fusion.FUEL_TANK, Fusion.CELL, Fusion.CONDENSATE_TANK, 0, Fusion.RESERVE);
        t.start();
        run(t, Fusion.IGNITION_TICKS);
        assertEquals(Phase.LIT, run(t, 499).phase());
        assertEquals(0, t.tritium(), E);
        assertEquals(Phase.COOL, t.tick(0, 1, NEVER).phase());
        assertEquals(Why.NO_TRITIUM, t.why());
        assertTrue(t.fillTritium());
        assertEquals(Phase.LIT, t.tick(0, 1, NEVER).phase());
    }

    @Test
    void noCondensateCoolsToo() {
        Torus t = lit();
        setCondensate(t, 0);
        assertEquals(Phase.COOL, t.tick(0, 1, NEVER).phase());
        assertEquals(Why.NO_CONDENSATE, t.why());
        t.addCondensate(10);
        assertEquals(Phase.LIT, t.tick(0, 1, NEVER).phase());
    }

    @Test
    void stopPutsItOutAtOnce() {
        Torus t = lit();
        t.stop();
        assertEquals(Phase.OFF, t.phase());
        Tick k = t.tick(0, 1, NEVER);
        assertEquals(Phase.OFF, k.phase());
        assertEquals(0, k.energy(), E);
        t.start();
        assertEquals(Why.RESERVE, waitFor(t));
    }

    @Test
    void aQuenchPutsItOutAndLeavesItStarted() {
        Torus t = lit();
        t.quench();
        assertEquals(Phase.WAIT, t.phase());
        assertTrue(t.running());
        assertEquals(0, t.tick(0, 1, NEVER).energy(), E);
        assertEquals(Why.RESERVE, t.why());
    }

    // ---- the ports and the buffer

    @Test
    void theEnergyPortsTakeOnlyWhatIsAboveTheReserve() {
        Torus t = new Torus();
        t.restoreTanks(0, 0, 0, 0, 1_500_000_000);
        assertEquals(500_000_000, t.drain(1_000_000_000), E);
        assertEquals(Fusion.RESERVE, t.buffer(), E);
        assertEquals(0, t.drain(1_000_000_000), E);
    }

    @Test
    void anEnergyPortChargesTheReserveAtABanksPace() {
        Torus t = new Torus();
        t.restoreTanks(0, 0, 0, 0, 500_000_000);
        assertEquals(Fusion.INTAKE, t.charge(Fusion.BUFFER), E);
        t.restoreTanks(0, 0, 0, 0, 950_000_000);
        assertEquals(50_000_000, t.charge(Fusion.BUFFER), E);
        assertEquals(0, t.charge(Fusion.BUFFER), E);
    }

    @Test
    void aFullBufferLosesTheRest() {
        Torus t = new Torus();
        t.restoreTanks(0, 0, 0, 0, 1_900_000_000);
        assertEquals(160_000_000, t.store(260_000_000), E);
        assertEquals(Fusion.BUFFER, t.buffer(), E);
    }

    @Test
    void theOwnOutputRefillsTheReserveFirst() {
        Torus t = lit();
        t.store(t.tick(0, 1, NEVER).energy());
        assertEquals(0, t.drain(Double.MAX_VALUE), E);
        for (int i = 0; i < 3; i++) t.store(t.tick(0, 1, NEVER).energy());
        assertEquals(4 * Fusion.FE_BEST - Fusion.RESERVE, t.drain(Double.MAX_VALUE), E);
    }

    @Test
    void aCellGoesInOnlyWhenItFitsWhole() {
        Torus t = new Torus();
        for (int i = 0; i < 8; i++) assertTrue(t.fillDeuterium());
        assertFalse(t.fillDeuterium());
        t.restoreTanks(Fusion.FUEL_TANK - Fusion.CELL + 1, 0, 0, 0, 0);
        assertFalse(t.fillDeuterium());
        t.restoreTanks(Fusion.FUEL_TANK - Fusion.CELL, 0, 0, 0, 0);
        assertTrue(t.fillDeuterium());
        assertEquals(Fusion.CONDENSATE_TANK, t.addCondensate(20_000), E);
        assertEquals(0, t.addCondensate(5), E);
    }

    @Test
    void heliumFillsACellAtAThousandAndAFullTankVents() {
        Torus t = lit();
        run(t, 499);
        assertEquals(1_000, t.helium(), E);
        assertTrue(t.takeHelium());
        assertFalse(t.takeHelium());
        t.restoreTanks(t.deuterium(), t.tritium(), t.condensate(), Fusion.HELIUM_TANK, t.buffer());
        Tick k = t.tick(0, 1, NEVER);
        assertEquals(2, k.vented(), E);
        assertEquals(Fusion.HELIUM_TANK, t.helium(), E);
        assertEquals(Phase.LIT, k.phase());
    }

    // ---- the Watcher

    @Test
    void aKnowingWatcherAddsFifteenPercentAndWidensTheWindow() {
        Torus t = lit();
        assertEquals(299_000_000, t.tick(1, 1, NEVER).energy(), E);
        t.setMix(0.2);
        assertEquals(Phase.LIT, t.tick(1, 1, NEVER).phase());
        assertEquals(Phase.COOL, t.tick(0, 1, NEVER).phase());
    }

    @Test
    void anUnstableWatcherKnocksTheMixOffForSixSeconds() {
        Torus t = lit();
        DoubleSupplier r = draws(0, 0.9, 0.99);
        Tick k = t.tick(0, 0, r);
        assertEquals(0.65, k.mix(), E);
        assertEquals(1 - 0.5 * 0.36, k.yield(), E);
        assertEquals(0.65, t.fed(), E);
        run(t, Fusion.JITTER_TICKS - 1, 0, 0, r);
        assertEquals(0.65, t.fed(), E);
        k = t.tick(0, 0, r);
        assertEquals(0.5, k.mix(), E);
        assertEquals(0, t.jitter(), E);
    }

    @Test
    void nearTheEdgeAKnockPutsTheTorusOut() {
        Torus t = lit();
        t.setMix(0.3);
        DoubleSupplier r = draws(0, 0.1, 0.99);
        assertEquals(Phase.COOL, t.tick(0, 0, r).phase());
        assertEquals(0.15, t.fed(), E);
        assertEquals(Phase.WAIT, run(t, Fusion.GRACE - 1, 0, 0, r).phase());
    }

    @Test
    void aStableWatcherNeverSlips() {
        Torus t = lit();
        run(t, 2_000, 1, 1, () -> 0);
        assertEquals(0, t.jitter(), E);
        assertEquals(Phase.LIT, t.phase());
    }

    // ---- saving

    @Test
    void whatComesBackFromASaveIsClamped() {
        Torus t = new Torus();
        t.restoreTanks(Double.NaN, 1e9, -5, 1e9, 3e9);
        assertEquals(0, t.deuterium(), E);
        assertEquals(Fusion.FUEL_TANK, t.tritium(), E);
        assertEquals(0, t.condensate(), E);
        assertEquals(Fusion.HELIUM_TANK, t.helium(), E);
        assertEquals(Fusion.BUFFER, t.buffer(), E);

        t.restoreRun(false, Phase.LIT, Why.NONE, 0, 0, 0, 0);
        assertEquals(Phase.OFF, t.phase());
        t.restoreRun(true, Phase.HEAT, null, 500, 7, 0.3, 50);
        assertEquals(Phase.HEAT, t.phase());
        assertEquals(Why.NONE, t.why());
        assertEquals(Fusion.IGNITION_TICKS - 1, t.heating());
        assertEquals(0, t.cooling());
        assertEquals(Fusion.JITTER, t.jitter(), E);
        t.restoreRun(true, Phase.COOL, Why.NO_TRITIUM, 0, 500, -0.15, 0);
        assertEquals(Fusion.GRACE - 1, t.cooling());
        assertEquals(0, t.jitter(), E);
    }
}
