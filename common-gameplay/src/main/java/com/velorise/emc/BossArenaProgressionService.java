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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Incremental boss-arena discovery and construction.
 *
 * <p>Structure searches are serialized and construction is split into bounded slices. Distant
 * arenas are recorded but not built until a player is close enough for the affected chunks to be
 * naturally loaded. This prevents dimension entry from combining several structure searches and
 * tens of thousands of block updates in one server tick.</p>
 */
public final class BossArenaProgressionService {
    private static final int SEARCH_INTERVAL_TICKS = 60;
    private static final int MAX_BUILD_STEPS_PER_TICK = 2;
    private static final long BUILD_BUDGET_NANOS = 1_500_000L;
    private static final double BUILD_TRIGGER_RADIUS = 128.0D;
    private static final double BUILD_TRIGGER_RADIUS_SQUARED = BUILD_TRIGGER_RADIUS * BUILD_TRIGGER_RADIUS;

    private static final int[][] MULTI_ARENA_ORIGINS = {
        {0, 0}, {4096, 0}, {-4096, 0}, {0, 4096}, {0, -4096}
    };
    private static final SearchTarget[] SEARCH_TARGETS = createSearchTargets();
    private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();

    private BossArenaProgressionService() {}

    public static void tick(MinecraftServer server) {
        int worldTier = WorldProgressionData.get(server).snapshot().worldTier();
        State state = state(server);
        if (worldTier != state.requestedTier) state.setRequestedTier(worldTier);

        if (state.activeBuild != null && processBuild(server, state)) return;

        if (state.activeBuild == null && startNearbyBuild(server, state)) {
            processBuild(server, state);
            return;
        }

        if (server.getTickCount() % SEARCH_INTERVAL_TICKS == 0) processOneSearch(server, state, worldTier);

        // Locating the Last Echo is cheap and deterministic. Actual placement remains handled by
        // LastEchoCitadelService's proximity-triggered, incremental builder.
        if (worldTier >= LastEchoProfile.REQUIRED_WORLD_TIER) LastEchoCitadelService.ensureLocated(server);
    }

    /** Called when progression changes. It schedules reconciliation instead of doing work inline. */
    public static void ensureUnlockedArenas(MinecraftServer server, int worldTier) {
        State state = state(server);
        if (worldTier > state.requestedTier) state.setRequestedTier(worldTier);
        if (worldTier >= LastEchoProfile.REQUIRED_WORLD_TIER) LastEchoCitadelService.ensureLocated(server);
    }

