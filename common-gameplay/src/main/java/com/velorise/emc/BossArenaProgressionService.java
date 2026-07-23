package com.velorise.emc;

import com.mojang.datafixers.util.Pair;
import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Map;
import java.util.WeakHashMap;

/** Generates every unlocked boss arena once per server session and catches up older alpha worlds. */
public final class BossArenaProgressionService {
    private static final Map<MinecraftServer, Integer> RECONCILED_TIER = new WeakHashMap<>();

    private BossArenaProgressionService() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 200 != 0) return;
        int tier = WorldProgressionData.get(server).snapshot().worldTier();
        Integer reconciled = RECONCILED_TIER.get(server);
        if (reconciled != null && reconciled >= tier) return;
        ensureUnlockedArenas(server, tier);
        RECONCILED_TIER.put(server, tier);
    }

    public static void ensureUnlockedArenas(MinecraftServer server, int worldTier) {
        if (worldTier >= 1) ensureInfernalArena(server);
        if (worldTier >= 2) ensureAethericArena(server);
        if (worldTier >= 3) ensureDeepboundArena(server);
        if (worldTier >= LastEchoProfile.REQUIRED_WORLD_TIER) ensureLastEchoCitadel(server);
    }


    private static final int[][] MULTI_ARENA_ORIGINS = {
        {0, 0}, {4096, 0}, {-4096, 0}, {0, 4096}, {0, -4096}
    };

    private static void ensureInfernalArena(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return;
        for (int[] offset : MULTI_ARENA_ORIGINS) {
            BlockPos arena = InfernalCrucibleArenaService.locateArena(nether,
                new BlockPos(offset[0], 70, offset[1]));
            if (arena != null && !InfernalCrucibleArenaService.hasArena(nether, arena)) {
                InfernalCrucibleArenaService.buildArena(nether, arena);
            } else if (arena != null) {
                BossArenaRegistryData.get(server).register(BossArenaRegistryData.Kind.INFERNAL, arena);
            }
        }
    }

    private static void ensureAethericArena(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        for (int[] offset : MULTI_ARENA_ORIGINS) {
            BlockPos landmark = locate(end, "end_city", new BlockPos(offset[0], 80, offset[1]), 256);
            if (landmark == null) continue;
            BlockPos arena = landmark.offset(48, 0, 0);
            if (!PromotionBossArenaService.hasBulwarkArena(end, arena)) {
                PromotionBossArenaService.buildBulwarkArena(end, arena);
            } else BossArenaRegistryData.get(server).register(BossArenaRegistryData.Kind.AETHERIC, arena);
        }
    }

    private static void ensureDeepboundArena(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        for (int[] offset : MULTI_ARENA_ORIGINS) {
            BlockPos origin = spawn.offset(offset[0], 0, offset[1]);
            BlockPos landmark = locate(overworld, "ancient_city", origin, 256);
            if (landmark == null) continue;
            BlockPos arena = landmark.offset(48, 2, 0);
            if (!PromotionBossArenaService.hasDeepboundArena(overworld, arena)) {
                PromotionBossArenaService.buildDeepboundArena(overworld, arena);
            } else BossArenaRegistryData.get(server).register(BossArenaRegistryData.Kind.DEEPBOUND, arena);
        }
    }

    private static void ensureLastEchoCitadel(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        BlockPos citadel = LastEchoCitadelService.ensureLocated(server);
        if (citadel == null) return;
        LastEchoCitadelData data = LastEchoCitadelData.get(server);
        if (!data.built()) LastEchoCitadelService.placeNow(end, citadel);
    }

    private static BlockPos locate(ServerLevel level, String structurePath, BlockPos origin, int radiusChunks) {
        ResourceLocation id = ResourceLocation.tryParse("minecraft:" + structurePath);
        if (id == null) return null;
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        java.util.Optional<Holder.Reference<Structure>> holder = registry.getHolder(
            ResourceKey.create(Registries.STRUCTURE, id));
        if (holder.isEmpty()) return null;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(holder.get()), origin, radiusChunks, false);
        return found == null ? null : found.getFirst();
    }
}
