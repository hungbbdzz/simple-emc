package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Budgeted, deterministic World-Tier ore retrogen for old and newly generated chunks. */
@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ResonantOreRetrogen {
    private static final String BLESSED_TIER = "velorise_emc:blessed_tier";
    private static final int MAX_CHUNKS_PER_TICK = 2;
    private static final int MAX_QUEUED_CHUNKS = 4096;
    private static final long TICK_BUDGET_NANOS = 1_250_000L;
    private static final Map<MinecraftServer, State> STATES = new ConcurrentHashMap<>();

    private ResonantOreRetrogen() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            enqueue(level, chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        processTick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.remove(event.getServer());
    }

    public static void enqueueAroundPlayers(MinecraftServer server, int radius) {
        int boundedRadius = Math.max(0, Math.min(radius, 8));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            ChunkPos center = player.chunkPosition();
            for (int dx = -boundedRadius; dx <= boundedRadius; dx++) {
                for (int dz = -boundedRadius; dz <= boundedRadius; dz++) {
                    int x = center.x + dx;
                    int z = center.z + dz;
                    if (level.getChunkSource().getChunkNow(x, z) != null) enqueue(level, new ChunkPos(x, z));
                }
            }
        }
    }

    private static void processTick(MinecraftServer server) {
        State state = STATES.computeIfAbsent(server, ignored -> new State());
        trackPlayers(server, state);
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;

        for (int processed = 0; processed < MAX_CHUNKS_PER_TICK && System.nanoTime() < deadline; processed++) {
            Pending pending = state.queue.poll();
            if (pending == null) break;
            state.queued.remove(pending.key());

            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) continue;
            LevelChunk chunk = level.getChunkSource().getChunkNow(pending.chunk().x, pending.chunk().z);
            if (chunk != null) bless(level, chunk);
        }
    }

    private static void trackPlayers(MinecraftServer server, State state) {
        Set<UUID> online = ConcurrentHashMap.newKeySet();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            ChunkPos chunk = player.chunkPosition();
            PendingKey key = new PendingKey(player.level().dimension(), chunk.toLong());
            if (!key.equals(state.lastPlayerChunk.put(player.getUUID(), key))) enqueue(player.serverLevel(), chunk);
        }
        state.lastPlayerChunk.keySet().removeIf(id -> !online.contains(id));
    }

    private static void enqueue(ServerLevel level, ChunkPos chunk) {
        int worldTier = WorldProgressionData.get(level.getServer()).snapshot().worldTier();
        if (worldTier <= 0) return;

        State state = STATES.computeIfAbsent(level.getServer(), ignored -> new State());
        if (state.queued.size() >= MAX_QUEUED_CHUNKS) return;
        PendingKey key = new PendingKey(level.dimension(), chunk.toLong());
        if (state.queued.add(key)) state.queue.add(new Pending(level.dimension(), chunk, key));
    }

    private static void bless(ServerLevel level, LevelChunk chunk) {
        WorldProgressionData data = WorldProgressionData.get(level.getServer());
        int currentTier = data.snapshot().worldTier();
        String chunkKey = level.dimension().location() + ":" + chunk.getPos().toLong();
        int blessedTier = data.getBlessedTier(chunkKey);
        if (blessedTier >= currentTier) return;
        for (int tier = blessedTier + 1; tier <= currentTier; tier++) placeTier(level, chunk, tier);
        data.setBlessedTier(chunkKey, currentTier);
    }

    private static void placeTier(ServerLevel level, LevelChunk chunk, int tier) {
        ChunkPos cp = chunk.getPos();
        RandomSource random = RandomSource.create(mix(level.getSeed(), cp.x, cp.z, tier));
        int veinSize = 1 + random.nextInt(4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 96; attempt++) {
            int x = cp.getMinBlockX() + random.nextInt(16);
            int z = cp.getMinBlockZ() + random.nextInt(16);
            if (tier == 1 && Level.OVERWORLD.equals(level.dimension())) {
                int y = randomY(level, random, Math.max(level.getMinBuildHeight() + 4, -56), Math.min(level.getMaxBuildHeight() - 2, 72));
                pos.set(x, y, z);
            } else if (tier == 2 && Level.NETHER.equals(level.dimension())) {
                pos.set(x, randomY(level, random, 8, 120), z);
            } else if (tier == 3 && Level.END.equals(level.dimension()) && outerEnd(cp)) {
                pos.set(x, randomY(level, random, 16, Math.min(112, level.getMaxBuildHeight() - 2)), z);
            } else if (tier == 4 && Level.OVERWORLD.equals(level.dimension())) {
                pos.set(x, randomY(level, random, level.getMinBuildHeight() + 5, Math.min(-1, level.getMaxBuildHeight() - 2)), z);
            } else return;

            if (isOreHost(level, pos, tier)) {
                placeVein(level, cp, pos.immutable(), tier, veinSize, random);
                return;
            }
        }
    }

    private static void placeVein(ServerLevel level, ChunkPos chunk, BlockPos origin, int tier,
                                  int targetSize, RandomSource random) {
        List<BlockPos> placed = new ArrayList<>(targetSize);
        placeOre(level, origin, tier);
        placed.add(origin);

        Direction[] directions = Direction.values();
        for (int attempt = 0; attempt < targetSize * 16 && placed.size() < targetSize; attempt++) {
            BlockPos base = placed.get(random.nextInt(placed.size()));
            BlockPos candidate = base.relative(directions[random.nextInt(directions.length)]);
            if (!chunk.equals(new ChunkPos(candidate)) || placed.contains(candidate) || !isOreHost(level, candidate, tier)) continue;
            placeOre(level, candidate, tier);
            placed.add(candidate);
        }
    }

    private static boolean isOreHost(ServerLevel level, BlockPos pos, int tier) {
        var state = level.getBlockState(pos);
        return switch (tier) {
            case 1 -> state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE);
            case 2 -> state.is(Blocks.NETHERRACK);
            case 3 -> state.is(Blocks.END_STONE);
            case 4 -> state.is(Blocks.DEEPSLATE);
            default -> false;
        };
    }

    private static void placeOre(ServerLevel level, BlockPos pos, int tier) {
        var host = level.getBlockState(pos);
        var ore = switch (tier) {
            case 1 -> host.is(Blocks.DEEPSLATE)
                ? VeloriseEMC.DEEPSLATE_VERDANT_RESONANT_ORE.get().defaultBlockState()
                : VeloriseEMC.VERDANT_RESONANT_ORE.get().defaultBlockState();
            case 2 -> VeloriseEMC.INFERNAL_RESONANT_ORE.get().defaultBlockState();
            case 3 -> VeloriseEMC.AETHERIC_RESONANT_ORE.get().defaultBlockState();
            case 4 -> VeloriseEMC.ECHOING_RESONANT_ORE.get().defaultBlockState();
            default -> throw new IllegalArgumentException("Unsupported ore tier: " + tier);
        };
        level.setBlock(pos, ore, 2);
    }

    private static int randomY(ServerLevel level, RandomSource random, int min, int max) {
        min = Math.max(level.getMinBuildHeight(), min);
        max = Math.min(level.getMaxBuildHeight() - 1, Math.max(min, max));
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private static boolean outerEnd(ChunkPos chunk) {
        long x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        return x * x + z * z >= 1_024L * 1_024L;
    }

    private static long mix(long seed, int x, int z, int tier) {
        long value = seed ^ (long)x * 341873128712L ^ (long)z * 132897987541L ^ (long)tier * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27; value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static final class State {
        private final ConcurrentLinkedQueue<Pending> queue = new ConcurrentLinkedQueue<>();
        private final Set<PendingKey> queued = ConcurrentHashMap.newKeySet();
        private final Map<UUID, PendingKey> lastPlayerChunk = new ConcurrentHashMap<>();
    }

    private record PendingKey(ResourceKey<Level> dimension, long chunk) {}
    private record Pending(ResourceKey<Level> dimension, ChunkPos chunk, PendingKey key) {}
}
