package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Lightweight short-lived boss summoning ritual controller shared by all altar encounters. */
public final class ArenaSummonService {
    public enum BossKind { INFERNAL_CRUCIBLE, AETHERIC_BULWARK, DEEPBOUND_WARDEN, LAST_ECHO }

    private static final int SUMMON_TICKS = 80;
    private static final List<Pending> PENDING = new ArrayList<>();

    private ArenaSummonService() {}

    public static boolean isPending(ResourceKey<Level> dimension, BlockPos center, BossKind kind) {
        return PENDING.stream().anyMatch(p -> p.dimension.equals(dimension)
            && p.kind == kind
            && p.center.distSqr(center) <= 36.0D * 36.0D);
    }

    public static boolean isBossPresent(ServerLevel level, BlockPos center, BossKind kind) {
        if (isPending(level.dimension(), center, kind)) return true;
        // Inflate to 96 blocks to match the actual summonBoss / summonBulwarkBoss / summonDeepboundBoss checks,
        // preventing Eye items from being consumed when a living boss is slightly further out.
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center).inflate(96.0D);
        return switch (kind) {
            case INFERNAL_CRUCIBLE -> !level.getEntitiesOfClass(InfernalCrucibleEntity.class, box, e -> e.isAlive()).isEmpty();
            case AETHERIC_BULWARK -> !level.getEntitiesOfClass(AethericBulwarkEntity.class, box, e -> e.isAlive()).isEmpty();
            case DEEPBOUND_WARDEN -> !level.getEntitiesOfClass(DeepboundWardenEntity.class, box, e -> e.isAlive()).isEmpty();
            case LAST_ECHO -> LastEchoCitadelData.get(level.getServer()).bossActive()
                || !level.getEntitiesOfClass(EchoboundDragonEntity.class, box, e -> e.isAlive()).isEmpty();
        };
    }

    public static boolean start(ServerLevel level, BlockPos center, BossKind kind) {
        if (isBossPresent(level, center, kind)) return false;
        PENDING.add(new Pending(level.getServer(), level.dimension(), center.immutable(), kind, SUMMON_TICKS));
        net.minecraft.sounds.SoundEvent startSound = switch (kind) {
            case INFERNAL_CRUCIBLE -> SoundEvents.FIRECHARGE_USE;
            case AETHERIC_BULWARK -> SoundEvents.END_PORTAL_FRAME_FILL;
            case DEEPBOUND_WARDEN -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
            case LAST_ECHO -> SoundEvents.ENDER_DRAGON_GROWL;
        };
        float pitch = kind == BossKind.DEEPBOUND_WARDEN ? 0.70F : (kind == BossKind.AETHERIC_BULWARK ? 1.25F : 1.0F);
        level.playSound(null, center, startSound, SoundSource.HOSTILE, 2.5F, pitch);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();
            MinecraftServer ownerServer = pending.serverRef.get();
            if (ownerServer == null || ownerServer != server) {
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(pending.dimension);
            if (level == null) {
                it.remove();
                continue;
            }
            // Wait if chunk is temporarily unloaded (e.g., player died during ritual and is respawning)
            if (!level.hasChunkAt(pending.center)) {
                if (++pending.unloadedTicks > 20 * 60) {
                    it.remove();
                }
                continue;
            }
            pending.unloadedTicks = 0;

            tickParticles(level, pending);
            pending.ticksRemaining--;
            if (pending.ticksRemaining > 0) continue;

            boolean spawned = switch (pending.kind) {
                case INFERNAL_CRUCIBLE -> InfernalCrucibleArenaService.summonBoss(level, pending.center);
                case AETHERIC_BULWARK -> PromotionBossArenaService.summonBulwarkBoss(level, pending.center);
                case DEEPBOUND_WARDEN -> PromotionBossArenaService.summonDeepboundBoss(level, pending.center);
                case LAST_ECHO -> LastEchoCitadelService.summonExisting(level, pending.center);
            };
            if (spawned) {
                spawnExplosionParticles(level, pending);
                net.minecraft.sounds.SoundEvent spawnSound = switch (pending.kind) {
                    case INFERNAL_CRUCIBLE -> SoundEvents.WITHER_SPAWN;
                    case AETHERIC_BULWARK -> SoundEvents.SHULKER_TELEPORT;
                    case DEEPBOUND_WARDEN -> SoundEvents.WARDEN_EMERGE;
                    case LAST_ECHO -> SoundEvents.ENDER_DRAGON_GROWL;
                };
                float pitch = pending.kind == BossKind.DEEPBOUND_WARDEN ? 0.70F : (pending.kind == BossKind.AETHERIC_BULWARK ? 1.20F : 0.90F);
                level.playSound(null, pending.center, spawnSound, SoundSource.HOSTILE, 5.0F, pitch);
            }
            it.remove();
        }
    }

    private static void spawnExplosionParticles(ServerLevel level, Pending pending) {
        double cx = pending.center.getX() + 0.5D;
        double cy = pending.center.getY() + 1.5D;
        double cz = pending.center.getZ() + 0.5D;
        double skyMax = pending.kind == BossKind.LAST_ECHO ? 26.0D : 20.0D;
        for (double y = 0; y <= skyMax; y += 2.0D) {
            switch (pending.kind) {
                case INFERNAL_CRUCIBLE -> {
                    level.sendParticles(ParticleTypes.FLAME, cx, cy + y, cz, 16, 1.2D, 1.0D, 1.2D, 0.15D);
                    level.sendParticles(ParticleTypes.LAVA, cx, cy + y, cz, 8, 1.0D, 0.8D, 1.0D, 0.1D);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + y, cz, 6, 1.5D, 1.0D, 1.5D, 0.05D);
                }
                case AETHERIC_BULWARK -> {
                    level.sendParticles(ParticleTypes.END_ROD, cx, cy + y, cz, 12, 1.0D, 1.0D, 1.0D, 0.12D);
                    level.sendParticles(ParticleTypes.PORTAL, cx, cy + y, cz, 20, 1.4D, 1.0D, 1.4D, 0.2D);
                }
                case DEEPBOUND_WARDEN -> {
                    level.sendParticles(ParticleTypes.SCULK_SOUL, cx, cy + y, cz, 15, 1.2D, 1.0D, 1.2D, 0.1D);
                    level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, cx, cy + y, cz, 10, 1.0D, 0.8D, 1.0D, 0.08D);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, cx, cy + y, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                case LAST_ECHO -> {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, cx, cy + y, cz, 22, 1.5D, 1.0D, 1.5D, 0.15D);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, cx, cy + y, cz, 18, 1.2D, 1.0D, 1.2D, 0.2D);
                    level.sendParticles(ParticleTypes.SCULK_SOUL, cx, cy + y, cz, 10, 1.0D, 0.8D, 1.0D, 0.1D);
                }
            }
        }
    }

    private static void tickParticles(ServerLevel level, Pending pending) {
        double cx = pending.center.getX() + 0.5D;
        double cy = pending.center.getY() + 1.2D;
        double cz = pending.center.getZ() + 0.5D;
        double progress = 1.0D - pending.ticksRemaining / (double)SUMMON_TICKS;
        double swirlRadius = Mth.lerp(progress, 4.5D, 0.6D);
        double maxSkyHeight = pending.kind == BossKind.LAST_ECHO ? 26.0D : 20.0D;
        double columnTop = cy + progress * maxSkyHeight;

        for (int i = 0; i < 12; i++) {
            double angle = (pending.ticksRemaining * 0.22D) + i * (Math.PI / 6.0D);
            double heightFrac = (double) i / 12.0D;
            double py = cy + progress * maxSkyHeight * heightFrac;
            double radius = swirlRadius * (1.0D - heightFrac * 0.4D);
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;

            switch (pending.kind) {
                case INFERNAL_CRUCIBLE -> {
                    level.sendParticles(ParticleTypes.FLAME, px, py, pz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
                    level.sendParticles(ParticleTypes.LAVA, px, py + 0.2D, pz, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                    if ((i & 3) == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0.02D, 0.02D, 0.02D, 0.01D);
                }
                case AETHERIC_BULWARK -> {
                    level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.03D, 0.03D, 0.03D, 0.01D);
                    level.sendParticles(ParticleTypes.PORTAL, px, py, pz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
                }
                case DEEPBOUND_WARDEN -> {
                    level.sendParticles(ParticleTypes.SCULK_SOUL, px, py, pz, 1, 0.03D, 0.03D, 0.03D, 0.01D);
                    level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, px, py, pz, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
                case LAST_ECHO -> {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, 0.04D, 0.04D, 0.04D, 0.02D);
                }
            }
        }

        for (double h = 0; h <= progress * maxSkyHeight; h += 2.5D) {
            double py = cy + h;
            switch (pending.kind) {
                case INFERNAL_CRUCIBLE -> level.sendParticles(ParticleTypes.FLAME, cx, py, cz, 3, 0.25D, 0.5D, 0.25D, 0.03D);
                case AETHERIC_BULWARK -> level.sendParticles(ParticleTypes.END_ROD, cx, py, cz, 2, 0.2D, 0.5D, 0.2D, 0.02D);
                case DEEPBOUND_WARDEN -> level.sendParticles(ParticleTypes.SCULK_SOUL, cx, py, cz, 2, 0.3D, 0.5D, 0.3D, 0.02D);
                case LAST_ECHO -> level.sendParticles(ParticleTypes.DRAGON_BREATH, cx, py, cz, 4, 0.35D, 0.5D, 0.35D, 0.03D);
            }
        }

        if ((pending.ticksRemaining & 7) == 0) {
            net.minecraft.sounds.SoundEvent pulseSound = switch (pending.kind) {
                case INFERNAL_CRUCIBLE -> SoundEvents.BLAZE_AMBIENT;
                case AETHERIC_BULWARK -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case DEEPBOUND_WARDEN -> SoundEvents.WARDEN_HEARTBEAT;
                case LAST_ECHO -> SoundEvents.END_PORTAL_SPAWN;
            };
            BlockPos soundPos = BlockPos.containing(cx, columnTop, cz);
            level.playSound(null, soundPos, pulseSound, SoundSource.BLOCKS, 1.2F, 0.7F + (float)progress * 0.6F);
        }
    }

    private static final class Pending {
        private final WeakReference<MinecraftServer> serverRef;
        private final ResourceKey<Level> dimension;
        private final BlockPos center;
        private final BossKind kind;
        private int ticksRemaining;
        private int unloadedTicks;

        private Pending(MinecraftServer server, ResourceKey<Level> dimension, BlockPos center, BossKind kind, int ticksRemaining) {
            this.serverRef = new WeakReference<>(server);
            this.dimension = dimension;
            this.center = center;
            this.kind = kind;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
