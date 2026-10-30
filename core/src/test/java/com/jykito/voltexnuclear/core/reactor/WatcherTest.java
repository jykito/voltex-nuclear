package com.jykito.voltexnuclear.core.reactor;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatcherTest {

    private static final double E = 1e-9;
    private static final DoubleSupplier NEVER = () -> 0.99;

    /** Gives the numbers in turn, then the last one for ever. */
    private static DoubleSupplier draws(double... v) {
        int[] i = {0};
        return () -> v[Math.min(i[0]++, v.length - 1)];
    }

    private static double run(Watcher w, int ticks, double knowledge, double stability, double hull, DoubleSupplier random) {
        double cap = w.cap();
        for (int i = 0; i < ticks; i++) cap = w.tick(knowledge, stability, hull, random);
        return cap;
    }

    @Test
    void aCoolHullIsLeftAlone() {
        assertEquals(1, run(new Watcher(), 200, 1, 1, 0.5, NEVER), E);
    }

    @Test
    void knowledgeMakesItStartEarlier() {
        // at 70 % of the hull a strain knowing nothing still waits (it starts at 90 %), a knowing one already eases off
        assertEquals(1, run(new Watcher(), 1, 0, 1, 0.7, NEVER), E);
        assertEquals(1 - Watcher.RATE_FULL, run(new Watcher(), 1, 1, 1, 0.7, NEVER), E);
    }

    @Test
    void knowledgeMakesItQuicker() {
        // near SCRAM: knowing nothing it falls 0.2 % a tick towards half, knowing fully 1 % a tick to its lower target
        assertEquals(1 - 100 * Watcher.RATE_NONE, run(new Watcher(), 100, 0, 1, 0.95, NEVER), E);
        assertEquals(1 - (0.95 - 0.6) / 0.4, run(new Watcher(), 100, 1, 1, 0.95, NEVER), E);
    }

    @Test
    void itLetsGoOnceTheHullCools() {
        Watcher w = new Watcher();
        run(w, 100, 1, 1, 0.95, NEVER);
        assertEquals(1, run(w, 100, 1, 1, 0.1, NEVER), E);
    }

    @Test
    void knowledgeAddsUpToFifteenPercent() {
        assertEquals(1, Watcher.bonus(0), E);
        assertEquals(1.075, Watcher.bonus(0.5), E);
        assertEquals(1.15, Watcher.bonus(1), E);
        assertEquals(1.15, Watcher.bonus(3), E);
    }

    @Test
    void aStableStrainNeverSlips() {
        Watcher w = new Watcher();
        run(w, 10 * Watcher.CHECK_EVERY, 0, 1, 0.1, () -> 0);
        assertEquals(Watcher.Slip.NONE, w.slip());
        assertEquals(1, w.cap(), E);
    }

    @Test
    void aFalseAlarmEasesOffACoolReactorForHalfAMinute() {
        Watcher w = new Watcher();
        run(w, Watcher.CHECK_EVERY, 1, Watcher.SLIP_CHANCE, 0, draws(0, 0.2, 0.99));
        assertEquals(Watcher.Slip.FALSE_ALARM, w.slip());
        double low = run(w, 49, 1, 1, 0, NEVER);
        assertEquals(Watcher.FALSE_ALARM_CAP, low, E);
        run(w, Watcher.SLIP_TICKS, 1, 1, 0, NEVER);
        assertEquals(Watcher.Slip.NONE, w.slip());
        assertEquals(1, run(w, 100, 1, 1, 0, NEVER), E);
    }

    @Test
    void aBlindSpellMissesTheHeat() {
        Watcher w = new Watcher();
        run(w, Watcher.CHECK_EVERY - 1, 1, 0.1, 0, NEVER);
        double cap = w.tick(1, 0.1, 0.99, draws(0, 0.7));
        assertEquals(Watcher.Slip.BLIND, w.slip());
        assertEquals(1, cap, E);
        assertEquals(1, run(w, 100, 1, 0.1, 0.99, NEVER), E);
    }

    @Test
    void theChanceOfASlipFollowsTheStability() {
        // a look slips while the draw is under (1 - stability) times the chance: at 0.5 stability that is 5 %
        Watcher w = new Watcher();
        run(w, Watcher.CHECK_EVERY, 0, 0.5, 0, draws(0.051));
        assertEquals(Watcher.Slip.NONE, w.slip());
        Watcher v = new Watcher();
        run(v, Watcher.CHECK_EVERY, 0, 0.5, 0, draws(0.049, 0.9));
        assertEquals(Watcher.Slip.BLIND, v.slip());
    }

    @Test
    void restoreKeepsTheNumbersInRange() {
        Watcher w = new Watcher();
        w.restore(Double.NaN, 99_999, Watcher.Slip.FALSE_ALARM, 0);
        assertEquals(1, w.cap(), E);
        assertTrue(w.clock() < Watcher.CHECK_EVERY);
        assertEquals(1, w.slipLeft());
        w.restore(0.3, 5, null, 40);
        assertEquals(0.3, w.cap(), E);
        assertEquals(Watcher.Slip.NONE, w.slip());
        assertEquals(0, w.slipLeft());
    }
}
