package com.velorise.emc.common;

public final class WirelessCoreProfileTest {
    private WirelessCoreProfileTest() {}

    public static void main(String[] args) {
        assert WirelessCoreProfile.nextTier(WirelessCoreTier.I).orElseThrow() == WirelessCoreTier.II;
        assert WirelessCoreProfile.nextTier(WirelessCoreTier.II).orElseThrow() == WirelessCoreTier.III;
        assert WirelessCoreProfile.nextTier(WirelessCoreTier.III).orElseThrow() == WirelessCoreTier.IV;
        assert WirelessCoreProfile.nextTier(WirelessCoreTier.IV).isEmpty();

        assert WirelessCoreProfile.isDirectUpgrade(WirelessCoreTier.I, WirelessCoreTier.II);
        assert !WirelessCoreProfile.isDirectUpgrade(WirelessCoreTier.I, WirelessCoreTier.III);
        assert WirelessCoreProfile.condensationUnits(WirelessCoreTier.II) == 9;
        assert WirelessCoreProfile.condensationUnits(WirelessCoreTier.IV) == 9;

        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        values.progression.worldTiers.scalingMode = "NONE";
        GameplayConfig.install(values);

        long[] counts = new long[WorldProgressionObjective.values().length];
        completeStage(counts, WorldTierStage.OVERWORLD_HUNT);
        WorldProgressionState state = new WorldProgressionState(counts, new int[]{1, 0, 0, 0});
        assert WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.I);
        assert !WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.II);

        completeStage(counts, WorldTierStage.NETHER_CONQUEST);
        state = new WorldProgressionState(counts, new int[]{1, 1, 0, 0});
        assert WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.II);
        assert !WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.III);

        completeStage(counts, WorldTierStage.END_CONQUEST);
        state = new WorldProgressionState(counts, new int[]{1, 1, 1, 0});
        assert WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.III);
        assert !WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.IV);

        completeStage(counts, WorldTierStage.DEEP_WORLD_CONQUEST);
        state = new WorldProgressionState(counts, new int[]{1, 1, 1, 1});
        assert WirelessCoreProfile.isUnlocked(state, WirelessCoreTier.IV);
        GameplayConfig.resetToDefaults();
    }

    private static void completeStage(long[] counts, WorldTierStage stage) {
        for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
            if (objective.stage() == stage) {
                counts[objective.ordinal()] = WorldProgressionProfile.baseRequirement(objective);
            }
        }
    }
}
