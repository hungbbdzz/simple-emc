package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent registry for every non-unique boss arena. Last Echo is intentionally excluded. */
public final class BossArenaRegistryData extends SavedData {
    public enum Kind { INFERNAL, AETHERIC, DEEPBOUND }
    private static final String DATA_NAME = "velorise_emc_boss_arena_registry";
    private final List<BlockPos> infernal = new ArrayList<>();
    private final List<BlockPos> aetheric = new ArrayList<>();
    private final List<BlockPos> deepbound = new ArrayList<>();

    public static BossArenaRegistryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(BossArenaRegistryData::new, BossArenaRegistryData::load), DATA_NAME);
    }

    public static BossArenaRegistryData load(CompoundTag tag, HolderLookup.Provider registries) {
        BossArenaRegistryData data = new BossArenaRegistryData();
        data.read(tag, "Infernal", data.infernal);
        data.read(tag, "Aetheric", data.aetheric);
        data.read(tag, "Deepbound", data.deepbound);
        return data;
    }

    public void register(Kind kind, BlockPos center) {
        List<BlockPos> list = mutable(kind);
        BlockPos value = center.immutable();
        for (BlockPos existing : list) if (existing.equals(value)) return;
        list.add(value);
        setDirty();
    }

    public List<BlockPos> positions(Kind kind) { return Collections.unmodifiableList(mutable(kind)); }

    public BlockPos nearest(Kind kind, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : mutable(kind)) {
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        return best;
    }

    private List<BlockPos> mutable(Kind kind) {
        return switch (kind) {
            case INFERNAL -> infernal;
            case AETHERIC -> aetheric;
            case DEEPBOUND -> deepbound;
        };
    }

    private void read(CompoundTag tag, String key, List<BlockPos> target) {
        ListTag list = tag.getList(key, 4);
        for (int i = 0; i < list.size(); i++) target.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
    }

    private void write(CompoundTag tag, String key, List<BlockPos> source) {
        ListTag list = new ListTag();
        for (BlockPos pos : source) list.add(LongTag.valueOf(pos.asLong()));
        tag.put(key, list);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag, "Infernal", infernal);
        write(tag, "Aetheric", aetheric);
        write(tag, "Deepbound", deepbound);
        return tag;
    }
}
