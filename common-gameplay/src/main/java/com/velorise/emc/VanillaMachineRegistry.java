package com.velorise.emc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Persistent per-player list of account-bound vanilla blocks upgraded with a Wireless Core. */
public final class VanillaMachineRegistry {
    public static final String ROOT = "VeloriseEMCMachines";
    public static final String ANVIL_FLAG = "VeloriseEMCUsingAlchemicalAnvil";

    public static final String FURNACE = "alchemical_furnace";

    private VanillaMachineRegistry() {}

    public record Entry(String dimension, long pos, String type, int coreTier) {
        public BlockPos blockPos() { return BlockPos.of(pos); }
    }

    public static boolean contains(ServerPlayer player, Level level, BlockPos pos, String type) {
        String dim = level.dimension().location().toString();
        for (Entry entry : getEntries(player)) {
            if (entry.pos == pos.asLong() && entry.dimension.equals(dim) && entry.type.equals(type)) return true;
        }
        return false;
    }

    public static boolean add(ServerPlayer player, Level level, BlockPos pos, String type, int coreTier) {
        if (contains(player, level, pos, type)) return false;
        CompoundTag root = player.getPersistentData();
        ListTag list = root.getList(ROOT, Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.putString("Dim", level.dimension().location().toString());
        entry.putLong("Pos", pos.asLong());
        entry.putString("Type", type);
        entry.putInt("CoreTier", clampCoreTier(coreTier));
        list.add(entry);
        root.put(ROOT, list);
        return true;
    }

    public static List<Entry> getEntries(ServerPlayer player) {
        List<Entry> result = new ArrayList<>();
        ListTag list = player.getPersistentData().getList(ROOT, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String dim = tag.getString("Dim");
            String type = tag.getString("Type");
            if (!dim.isBlank() && !type.isBlank()) {
                result.add(new Entry(dim, tag.getLong("Pos"), type, clampCoreTier(tag.getInt("CoreTier"))));
            }
        }
        return result;
    }

    public static void remove(ServerPlayer player, Entry target) {
        ListTag old = player.getPersistentData().getList(ROOT, Tag.TAG_COMPOUND);
        ListTag next = new ListTag();
        for (int i = 0; i < old.size(); i++) {
            CompoundTag tag = old.getCompound(i);
            if (tag.getLong("Pos") == target.pos
                && tag.getString("Dim").equals(target.dimension)
                && tag.getString("Type").equals(target.type)) {
                continue;
            }
            next.add(tag.copy());
        }
        player.getPersistentData().put(ROOT, next);
    }

    public static Entry find(ServerPlayer player, Level level, BlockPos pos, String type) {
        String dim = level.dimension().location().toString();
        for (Entry entry : getEntries(player)) {
            if (entry.pos == pos.asLong() && entry.dimension.equals(dim) && entry.type.equals(type)) return entry;
        }
        return null;
    }

    public static void copyMachines(ServerPlayer original, ServerPlayer replacement) {
        ListTag list = original.getPersistentData().getList(ROOT, Tag.TAG_COMPOUND);
        replacement.getPersistentData().put(ROOT, list.copy());
    }

    public static ResourceLocation parseDimension(Entry entry) {
        return ResourceLocation.tryParse(entry.dimension);
    }

    private static int clampCoreTier(int coreTier) {
        return Math.max(1, Math.min(4, coreTier));
    }
}
