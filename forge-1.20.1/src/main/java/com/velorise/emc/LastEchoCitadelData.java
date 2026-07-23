package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One global Citadel of the Last Echo per world. Alpha data intentionally has no legacy migration. */
public final class LastEchoCitadelData extends SavedData {
    private static final String DATA_NAME = "velorise_emc_last_echo_citadel";
    private BlockPos center;
    private int buildCursor;
    private boolean built;
    private boolean bossActive;
    private boolean defeated;

    public static LastEchoCitadelData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            LastEchoCitadelData::load, LastEchoCitadelData::new, DATA_NAME
        );
    }

    public static LastEchoCitadelData load(CompoundTag tag) {
        LastEchoCitadelData data = new LastEchoCitadelData();
        if (tag.contains("Center")) data.center = BlockPos.of(tag.getLong("Center"));
        data.buildCursor = Math.max(0, tag.getInt("BuildCursor"));
        data.built = tag.getBoolean("Built");
        data.bossActive = tag.getBoolean("BossActive");
        data.defeated = tag.getBoolean("Defeated");
        return data;
    }

    public BlockPos center() { return center; }
    public int buildCursor() { return buildCursor; }
    public boolean built() { return built; }
    public boolean bossActive() { return bossActive; }
    public boolean defeated() { return defeated; }

    public void locate(BlockPos value) { center = value.immutable(); setDirty(); }
    public void setBuildCursor(int value) { buildCursor = Math.max(0, value); setDirty(); }
    public void markBuilt() { built = true; buildCursor = 0; setDirty(); }
    public void setBossActive(boolean value) { bossActive = value; setDirty(); }
    public void markDefeated() { defeated = true; bossActive = false; setDirty(); }
    public void resetAt(BlockPos value) {
        center = value.immutable(); buildCursor = 0; built = false; bossActive = false; defeated = false; setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (center != null) tag.putLong("Center", center.asLong());
        tag.putInt("BuildCursor", buildCursor);
        tag.putBoolean("Built", built);
        tag.putBoolean("BossActive", bossActive);
        tag.putBoolean("Defeated", defeated);
        return tag;
    }
}
