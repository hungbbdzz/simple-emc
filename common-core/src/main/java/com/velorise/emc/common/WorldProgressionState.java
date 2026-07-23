package com.velorise.emc.common;

import java.util.Arrays;

/** Immutable world-global hunt counters controlling World Tier, Core access and World Threat. */
public final class WorldProgressionState {
    private static final int OBJECTIVE_COUNT = WorldProgressionObjective.values().length;
    private static final int STAGE_COUNT = WorldTierStage.values().length;

    public static final WorldProgressionState INITIAL = new WorldProgressionState(
        new long[OBJECTIVE_COUNT], new int[STAGE_COUNT]);

    private final long[] counts;
    private final int[] contributors;

    public WorldProgressionState(long[] counts, int[] contributors) {
        this.counts = normalizedCounts(counts);
        this.contributors = normalizedContributors(contributors);
    }

    private static long[] normalizedCounts(long[] source) {
        long[] out = new long[OBJECTIVE_COUNT];
        if (source != null) {
            for (int i = 0; i < Math.min(source.length, out.length); i++) out[i] = Math.max(0L, source[i]);
        }
        return out;
    }

    private static int[] normalizedContributors(int[] source) {
        int[] out = new int[STAGE_COUNT];
        if (source != null) {
            for (int i = 0; i < Math.min(source.length, out.length); i++) out[i] = Math.max(0, source[i]);
        }
        return out;
    }

    public long count(WorldProgressionObjective objective) {
        return counts[objective.ordinal()];
    }

    public int contributorCount(WorldTierStage stage) {
        return contributors[stage.index()];
    }

    public long required(WorldProgressionObjective objective) {
        return WorldProgressionProfile.required(objective, contributorCount(objective.stage()));
    }

    public boolean objectiveComplete(WorldProgressionObjective objective) {
        long required = required(objective);
        return required <= 0L || count(objective) >= required;
    }

    public boolean stageComplete(WorldTierStage stage) {
        for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
            if (objective.stage() == stage && !objectiveComplete(objective)) return false;
        }
        return true;
    }

    /** Current completed World Tier. Tier 0 has no Core; Tier 1-IV unlock Core I-IV. */
    public int worldTier() {
        if (!stageComplete(WorldTierStage.OVERWORLD_HUNT)) return 0;
        if (!stageComplete(WorldTierStage.NETHER_CONQUEST)) return 1;
        if (!stageComplete(WorldTierStage.END_CONQUEST)) return 2;
        if (!stageComplete(WorldTierStage.DEEP_WORLD_CONQUEST)) return 3;
        return 4;
    }

    public WorldTierStage activeStage() {
        int tier = worldTier();
        return tier >= 4 ? null : WorldTierStage.byIndex(tier);
    }

    public double stageProgress(WorldTierStage stage) {
        double total = 0.0D;
        int objectives = 0;
        for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
            if (objective.stage() != stage) continue;
            long required = required(objective);
            if (required <= 0L) continue;
            total += Math.min(1.0D, (double)count(objective) / (double)required);
            objectives++;
        }
        return objectives == 0 ? 1.0D : total / objectives;
    }

    public boolean isUnlocked(TechnologyTier tier) {
        return tier != null && worldTier() >= tier.id();
    }

    public boolean isUnlocked(WirelessCoreTier coreTier) {
        return coreTier != null && worldTier() >= coreTier.level();
    }

    /** Legacy display helper. Callers that need Tier 0 must use {@link #worldTier()}. */
    public TechnologyTier highestUnlockedTier() {
        return TechnologyTier.byId(Math.max(1, worldTier()));
    }

    public long[] countsCopy() { return Arrays.copyOf(counts, counts.length); }
    public int[] contributorsCopy() { return Arrays.copyOf(contributors, contributors.length); }
}
