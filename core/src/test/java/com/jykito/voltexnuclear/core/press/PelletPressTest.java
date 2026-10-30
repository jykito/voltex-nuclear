package com.jykito.voltexnuclear.core.press;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PelletPressTest {

    @Test
    void oneRodCostsEightThousandFeAndAFullBufferPressesEight() {
        assertEquals(8_000, PelletPress.energyPerRod());
        assertEquals(8, PelletPress.BUFFER / PelletPress.energyPerRod());
    }
}
