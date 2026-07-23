package com.velorise.emc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CondensedCoreGeneratorProfileTest {
    @Test
    void rewardCurveUsesNinefoldOutput() {
        assertEquals(3, CondensedCoreGeneratorProfile.radius(1));
        assertEquals(5, CondensedCoreGeneratorProfile.radius(2));
        assertEquals(7, CondensedCoreGeneratorProfile.radius(3));
        assertEquals(9, CondensedCoreGeneratorProfile.radius(4));
        assertEquals(512L, CondensedCoreGeneratorProfile.emcPerSecond(1));
        assertEquals(4_608L, CondensedCoreGeneratorProfile.emcPerSecond(2));
        assertEquals(41_472L, CondensedCoreGeneratorProfile.emcPerSecond(3));
        assertEquals(373_248L, CondensedCoreGeneratorProfile.emcPerSecond(4));
    }
}
