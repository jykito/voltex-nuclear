package com.jykito.voltexnuclear.core.radiochem;

import com.jykito.voltexnuclear.core.radiochem.Radiochem.Product;
import com.jykito.voltexnuclear.core.radiochem.Radiochem.Shares;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RadiochemTest {

    /** {low enriched, plutonium, depleted, palladium} out of this many single rods, one at a time. */
    private static int[] outOf(Shares s, int rods) {
        int[] total = new int[Product.values().length];
        for (int i = 0; i < rods; i++) {
            int[] out = s.add(1);
            for (int k = 0; k < total.length; k++) total[k] += out[k];
        }
        return total;
    }

    @Test
    void fourRodsGiveOneUraniumOnePlutoniumAndTwoDepleted() {
        assertArrayEquals(new int[]{1, 1, 2, 0}, outOf(new Shares(), 4));
    }

    @Test
    void sixteenRodsGiveOnePalladium() {
        assertArrayEquals(new int[]{4, 4, 8, 1}, outOf(new Shares(), 16));
    }

    @Test
    void nothingComesOutBeforeItsShareIsFull() {
        Shares s = new Shares();
        assertArrayEquals(new int[]{0, 0, 0, 0}, s.add(1));
        assertArrayEquals(new int[]{0, 0, 1, 0}, s.add(1));
        assertEquals(2, s.get(Product.LOW_ENRICHED));
        assertEquals(0, s.get(Product.DEPLETED));
        assertEquals(2, s.get(Product.PALLADIUM));
    }

    @Test
    void anAssemblyCountsAsItsRodsAndKeepsTheRest() {
        Shares quad = new Shares();
        assertArrayEquals(new int[]{1, 1, 2, 0}, quad.add(4));
        Shares mixed = new Shares();
        mixed.add(3);
        // 3 + 2 rods: the quarter products come out at the 4th rod, and one depleted with them
        assertArrayEquals(new int[]{1, 1, 1, 0}, mixed.add(2));
        assertEquals(1, mixed.get(Product.PLUTONIUM));
        assertEquals(1, mixed.get(Product.DEPLETED));
    }

    @Test
    void sharesSurviveASaveAndBadOnesStartFromNothing() {
        Shares s = new Shares();
        s.add(3);
        Shares back = new Shares();
        back.load(s.save());
        assertArrayEquals(s.save(), back.save());
        back.load(new int[]{4, -1, 1});
        assertArrayEquals(new int[]{0, 0, 1, 0}, back.save(), "a whole share, a negative one and a missing one are dropped");
        back.load(null);
        assertArrayEquals(new int[]{0, 0, 0, 0}, back.save());
    }

    @Test
    void timeWaterAndEnergyGoByTheRod() {
        assertEquals(200, Radiochem.ticks(1));
        assertEquals(800, Radiochem.ticks(4));
        assertEquals(4_000, Radiochem.water(4));
        assertEquals(40_000, Radiochem.energyPerRod());
    }
}
