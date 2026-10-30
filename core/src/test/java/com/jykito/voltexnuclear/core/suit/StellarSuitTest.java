package com.jykito.voltexnuclear.core.suit;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.jykito.voltexnuclear.core.suit.StellarSuit.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StellarSuitTest {

    @Test
    void energyComingInIsSharedEvenlyOverThePieces() {
        assertArrayEquals(new int[]{500, 500, 500, 500}, share(PELLET_PER_TICK, new int[]{CAPACITY, CAPACITY, CAPACITY, CAPACITY}));
    }

    @Test
    void aFullPieceLeavesItsShareToTheOthers() {
        int[] got = share(2_000, new int[]{0, 100, CAPACITY, CAPACITY});
        assertArrayEquals(new int[]{0, 100, 950, 950}, got);
        assertEquals(2_000, Arrays.stream(got).sum());
    }

    @Test
    void moreThanTheRoomFillsTheSuitAndStops() {
        assertArrayEquals(new int[]{10, 20, 0, 5}, share(1_000, new int[]{10, 20, 0, 5}));
    }

    @Test
    void energyGoingOutIsTakenEvenlyAsFarAsEachPieceHas() {
        assertArrayEquals(new int[]{250, 250, 250, 250}, drain(1_000, new int[]{CAPACITY, CAPACITY, CAPACITY, CAPACITY}));
        int[] gave = drain(1_000, new int[]{0, 100, CAPACITY, CAPACITY});
        assertEquals(1_000, Arrays.stream(gave).sum());
        assertEquals(0, gave[0]);
        assertEquals(100, gave[1]);
    }

    @Test
    void theChestplateTakesAPelletOnlyUnderAQuarterWithNoneBurning() {
        long full = (long) CAPACITY * PIECES;
        assertTrue(wantsPellet(full / 4 - 1, full, 0));
        assertFalse(wantsPellet(full / 4, full, 0));
        assertFalse(wantsPellet(0, full, 1));
    }

    @Test
    void aPelletBurnsAnHourAndCarriesThirtySixSuitsWorthOfPieces() {
        assertEquals(72_000, PELLET_TICKS);   // an hour of game ticks
        assertEquals(144_000_000L, (long) PELLET_TICKS * PELLET_PER_TICK);
        assertEquals(36, (long) PELLET_TICKS * PELLET_PER_TICK / CAPACITY);
    }

    @Test
    void eachChargedPieceTurnsAwayAQuarterOfTheDamageAtHalfPlasmasPrice() {
        assertEquals(0f, absorbed(0));
        assertEquals(0.5f, absorbed(2));
        assertEquals(1f, absorbed(4));
        assertEquals(1f, absorbed(9));
        assertEquals(500, damageCost(10f));   // Plasma pays 1 000 for the same
    }

    @Test
    void theSuitPaysOnlyWhatAllItsPiecesHoldTogether() {
        assertTrue(canPay(RUSH_PER_TICK, new int[]{500, 500, 500, 500}));
        assertFalse(canPay(RUSH_PER_TICK, new int[]{500, 500, 500, 499}));
    }
}
