package com.jykito.voltexnuclear.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModInfoTest {

    @Test
    void modIdIsAValidNamespace() {
        assertTrue(ModInfo.MOD_ID.matches("[a-z0-9_.-]+"), "resource namespaces allow only a-z 0-9 _ . -");
    }

    @Test
    void modIdIsNotVoltex() {
        assertNotEquals(ModInfo.VOLTEX_ID, ModInfo.MOD_ID);
    }
}
