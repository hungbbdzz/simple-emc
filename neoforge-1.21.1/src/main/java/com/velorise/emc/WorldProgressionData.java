package com.velorise.emc;

import com.velorise.emc.common.WorldProgressionObjective;
import com.velorise.emc.common.WorldProgressionState;
import com.velorise.emc.common.WorldTierStage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** World-global objective counters and unique contributors for the Tier 0-IV progression. */
public final class WorldProgressionData extends SavedData {
    private static final String DATA_NAME = "velorise_emc_world_progression";
    private static final String COUNTS = "ObjectiveCounts";
    private static final String CONTRIBUTORS = "Contributors";

    private final long[] counts = new long[WorldProgressionObjective.values().length];
    @SuppressWarnings("unchecked")
    private final Set<UUID>[] contributors = new Set[WorldTierStage.values().length];
    private final java.util.Map<String, Integer> blessedChunks = new java.util.HashMap<>();

    public WorldProgressionData() {
        for (int i = 0; i < contributors.length; i++) contributors[i] = new LinkedHashSet<>();
    }

    public int getBlessedTier(String key) {
        return blessedChunks.getOrDefault(key, 0);
    }

    public void setBlessedTier(String key, int tier) {
        blessedChunks.put(key, tier);
        setDirty();
    }

    public static WorldProgressionData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldProgressionData data = new WorldProgressionData();
        long[] savedCounts = tag.getLongArray(COUNTS);
        for (int i = 0; i < Math.min(savedCounts.length, data.counts.length); i++) {
            data.counts[i] = Math.max(0L, savedCounts[i]);
        }
        CompoundTag contributorTag = tag.getCompound(CONTRIBUTORS);
        for (WorldTierStage stage : WorldTierStage.values()) {
            ListTag list = contributorTag.getList(Integer.toString(stage.index()), Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    data.contributors[stage.index()].add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {
                    // Alpha data is intentionally strict: malformed entries are discarded.
                }
            }
        }
        CompoundTag blessedTag = tag.getCompound("BlessedChunks");
        for (String key : blessedTag.getAllKeys()) {
            data.blessedChunks.put(key, blessedTag.getInt(key));
        }
        return data;
    }

    public static WorldProgressionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(WorldProgressionData::new, WorldProgressionData::load),
            DATA_NAME
        );
    }

    public WorldProgressionState snapshot() {
        int[] contributorCounts = new int[contributors.length];
        for (int i = 0; i < contributors.length; i++) contributorCounts[i] = contributors[i].size();
        return new WorldProgressionState(counts, contributorCounts);
    }

    public boolean record(UUID playerId, WorldProgressionObjective... objectives) {
        if (playerId == null || objectives == null || objectives.length == 0) return false;
        WorldTierStage stage = objectives[0].stage();
        boolean changed = contributors[stage.index()].add(playerId);
        for (WorldProgressionObjective objective : objectives) {
            if (objective == null || objective.stage() != stage) continue;
            int index = objective.ordinal();
            long current = counts[index];
            if (current < Long.MAX_VALUE) {
                counts[index] = current + 1L;
                changed = true;
            }
        }
        if (changed) setDirty();
        return changed;
    }

    public void reset() {
        java.util.Arrays.fill(counts, 0L);
        for (Set<UUID> contributorSet : contributors) contributorSet.clear();
        blessedChunks.clear();
        setDirty();
    }

    public void grantTier(UUID contributor, int tier) {
        int clamped = Math.max(0, Math.min(4, tier));
        for (WorldTierStage stage : WorldTierStage.values()) {
            if (stage.index() >= clamped) continue;
            if (contributor != null) contributors[stage.index()].add(contributor);
            for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
                if (objective.stage() == stage) counts[objective.ordinal()] = Long.MAX_VALUE;
            }
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray(COUNTS, counts);
        CompoundTag contributorTag = new CompoundTag();
        for (WorldTierStage stage : WorldTierStage.values()) {
            ListTag list = new ListTag();
            for (UUID uuid : contributors[stage.index()]) list.add(StringTag.valueOf(uuid.toString()));
            contributorTag.put(Integer.toString(stage.index()), list);
        }
        tag.put(CONTRIBUTORS, contributorTag);
        CompoundTag blessedTag = new CompoundTag();
        for (java.util.Map.Entry<String, Integer> entry : blessedChunks.entrySet()) {
            blessedTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("BlessedChunks", blessedTag);
        return tag;
    }
}
