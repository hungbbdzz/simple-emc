package com.velorise.emc.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreatScalingProfileTest {
    @AfterEach
    void reset() { GameplayConfig.resetToDefaults(); }

    @Test
    void bossScalingIncludesFirstPlayerAndUsesDefaultCap() {
        assertEquals(100.0D, ThreatScalingProfile.bossBonusHealth(1));
        assertEquals(2.0F, ThreatScalingProfile.bossBonusDamage(1));
        assertEquals(1_000.0D, ThreatScalingProfile.bossBonusHealth(20));
        assertEquals(20.0F, ThreatScalingProfile.bossBonusDamage(20));
    }

    @Test
    void worldThreatTracksCompletedWorldTier() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.progression.worldTiers.scalingMode = "NONE";
        GameplayConfig.install(values);

        long[] counts = new long[WorldProgressionObjective.values().length];
        completeStage(counts, WorldTierStage.OVERWORLD_HUNT);
        WorldProgressionState tierOne = new WorldProgressionState(counts, new int[]{1, 0, 0, 0});
        assertEquals(7.0D, ThreatScalingProfile.mobBonusHealth(20.0D, tierOne), 0.0001D);
        assertEquals(1.20F, ThreatScalingProfile.mobDamageMultiplier(tierOne));
        assertEquals(2.0D, ThreatScalingProfile.mobBonusArmor(tierOne));

        completeStage(counts, WorldTierStage.NETHER_CONQUEST);
        completeStage(counts, WorldTierStage.END_CONQUEST);
        completeStage(counts, WorldTierStage.DEEP_WORLD_CONQUEST);
        WorldProgressionState tierFour = new WorldProgressionState(counts, new int[]{1, 1, 1, 1});
        assertEquals(44.0D, ThreatScalingProfile.mobBonusHealth(20.0D, tierFour), 0.0001D);
        assertEquals(2.30F, ThreatScalingProfile.mobDamageMultiplier(tierFour));
        assertEquals(15.0D, ThreatScalingProfile.mobBonusArmor(tierFour));
        assertEquals(0.20D, ThreatScalingProfile.mobBonusKnockbackResistance(tierFour));
    }

    private static void completeStage(long[] counts, WorldTierStage stage) {
        for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
            if (objective.stage() == stage) {
                counts[objective.ordinal()] = WorldProgressionProfile.baseRequirement(objective);
            }
        }
    }
}
