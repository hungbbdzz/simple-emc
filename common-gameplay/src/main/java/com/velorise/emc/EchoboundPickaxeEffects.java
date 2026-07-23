package com.velorise.emc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Terrain-safe earthquake slam for the Echobound Pickaxe. */
public final class EchoboundPickaxeEffects {
    public static final double RADIUS = 8.0D;
    public static final float DAMAGE = 100.0F;

    private EchoboundPickaxeEffects() {}

    public static Vec3 impactPoint(Player player) {
        return player.pick(5.0D, 0.0F, false).getLocation();
    }

    public static void slam(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = impactPoint(player);

        // Visual/sound-only explosion: level.explode is deliberately never used,
        // so neither Vanilla nor another loader can turn this into block damage.
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.5F, 0.62F);
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 2.2F, 0.72F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            center.x, center.y, center.z, 2, 0.65D, 0.18D, 0.65D, 0.0D);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(RADIUS),
            entity -> entity != player && entity.isAlive())) {
            double distance = target.position().distanceTo(center);
            if (distance > RADIUS) continue;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);

            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() > 1.0E-6D) away = away.normalize();
            double strength = Math.max(0.25D, 1.0D - distance / RADIUS);
            target.push(away.x * (1.2D + strength * 1.8D),
                0.55D + strength * 0.85D,
                away.z * (1.2D + strength * 1.8D));
        }

        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * (RADIUS / 3.0D);
            int points = 20 + ring * 8;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles(ring == 3 ? ParticleTypes.SCULK_SOUL : ParticleTypes.SCULK_CHARGE_POP,
                    center.x + Math.cos(angle) * radius, center.y + 0.16D,
                    center.z + Math.sin(angle) * radius,
                    1, 0.06D, 0.04D, 0.06D, 0.01D);
            }
        }
    }
}
