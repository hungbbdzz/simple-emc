package com.velorise.emc.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class EffectConsumableProfileTest {
    @AfterEach void reset() { GameplayConfig.resetToDefaults(); }

    @Test void purificationKeepsItsFixedCost() {
        assertEquals(4_096L, EffectConsumableProfile.purificationCost(4));
    }

    @Test void coreFourIgnoresEffectLevel() {
        assertTrue(EffectConsumableProfile.acceptsEffectLevel(1, 0));
        assertFalse(EffectConsumableProfile.acceptsEffectLevel(1, 1));
        assertTrue(EffectConsumableProfile.acceptsEffectLevel(3, 2));
        assertFalse(EffectConsumableProfile.acceptsEffectLevel(3, 3));
        assertTrue(EffectConsumableProfile.acceptsEffectLevel(4, 127));
    }
}
