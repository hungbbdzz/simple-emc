package com.velorise.emc.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuraNodeProfileTest {
    @AfterEach
    void reset() { GameplayConfig.resetToDefaults(); }

    @Test
    void defaultsUseRewardReserveAndConfiguredSpacing() {
        assertEquals(1_228_800L, AuraNodeProfile.reserve(1));
        assertEquals(78_643_200L, AuraNodeProfile.reserve(4));
        assertEquals(2_048L, AuraNodeProfile.outputPerPlayer(1));
        assertEquals(131_072L, AuraNodeProfile.outputPerPlayer(4));
        assertEquals(12_000, AuraNodeProfile.rechargeTicks(1));
        assertEquals(12_000, AuraNodeProfile.rechargeTicks(4));
        assertEquals(600, AuraNodeProfile.spacingBlocks(1));
        assertEquals(2_000, AuraNodeProfile.spacingBlocks(4));
        assertEquals(0.50D, AuraNodeProfile.ancientCitySpawnChance());
    }

    @Test
    void pulseAmountPreservesPerSecondRate() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.world.auraNodes.pulseIntervalTicks = 40;
        GameplayConfig.install(values);
        assertEquals(4_096L, AuraNodeProfile.outputPerPlayerPerPulse(1));
        assertEquals(2.0F, AuraNodeProfile.siphonDamagePerPulse(1));
    }
}