    private static State state(MinecraftServer server) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(server, ignored -> new State());
        }
    }

    private static void processOneSearch(MinecraftServer server, State state, int worldTier) {
        for (int offset = 0; offset < SEARCH_TARGETS.length; offset++) {
            int index = Math.floorMod(state.searchCursor + offset, SEARCH_TARGETS.length);
            SearchTarget target = SEARCH_TARGETS[index];
            if (state.searched[index] || target.kind.requiredTier > worldTier) continue;

            ServerLevel level = server.getLevel(target.kind.dimension);
            if (level == null || !hasPlayerInDimension(server, target.kind.dimension)) continue;

            state.searchCursor = (index + 1) % SEARCH_TARGETS.length;
            state.searched[index] = true;
            BlockPos center = locateTarget(level, target);
            if (center != null) state.addPending(new PendingArena(target.kind, center));
            return; // Never perform more than one structure search in one scheduler pass.
        }
    }

    private static BlockPos locateTarget(ServerLevel level, SearchTarget target) {
        int[] offset = MULTI_ARENA_ORIGINS[target.originIndex];
        return switch (target.kind) {
            case INFERNAL -> InfernalCrucibleArenaService.locateArena(level,
                new BlockPos(offset[0], 70, offset[1]));
            case AETHERIC -> {
                BlockPos landmark = locate(level, "end_city", new BlockPos(offset[0], 80, offset[1]), 256);
                yield landmark == null ? null : landmark.offset(48, 0, 0);
            }
            case DEEPBOUND -> {
                BlockPos spawn = level.getSharedSpawnPos();
                BlockPos origin = spawn.offset(offset[0], 0, offset[1]);
                BlockPos landmark = locate(level, "ancient_city", origin, 256);
                yield landmark == null ? null : landmark.offset(48, 2, 0);
            }
        };
    }

    private static boolean startNearbyBuild(MinecraftServer server, State state) {
        Iterator<PendingArena> iterator = state.pending.iterator();
        while (iterator.hasNext()) {
            PendingArena pending = iterator.next();
            ServerLevel level = server.getLevel(pending.kind.dimension);
            if (level == null || !playerNear(level, pending.center) || !level.hasChunkAt(pending.center)) continue;

            iterator.remove();
            if (arenaAlreadyBuilt(level, pending)) {
                registerExisting(server, pending);
                continue;
            }
            state.activeBuild = new BuildJob(pending.kind, pending.center);
            return true;
        }
        return false;
    }

    private static boolean arenaAlreadyBuilt(ServerLevel level, PendingArena pending) {
        return switch (pending.kind) {
            case INFERNAL -> InfernalCrucibleArenaService.hasArena(level, pending.center);
            case AETHERIC -> PromotionBossArenaService.hasBulwarkArena(level, pending.center);
            case DEEPBOUND -> PromotionBossArenaService.hasDeepboundArena(level, pending.center);
        };
    }

    private static void registerExisting(MinecraftServer server, PendingArena pending) {
        BossArenaRegistryData.Kind registryKind = switch (pending.kind) {
            case INFERNAL -> BossArenaRegistryData.Kind.INFERNAL;
            case AETHERIC -> BossArenaRegistryData.Kind.AETHERIC;
            case DEEPBOUND -> BossArenaRegistryData.Kind.DEEPBOUND;
        };
        BossArenaRegistryData.get(server).register(registryKind, pending.center);
    }

    private static boolean processBuild(MinecraftServer server, State state) {
        BuildJob job = state.activeBuild;
        if (job == null) return false;
        ServerLevel level = server.getLevel(job.kind.dimension);
        if (level == null || !playerNear(level, job.center)) return false;

        long deadline = System.nanoTime() + BUILD_BUDGET_NANOS;
        int processed = 0;
        while (processed < MAX_BUILD_STEPS_PER_TICK && System.nanoTime() < deadline) {
            if (job.step(level)) {
                state.activeBuild = null;
                return true;
            }
            processed++;
        }
        return processed > 0;
    }

    private static boolean hasPlayerInDimension(MinecraftServer server, ResourceKey<Level> dimension) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator() && player.level().dimension().equals(dimension)) return true;
        }
        return false;
    }

    private static boolean playerNear(ServerLevel level, BlockPos center) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= BUILD_TRIGGER_RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
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

    private static SearchTarget[] createSearchTargets() {
        List<SearchTarget> targets = new ArrayList<>(15);
        for (ArenaKind kind : ArenaKind.values()) {
            for (int origin = 0; origin < MULTI_ARENA_ORIGINS.length; origin++) {
                targets.add(new SearchTarget(kind, origin));
            }
        }
        return targets.toArray(SearchTarget[]::new);
    }

    private enum ArenaKind {
        INFERNAL(1, Level.NETHER),
        AETHERIC(2, Level.END),
        DEEPBOUND(3, Level.OVERWORLD);

        private final int requiredTier;
        private final ResourceKey<Level> dimension;

        ArenaKind(int requiredTier, ResourceKey<Level> dimension) {
            this.requiredTier = requiredTier;
            this.dimension = dimension;
        }
    }

    private static final class State {
        private int requestedTier = -1;
        private int searchCursor;
        private final boolean[] searched = new boolean[SEARCH_TARGETS.length];
        private final List<PendingArena> pending = new ArrayList<>();
        private final Set<ArenaKey> known = new HashSet<>();
        private BuildJob activeBuild;

        private void setRequestedTier(int tier) {
            requestedTier = tier;
            searchCursor = 0;
            for (int i = 0; i < searched.length; i++) searched[i] = false;
        }

        private void addPending(PendingArena pendingArena) {
            ArenaKey key = new ArenaKey(pendingArena.kind, pendingArena.center.asLong());
            if (known.add(key)) pending.add(pendingArena);
        }
    }

    private static final class BuildJob {
        private final ArenaKind kind;
        private final BlockPos center;
        private int column;
        private int decoration;
        private boolean decorating;

        private BuildJob(ArenaKind kind, BlockPos center) {
            this.kind = kind;
            this.center = center.immutable();
            this.column = minX(kind);
        }

        /** @return true once the complete arena has been built and registered. */
        private boolean step(ServerLevel level) {
            if (!decorating) {
                buildColumn(level, kind, center, column++);
                if (column > maxX(kind)) decorating = true;
                return false;
            }

            buildDecoration(level, kind, center, decoration++);
            return decoration >= decorationCount(kind);
        }

        private static int minX(ArenaKind kind) {
            return switch (kind) {
                case INFERNAL -> InfernalCrucibleArenaService.incrementalMinX();
                case AETHERIC -> PromotionBossArenaService.bulwarkMinX();
                case DEEPBOUND -> PromotionBossArenaService.deepboundMinX();
            };
        }

        private static int maxX(ArenaKind kind) {
            return switch (kind) {
                case INFERNAL -> InfernalCrucibleArenaService.incrementalMaxX();
                case AETHERIC -> PromotionBossArenaService.bulwarkMaxX();
                case DEEPBOUND -> PromotionBossArenaService.deepboundMaxX();
            };
        }

        private static int decorationCount(ArenaKind kind) {
            return switch (kind) {
                case INFERNAL -> InfernalCrucibleArenaService.incrementalDecorationCount();
                case AETHERIC -> PromotionBossArenaService.bulwarkDecorationCount();
                case DEEPBOUND -> PromotionBossArenaService.deepboundDecorationCount();
            };
        }

        private static void buildColumn(ServerLevel level, ArenaKind kind, BlockPos center, int x) {
            switch (kind) {
                case INFERNAL -> InfernalCrucibleArenaService.buildArenaColumn(level, center, x);
                case AETHERIC -> PromotionBossArenaService.buildBulwarkColumn(level, center, x);
                case DEEPBOUND -> PromotionBossArenaService.buildDeepboundColumn(level, center, x);
            }
        }

        private static void buildDecoration(ServerLevel level, ArenaKind kind, BlockPos center, int step) {
            switch (kind) {
                case INFERNAL -> InfernalCrucibleArenaService.buildArenaDecoration(level, center, step);
                case AETHERIC -> PromotionBossArenaService.buildBulwarkDecoration(level, center, step);
                case DEEPBOUND -> PromotionBossArenaService.buildDeepboundDecoration(level, center, step);
            }
        }
    }

    private record SearchTarget(ArenaKind kind, int originIndex) {}
    private record PendingArena(ArenaKind kind, BlockPos center) {}
    private record ArenaKey(ArenaKind kind, long center) {}
}
