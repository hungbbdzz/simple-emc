package com.velorise.emc.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldProgressionStateTest {
    @AfterEach
    void resetConfig() {
        GameplayConfig.resetToDefaults();
    }

    @Test
    void tierZeroStartsWithoutAnyCoreUnlocked() {
        WorldProgressionState state = WorldProgressionState.INITIAL;
        assertEquals(0, state.worldTier());
        assertFalse(state.isUnlocked(WirelessCoreTier.I));
        assertEquals(WorldTierStage.OVERWORLD_HUNT, state.activeStage());
    }

    @Test
    void sequentialStagesUnlockMatchingCoreTiers() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.progression.worldTiers.overworld.totalKills = 1;
        values.progression.worldTiers.overworld.zombieFamily = 0;
        values.progression.worldTiers.overworld.skeletonFamily = 0;
        values.progression.worldTiers.overworld.spiderFamily = 0;
        values.progression.worldTiers.overworld.creeper = 0;
        values.progression.worldTiers.overworld.enderman = 0;
        values.progression.worldTiers.overworld.witch = 0;
        values.progression.worldTiers.overworld.illager = 0;
        GameplayConfig.install(values);

        long[] counts = new long[WorldProgressionObjective.values().length];
        counts[WorldProgressionObjective.OVERWORLD_TOTAL.ordinal()] = 1;
        WorldProgressionState state = new WorldProgressionState(counts, new int[]{1, 0, 0, 0});
        assertEquals(1, state.worldTier());
        assertTrue(state.isUnlocked(WirelessCoreTier.I));
        assertFalse(state.isUnlocked(WirelessCoreTier.II));
    }

    @Test
    void multiplicativeScalingUsesUniqueContributorCountAndCap() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.progression.worldTiers.perAdditionalPlayerMultiplier = 2.0D;
        values.progression.worldTiers.maximumScale = 3.0D;
        values.progression.worldTiers.overworld.zombieFamily = 10L;
        GameplayConfig.install(values);

        long[] counts = new long[WorldProgressionObjective.values().length];
        WorldProgressionState state = new WorldProgressionState(counts, new int[]{3, 0, 0, 0});
        assertEquals(30L, state.required(WorldProgressionObjective.ZOMBIE_FAMILY));
    }

    @Test
    void bossObjectivesRemainFixedUnlessServerEnablesScaling() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.progression.worldTiers.perAdditionalPlayerMultiplier = 2.0D;
        values.progression.worldTiers.nether.wither = 1L;
        GameplayConfig.install(values);
        WorldProgressionState state = new WorldProgressionState(null, new int[]{0, 4, 0, 0});
        assertEquals(1L, state.required(WorldProgressionObjective.WITHER));

        values.progression.worldTiers.scaleBossObjectives = true;
        GameplayConfig.install(values);
        state = new WorldProgressionState(null, new int[]{0, 4, 0, 0});
        assertEquals(3L, state.required(WorldProgressionObjective.WITHER));
    }

    @Test
    void coreModeIsClampedByCoreTier() {
        assertEquals(1, CoreModeRules.effectiveMode(4, WirelessCoreTier.I));
        assertEquals(3, CoreModeRules.effectiveMode(4, WirelessCoreTier.III));
        assertEquals(4, CoreModeRules.effectiveMode(4, WirelessCoreTier.IV));
    }
}
