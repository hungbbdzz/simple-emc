package com.velorise.emc.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameplayConfigTest {
    @AfterEach
    void reset() {
        GameplayConfig.resetToDefaults();
    }

    @Test
    void defaultsMatchCurrentBalance() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        GameplayConfig.install(values);

        assertEquals(2_000_000L, HourglassProfile.maxEmc());
        assertEquals(16, ResonantMachineProfile.furnaceSpeedMultiplier(4));
        assertEquals(16.0F, UtilityItemProfile.cursedIgniterDamage(4));
        assertEquals(32, MatterburstProfile.radius(4));
        assertEquals(1_024L, CustomUtilityProfile.translocatorCost());
        assertEquals(0.5D, CustomUtilityProfile.magicMirrorEmcPerBlock(4));
        assertEquals(2_048L, FixedUtilityProfile.aquaticActivationCost());
        assertEquals(8_096L, PurificationProfile.operationCost(3));
        assertEquals(78_643_200L, AuraNodeProfile.reserve(4));
        assertEquals(131_072L, AuraNodeProfile.outputPerPlayer(4));
        assertEquals(0.50D, AuraNodeProfile.ancientCitySpawnChance());
        assertEquals(1_000.0D, ThreatScalingProfile.bossBonusHealth(10));
        assertEquals(1L, GameplayConfig.current().economy.transmutationTax.recoveryPointsPerSecond);
        assertEquals(64L, GameplayConfig.current().economy.transmutationTax.pointThresholds[0]);
        assertEquals(64L, GameplayConfig.current().economy.transmutationTax.extraEmcPerItem[5]);
    }

    @Test
    void installedValuesDriveProfilesImmediately() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.machines.hourglass.maxEmc = 500_000L;
        values.machines.hourglass.radius = new int[]{4, 6, 8, 12};
        values.machines.resonantFurnace.speedMultiplier = new int[]{1, 2, 8, 32};
        values.items.cursedIgniter.damage = new float[]{3.0F, 6.0F, 9.0F, 24.0F};
        values.items.aethericElytra.boostImpulse = new double[]{0.5D, 1.0D, 1.5D, 2.5D};
        values.items.aethericElytra.boostCooldownTicks = 25;
        values.items.general.recoveryMovementTolerance = 1.25D;
        values.items.general.recoveryHorizontalSearchRadius = 6;
        values.items.general.recoveryVerticalSearchRadius = 12;
        values.world.auraNodes.reserve = new long[]{1L, 2L, 3L, 4L};
        values.world.auraNodes.ancientCitySpawnChance = 0.75D;
        values.world.bossPlayerScaling.maxCountedPlayers = 3;
        values.world.mobThreatScaling.healthPerTier = 7.0D;
        values.economy.transmutationTax.recoveryPointsPerSecond = 4L;
        values.economy.transmutationTax.extraEmcPerItem = new long[]{0L, 1L, 3L, 7L, 15L, 31L};
        GameplayConfig.install(values);

        assertEquals(500_000L, HourglassProfile.maxEmc());
        assertEquals(12, HourglassProfile.radius(4));
        assertEquals(32, ResonantMachineProfile.furnaceSpeedMultiplier(4));
        assertEquals(24.0F, UtilityItemProfile.cursedIgniterDamage(4));
        assertEquals(2.5D, UtilityItemProfile.elytraBoostImpulse(4));
        assertEquals(25, UtilityItemProfile.elytraBoostCooldownTicks());
        assertEquals(1.25D, UtilityItemProfile.recoveryMovementTolerance());
        assertEquals(6, UtilityItemProfile.recoveryHorizontalSearchRadius());
        assertEquals(12, UtilityItemProfile.recoveryVerticalSearchRadius());
        assertEquals(4L, AuraNodeProfile.reserve(4));
        assertEquals(0.75D, AuraNodeProfile.ancientCitySpawnChance());
        assertEquals(300.0D, ThreatScalingProfile.bossBonusHealth(99));
        assertEquals(4L, GameplayConfig.current().economy.transmutationTax.recoveryPointsPerSecond);
        assertEquals(31L, GameplayConfig.current().economy.transmutationTax.extraEmcPerItem[5]);
    }

    @Test
    void sanitizeRepairsMissingArraysAndClampsDangerousValues() {
        GameplayConfig.Values values = new GameplayConfig.Values();
        values.machines.hourglass.radius = new int[]{0, 999};
        values.machines.matterburst.maxBlocksPerTick = Integer.MAX_VALUE;
        values.items.cursedIgniter.damage = new float[]{-5.0F, Float.NaN};
        values.items.general.recoveryMovementTolerance = Double.POSITIVE_INFINITY;
        values.items.general.recoveryHorizontalSearchRadius = 999;
        values.items.general.recoveryVerticalSearchRadius = -5;
        values.items.customUtilities = null;
        values.world.auraNodes.spacingBlocks = new int[]{0, 200_000};
        values.world.auraNodes.ancientCitySpawnChance = 4.0D;
        values.world.bossPlayerScaling.maxCountedPlayers = -1;
        values.world.mobThreatScaling.excludedEntities = new String[]{" MINECRAFT:ZOMBIE ", "bad id", "minecraft:zombie"};
        values.economy.transmutationTax.recoveryPointsPerSecond = 0L;
        values.economy.transmutationTax.pointThresholds = new long[]{999_999L, 1L, 1L, 1L, 1L};
        values.economy.transmutationTax.extraEmcPerItem = new long[]{-1L};

        GameplayConfig.Values sanitized = GameplayConfig.sanitize(values);

        assertEquals(4, sanitized.machines.hourglass.radius.length);
        assertEquals(1, sanitized.machines.hourglass.radius[0]);
        assertEquals(128, sanitized.machines.hourglass.radius[1]);
        assertEquals(16_384, sanitized.machines.matterburst.maxBlocksPerTick);
        assertEquals(0.0F, sanitized.items.cursedIgniter.damage[0]);
        assertEquals(0.0F, sanitized.items.cursedIgniter.damage[1]);
        assertEquals(0.0D, sanitized.items.general.recoveryMovementTolerance);
        assertEquals(32, sanitized.items.general.recoveryHorizontalSearchRadius);
        assertEquals(0, sanitized.items.general.recoveryVerticalSearchRadius);
        assertNotNull(sanitized.items.customUtilities);
        assertEquals(128, sanitized.world.auraNodes.spacingBlocks[0]);
        assertEquals(100_000, sanitized.world.auraNodes.spacingBlocks[1]);
        assertEquals(1.0D, sanitized.world.auraNodes.ancientCitySpawnChance);
        assertEquals(0, sanitized.world.bossPlayerScaling.maxCountedPlayers);
        assertEquals(1, sanitized.world.mobThreatScaling.excludedEntities.length);
        assertEquals("minecraft:zombie", sanitized.world.mobThreatScaling.excludedEntities[0]);
        assertEquals(1L, sanitized.economy.transmutationTax.recoveryPointsPerSecond);
        assertEquals(999_996L, sanitized.economy.transmutationTax.pointThresholds[0]);
        assertEquals(1_000_000L, sanitized.economy.transmutationTax.pointThresholds[4]);
        assertEquals(0L, sanitized.economy.transmutationTax.extraEmcPerItem[0]);
        assertEquals(64L, sanitized.economy.transmutationTax.extraEmcPerItem[5]);
    }
}
