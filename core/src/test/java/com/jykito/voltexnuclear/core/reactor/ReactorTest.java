package com.jykito.voltexnuclear.core.reactor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorTest {

    private static final double E = 1e-6, PLENTY = Double.POSITIVE_INFINITY;

    private static Reactor running(Zone zone) {
        Reactor r = new Reactor(zone);
        r.start();
        return r;
    }

    private static Reactor running(String... rows) {
        return running(Layouts.zone("", rows));
    }

    // ---- the numbers of the mock-up

    @Test
    void theFirstPresetGivesAbout55kAndItsSinksTakeAllTheHeat() {
        Reactor.Tick t = running(Layouts.first()).tick(PLENTY, PLENTY);
        assertEquals(1.0, t.power(), E);
        assertEquals(54_600, t.energy(), E);
        assertEquals(93.6, t.heat(), E);
        assertEquals(93.6, t.removed(), E);
        assertEquals(0, t.toHull(), E);
    }

    @Test
    void thePowerfulPresetAtThreeQuartersGives323kAndKeepsItsHullCool() {
        Reactor r = running(Layouts.powerful());
        r.setKnob(0.75);
        Reactor.Tick t = r.tick(PLENTY, PLENTY);
        assertEquals(0.75, t.power(), E);
        assertEquals(323_400, t.energy(), E);
        assertEquals(776.16, t.heat(), E);
        assertEquals(776.16, t.removed(), E);
        assertEquals(0, t.toHull(), E);
    }

    // ---- the controls

    @Test
    void aStoppedReactorGivesNothing() {
        Reactor r = new Reactor(Layouts.first());
        assertEquals(0, r.tick(PLENTY, PLENTY).energy(), E);
        r.start();
        r.stop();
        assertEquals(0, r.tick(PLENTY, PLENTY).energy(), E);
    }

    @Test
    void theKnobGoesNoLowerThanTheControlRodsAllow() {
        Reactor r = running(Layouts.first());   // one control rod: 80 %
        r.setKnob(0.5);
        assertEquals(0.8, r.tick(PLENTY, PLENTY).power(), E);
        Reactor bare = running(".......", ".......", ".......", "...L...");
        bare.setKnob(0.3);
        assertEquals(1.0, bare.tick(PLENTY, PLENTY).power(), E);
    }

    // ---- heat

    @Test
    void heatIsSharedEvenlyAmongTheOutletsNextToTheRod() {
        Reactor.Tick t = running(".......", ".......", ".......", "..SLW..").tick(PLENTY, PLENTY);
        assertEquals(6, t.heat(), E);
        assertEquals(6, t.removed(), E);
        assertEquals(3 / Fission.HEAT_PER_WATER_MB, t.waterMb(), E);
    }

    @Test
    void anExchangerGroupPassesItsShareOnToTheOutletsAroundIt() {
        assertEquals(6, running(".......", ".......", ".......", "..LXXS.").tick(PLENTY, PLENTY).removed(), E);
        // a group with no outlet sends its share into the hull
        assertEquals(6, running(".......", ".......", ".......", "..LXX..").tick(PLENTY, PLENTY).toHull(), E);
    }

    @Test
    void aHeatSinkTakesUpToItsRateAndTheRestGoesIntoTheHull() {
        // a quad high enriched rod: flux 4 x 2 x 1.75 = 14, heat 14 x 6 x 1.4 = 117.6
        Reactor.Tick t = running(".......", ".......", ".......", "...US..").tick(PLENTY, PLENTY);
        assertEquals(117.6, t.heat(), E);
        assertEquals(80, t.removed(), E);
        assertEquals(37.6, t.toHull(), E);
    }

    @Test
    void aWaterCellTakesHeatAsLongAsItHasWater() {
        // two quad high enriched rods give it 2 x 117.6; it takes its 150 for 7.5 mB, then what 2 mB of water allow
        Reactor r = running(".......", ".......", ".......", "..UWU..");
        Reactor.Tick full = r.tick(PLENTY, PLENTY);
        assertEquals(150, full.removed(), E);
        assertEquals(2 * 117.6 - 150, full.toHull(), E);
        assertEquals(7.5, full.waterMb(), E);
        Reactor.Tick short_ = r.tick(2, PLENTY);
        assertEquals(40, short_.removed(), E);
        assertEquals(2 * 117.6 - 40, short_.toHull(), E);
        assertEquals(2, short_.waterMb(), E);
    }

    @Test
    void anOutletShowsTheHeatItWasGivenOnTheLastTick() {
        Reactor r = running(".......", ".......", ".......", "..UWU..");
        r.tick(2, PLENTY);
        assertEquals(2 * 117.6, r.load(3, 3), E);   // given, though only 40 of it went into the water
        assertEquals(0, r.load(3, 2), E);
    }

    @Test
    void aCryoCellTakesHeatAsLongAsItHasCoolant() {
        Reactor r = running(".......", ".......", ".......", "...UK..");
        Reactor.Tick full = r.tick(PLENTY, 1);
        assertEquals(117.6, full.removed(), E);
        assertEquals(117.6 / 600, full.coolantMb(), E);
        Reactor.Tick short_ = r.tick(PLENTY, 0.1);
        assertEquals(60, short_.removed(), E);
        assertEquals(57.6, short_.toHull(), E);
        assertEquals(0.1, short_.coolantMb(), E);
    }

    @Test
    void aCryoCellWithoutCoolantIsGivenHeatButTakesNone() {
        Reactor r = running(".......", ".......", ".......", "...UK..");
        r.tick(PLENTY, 0);
        assertEquals(117.6, r.load(3, 4), E);   // the rod still shares its heat with it
        assertEquals(0, r.took(3, 4), E);       // the window shows this cell short of coolant
        r.tick(PLENTY, PLENTY);
        assertEquals(117.6, r.took(3, 4), E);
    }

    @Test
    void aFullHullScramsTheReactorAndResetCoolsIt() {
        // a lone quad high enriched rod heats the hull by 117.6 - 20 a tick: SCRAM on tick 205
        Reactor r = running(".......", ".......", ".......", "...U...");
        int ticks = 0;
        while (!r.scrammed()) {
            r.tick(PLENTY, PLENTY);
            ticks++;
        }
        assertEquals(205, ticks);
        assertEquals(0, r.tick(PLENTY, PLENTY).energy(), E);
        r.reset();
        assertFalse(r.scrammed());
        assertEquals(0, r.hull(), E);
        assertTrue(r.tick(PLENTY, PLENTY).energy() > 0);
    }

    @Test
    void scramByHandStopsTheReactor() {
        Reactor r = running(Layouts.first());
        r.scram();
        assertFalse(r.running());
        assertEquals(0, r.tick(PLENTY, PLENTY).power(), E);
        r.reset();
        assertEquals(0, r.tick(PLENTY, PLENTY).power(), E);   // it needs starting again
    }

    @Test
    void aHeldReactorGivesNothingAndBurnsNothingButItsHullCools() {
        Reactor r = running(".......", ".......", ".......", "..UUU..");   // no outlets: all the heat goes into the hull
        r.tick(PLENTY, PLENTY);
        double hull = r.hull();
        r.hold(true);
        Reactor.Tick t = r.tick(PLENTY, PLENTY);
        assertEquals(0, t.power(), E);
        assertEquals(0, t.energy(), E);
        assertEquals(1, r.wear(3, 3), E);
        assertEquals(hull - Fission.HULL_COOLING, r.hull(), E);
        assertTrue(r.running());
        r.hold(false);
        assertTrue(r.tick(PLENTY, PLENTY).energy() > 0);
    }

    @Test
    void theSavedStateComesBackWithinItsBounds() {
        Reactor r = new Reactor(Layouts.first());
        r.restore(5_000, true, false);
        assertEquals(5_000, r.hull(), E);
        assertTrue(r.running());
        r.restore(1e9, true, true);   // a SCRAMmed reactor never comes back running
        assertEquals(Fission.HULL_CAPACITY, r.hull(), E);
        assertTrue(r.scrammed());
        assertFalse(r.running());
        r.restore(Double.NaN, false, false);
        assertEquals(0, r.hull(), E);
    }

    // ---- wear and channels

    @Test
    void rodsBurnOutAfterAnHourAtFullPowerAndLeaveTheZone() {
        Reactor r = running(Layouts.first());
        for (int i = 1; i < 72_000; i++) assertTrue(r.tick(PLENTY, PLENTY).burntOut().isEmpty());
        Reactor.Tick last = r.tick(PLENTY, PLENTY);
        assertEquals(6, last.burntOut().size());
        assertEquals(Part.fuel(Fuel.LOW_ENRICHED, 1), last.burntOut().get(0).part());
        assertEquals(0, r.tick(PLENTY, PLENTY).energy(), E);
    }

    @Test
    void atHalfPowerARodLastsTwiceAsLong() {
        Reactor r = running(".......", ".CCC...", ".......", "...LS..");   // three control rods: the knob goes down to 40 %
        r.setKnob(0.5);
        for (int i = 1; i < 144_000; i++) assertTrue(r.tick(PLENTY, PLENTY).burntOut().isEmpty());
        assertEquals(1, r.tick(PLENTY, PLENTY).burntOut().size());
    }

    @Test
    void aRodBroughtInWornBurnsOutSooner() {
        Reactor r = running(".......", ".......", ".......", "...S...");
        r.place(3, 4, Part.fuel(Fuel.LOW_ENRICHED, 1), Fission.ROD_LIFE - 10);
        assertEquals(Fission.ROD_LIFE - 10, r.wear(3, 4), E);
        for (int i = 1; i < 10; i++) assertTrue(r.tick(PLENTY, PLENTY).burntOut().isEmpty());
        assertEquals(1, r.tick(PLENTY, PLENTY).burntOut().size());
    }

    @Test
    void theWearAnItemCarriesIsClampedAndOnlyARodKeepsIt() {
        Reactor r = new Reactor(new Zone());
        r.place(1, 1, Part.HEAT_SINK, 500);
        assertEquals(0, r.wear(1, 1), E);
        r.place(1, 2, Part.fuel(Fuel.HIGH_ENRICHED, 2), 1e12);
        assertEquals(1.5 * Fission.ROD_LIFE, r.wear(1, 2), E);   // clamped to the dual assembly's life
        r.place(1, 3, Part.fuel(Fuel.LOW_ENRICHED, 1), -5);
        assertEquals(0, r.wear(1, 3), E);
        r.place(1, 4, Part.fuel(Fuel.LOW_ENRICHED, 1), Double.NaN);
        assertEquals(0, r.wear(1, 4), E);
    }

    @Test
    void aChannelBlankBecomesItsProductAndLeavesTheCell() {
        // four low enriched rods round the blank: it sees 4 a tick at full power, so 6000 takes 1500 ticks
        Reactor r = running(".......", ".......", "...L...", "..LcL..", "...L...");
        for (int i = 1; i < 1500; i++) assertTrue(r.tick(PLENTY, PLENTY).products().isEmpty());
        assertEquals(List.of(new Reactor.Cell(3, 3)), r.tick(PLENTY, PLENTY).products());
        assertNull(r.zone().get(3, 3));
        assertEquals(0, r.channelWork(3, 3), E);
        assertTrue(r.tick(PLENTY, PLENTY).products().isEmpty());
    }

    @Test
    void aChannelBlankKeepsTheIrradiationItsItemCarries() {
        Reactor r = running(".......", ".......", "...L...", "..LcL..", "...L...");
        r.place(3, 3, Part.CHANNEL, 5_996);
        assertEquals(5_996, r.channelWork(3, 3), E);
        assertEquals(1, r.tick(PLENTY, PLENTY).products().size());
        r.place(3, 3, Part.CHANNEL, 1e12);
        assertEquals(Fission.CHANNEL_WORK, r.channelWork(3, 3), E);
        r.place(3, 3, Part.CHANNEL, Double.NaN);
        assertEquals(0, r.channelWork(3, 3), E);
        r.place(3, 3, Part.HEAT_SINK, 3_000);
        assertEquals(0, r.channelWork(3, 3), E);
    }

    @Test
    void anIsotopeBlankTakesTwentyTimesTheLithiumBlanksWork() {
        // the same four rods, 4 a tick: past a lithium blank's work the isotope blank goes on, and turns at its own
        Reactor r = running(".......", ".......", "...L...", "..LcL..", "...L...");
        Part isotope = Part.channel(Fission.ISOTOPE_WORK);
        r.place(3, 3, isotope, Fission.CHANNEL_WORK + 100);
        assertTrue(r.tick(PLENTY, PLENTY).products().isEmpty());
        assertEquals((Fission.CHANNEL_WORK + 104) / Fission.ISOTOPE_WORK, r.channelProgress(3, 3), E);
        r.place(3, 3, isotope, Fission.ISOTOPE_WORK - 6);
        assertTrue(r.tick(PLENTY, PLENTY).products().isEmpty());
        assertEquals(1, r.tick(PLENTY, PLENTY).products().size());
        r.place(3, 3, isotope, 1e12);
        assertEquals(Fission.ISOTOPE_WORK, r.channelWork(3, 3), E);
        assertEquals(1, r.channelProgress(3, 3), E);
    }

    // ---- a pack's parts (PLAN stage 12, step 6)

    @Test
    void aPacksFuelIsAsStrongAsHotAndLastsAsLongAsItSays() {
        Fuel weak = new Fuel("weak", 0.5, 0.5, 100);
        Reactor r = running(".......", ".......", ".......", "...S...");
        r.place(3, 4, Part.fuel(weak, 1));
        Reactor.Tick t = r.tick(PLENTY, PLENTY);
        assertEquals(0.5 * Fission.FE_PER_FLUX, t.energy(), E);   // a lone single rod's flux is its source
        assertEquals(0.5 * Fission.HEAT_PER_FLUX * 0.5, t.heat(), E);
        for (int i = 2; i < 100; i++) assertTrue(r.tick(PLENTY, PLENTY).burntOut().isEmpty());
        assertEquals(1, r.tick(PLENTY, PLENTY).burntOut().size());
        // a worn rod brought in is held to its own fuel's life, not the add-on's hour
        r.place(3, 4, Part.fuel(weak, 1), 1e9);
        assertEquals(100, r.wear(3, 4), E);
    }

    @Test
    void aPacksCellDrinksItsOwnFluidAtItsOwnRate() {
        Cooling nitrogen = new Cooling(200, "pack:liquid_nitrogen", 50, "pack:nitrogen_gas");
        Reactor r = running(".......", ".......", ".......", "...U...");
        r.place(3, 4, Part.outlet(nitrogen));
        assertEquals(Part.Kind.CRYO_CELL, r.zone().get(3, 4).kind());   // it gives a fluid back, as the cryo cell
        assertEquals(List.of(nitrogen), r.drinking());
        // water and Voltex coolant are no use to it
        assertEquals(117.6, r.tick(PLENTY, PLENTY).toHull(), E);
        Reactor.Tick short_ = r.tick(Map.of("pack:liquid_nitrogen", 1.0));
        assertEquals(50, short_.removed(), E);
        assertEquals(1, short_.used("pack:liquid_nitrogen"), E);
        Reactor.Tick full = r.tick(Map.of("pack:liquid_nitrogen", PLENTY));
        assertEquals(117.6, full.removed(), E);
        assertEquals(117.6 / 50, full.used("pack:liquid_nitrogen"), E);
        assertEquals(0, full.coolantMb(), E);
    }

    @Test
    void aPacksHeatSinkTakesItsOwnRate() {
        Reactor r = running(".......", ".......", ".......", "...U...");
        r.place(3, 4, Part.outlet(new Cooling(100, null, 0, null)));
        assertEquals(Part.Kind.HEAT_SINK, r.zone().get(3, 4).kind());
        assertTrue(r.drinking().isEmpty());
        assertEquals(100, r.tick(PLENTY, PLENTY).removed(), E);
    }

    @Test
    void anOutletsKindFollowsItsCooling() {
        assertEquals(Part.Kind.WATER_CELL, Part.outlet(new Cooling(10, "pack:brine", 5, null)).kind());
        assertThrows(IllegalArgumentException.class, () -> new Part(Part.Kind.HEAT_SINK, null, 0, 0, Cooling.WATER_CELL));
        assertThrows(IllegalArgumentException.class, () -> new Part(Part.Kind.GRAPHITE, null, 0, 0, Cooling.SINK));
        assertThrows(IllegalArgumentException.class, () -> new Cooling(10, null, 5, null));
        assertThrows(IllegalArgumentException.class, () -> new Fuel("none", 0, 1, 100));
    }
}
