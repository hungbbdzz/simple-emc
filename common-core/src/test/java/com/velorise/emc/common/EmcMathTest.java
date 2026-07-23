package com.velorise.emc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmcMathTest {
    @Test
    void addsNormally() {
        assertEquals(150L, EmcMath.saturatingAdd(100L, 50L, 1_000L));
    }

    @Test
    void clampsAtMaximumWithoutOverflow() {
        assertEquals(Long.MAX_VALUE, EmcMath.saturatingAdd(Long.MAX_VALUE - 5L, 10L, Long.MAX_VALUE));
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> EmcMath.saturatingAdd(0L, -1L, 100L));
    }
}
