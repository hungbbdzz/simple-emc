package com.velorise.emc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side hit-chain echo effects for the ordinary resonant swords. */
public final class ResonantSwordEffects {
    private static final float BONUS_DAMAGE = 5.0F;
    private static final Map<UUID, HitChain> HIT_CHAINS = new HashMap<>();

    private ResonantSwordEffects() {}

    public static void recordHit(LivingEntity attacker, LivingEntity target, int tier) {
        if (!(attacker instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        int requiredHits = requiredHits(tier);
        if (requiredHits <= 0) return;

        HitChain chain = HIT_CHAINS.computeIfAbsent(player.getUUID(), ignored -> new HitChain(tier));
        if (chain.tier != tier) {
            chain.tier = tier;
            chain.hits = 0;
        }
        chain.hits++;
        if (chain.hits < requiredHits) return;
        chain.hits = 0;

        renderEchoPulse(level, target, tier);
        playEchoSound(level, target, tier);

        if (target.isAlive()) {
            // The bonus is a separate hit but never destroys terrain and never
            // passes through the sword hook again, so it cannot count itself.
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), BONUS_DAMAGE);
        }
    }

    private static int requiredHits(int tier) {
        return switch (tier) {
            case 2 -> 5;
            case 3 -> 4;
            case 4 -> 3;
            default -> 0;
        };
    }

    private static void renderEchoPulse(ServerLevel level, LivingEntity target, int tier) {
        var center = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        int count = 2 + level.random.nextInt(3);
        double spread = 0.10D + level.random.nextDouble() * 0.07D;
        if (tier == 2) {
            level.sendParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z, count, spread, spread, spread, 0.005D);
        } else if (tier == 3) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, count, spread, spread, spread, 0.005D);
        } else {
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                center.x, center.y, center.z, count, spread, spread, spread, 0.005D);
        }
    }

    private static void playEchoSound(ServerLevel level, LivingEntity target, int tier) {
        double x = target.getX();
        double y = target.getY(0.55D);
        double z = target.getZ();
        if (tier == 2) {
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.72F, 0.86F);
        } else if (tier == 3) {
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 0.74F, 1.12F);
        } else {
            level.playSound(null, x, y, z, SoundEvents.SCULK_CLICKING,
                SoundSource.PLAYERS, 0.8F, 1.28F);
        }
    }

    private static final class HitChain {
        private int tier;
        private int hits;

        private HitChain(int tier) {
            this.tier = tier;
        }
    }
}
