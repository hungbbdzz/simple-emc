package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Tracks Webcaster cobwebs, their short venom window and restoration time. */
public final class TemporaryWebManager {
    private static final int VENOM_PULSE_INTERVAL_TICKS = 20;
    private static final int POISON_REFRESH_TICKS = 60;
    private static final Map<ServerLevel, Map<BlockPos, Entry>> WEBS = new IdentityHashMap<>();

    private TemporaryWebManager() {}

    public static boolean place(ServerLevel level, BlockPos pos, int durationTicks,
                                int venomDurationTicks, int poisonAmplifier, UUID ownerId) {
        BlockState current = level.getBlockState(pos);
        Map<BlockPos, Entry> entries = WEBS.computeIfAbsent(level, ignored -> new HashMap<>());
        BlockPos key = pos.immutable();
        Entry existing = entries.get(key);
        long now = level.getGameTime();
        long expiresAt = now + Math.max(1, durationTicks);
        long venomExpiresAt = now + Math.max(0, venomDurationTicks);
        int amplifier = Math.max(0, poisonAmplifier);
        if (existing != null && current.is(Blocks.COBWEB)) {
            existing.expiresAt = Math.max(existing.expiresAt, expiresAt);
            existing.venomExpiresAt = Math.max(existing.venomExpiresAt, venomExpiresAt);
            existing.poisonAmplifier = Math.max(existing.poisonAmplifier, amplifier);
            if (ownerId != null) existing.ownerId = ownerId;
            return true;
        }
        if (!current.canBeReplaced() || !level.getFluidState(pos).isEmpty()) return false;
        entries.put(key, new Entry(current, expiresAt, venomExpiresAt, amplifier, ownerId));
        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<ServerLevel, Map<BlockPos, Entry>>> levels = WEBS.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ServerLevel, Map<BlockPos, Entry>> levelEntry = levels.next();
            ServerLevel level = levelEntry.getKey();
            if (level.getServer() != server) continue;

            long now = level.getGameTime();
            boolean venomPulse = now % VENOM_PULSE_INTERVAL_TICKS == 0L;
            Map<LivingEntity, Integer> poisoned = venomPulse ? new IdentityHashMap<>() : Map.of();
            Map<LivingEntity, UUID> venomOwners = venomPulse ? new IdentityHashMap<>() : Map.of();
            Iterator<Map.Entry<BlockPos, Entry>> webs = levelEntry.getValue().entrySet().iterator();
            while (webs.hasNext()) {
                Map.Entry<BlockPos, Entry> web = webs.next();
                BlockPos pos = web.getKey();
                Entry entry = web.getValue();
                if (!level.hasChunkAt(pos)) continue;

                if (now >= entry.expiresAt) {
                    if (level.getBlockState(pos).is(Blocks.COBWEB)) {
                        level.setBlock(pos, entry.previous, 3);
                    }
                    webs.remove();
                    continue;
                }

                if (!venomPulse || now >= entry.venomExpiresAt || !level.getBlockState(pos).is(Blocks.COBWEB)) {
                    continue;
                }
                AABB webBox = new AABB(pos).inflate(0.05D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, webBox, LivingEntity::isAlive)) {
                    poisoned.merge(target, entry.poisonAmplifier, Math::max);
                    if (entry.ownerId != null) venomOwners.putIfAbsent(target, entry.ownerId);
                }
            }

            if (venomPulse) {
                poisoned.forEach((target, amplifier) -> target.addEffect(
                    new MobEffectInstance(MobEffects.POISON, POISON_REFRESH_TICKS, amplifier, true, true, true)));
                venomOwners.forEach((target, ownerId) -> {
                    net.minecraft.server.level.ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
                    if (owner != null) ArcaneAchievementTracker.markWebVenomPulse(owner, target);
                });
            }
            if (levelEntry.getValue().isEmpty()) levels.remove();
        }
    }

    private static final class Entry {
        private final BlockState previous;
        private long expiresAt;
        private long venomExpiresAt;
        private int poisonAmplifier;
        private UUID ownerId;

        private Entry(BlockState previous, long expiresAt, long venomExpiresAt, int poisonAmplifier, UUID ownerId) {
            this.previous = previous;
            this.expiresAt = expiresAt;
            this.venomExpiresAt = venomExpiresAt;
            this.poisonAmplifier = poisonAmplifier;
            this.ownerId = ownerId;
        }
    }
}
