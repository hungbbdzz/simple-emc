package com.velorise.emc;

import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Server-authoritative combat effects for the post-boss Echobound weapons. */
public final class EchoboundWeaponEffects {
    private static final List<EchoCombo> ACTIVE_COMBOS = new ArrayList<>();
    private static final List<SonicPulseCombo> ACTIVE_SONIC_PULSES = new ArrayList<>();
    private static final List<ForwardWave> ACTIVE_FORWARD_WAVES = new ArrayList<>();

    private EchoboundWeaponEffects() {}

    /** Adds twenty echoes: the first five deal 100 damage, then the normal 25 damage. */
    public static void applyMeleeEchoes(LivingEntity attacker, LivingEntity target) {
        if (!(attacker.level() instanceof ServerLevel) || !target.isAlive()) return;
        Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        ACTIVE_COMBOS.add(new EchoCombo(attacker, target, impact, 0, 0));
    }

    /** Resolves one echo per server tick, producing a one-second twenty-hit flurry. */
    public static void tick(MinecraftServer server) {
        Iterator<EchoCombo> iterator = ACTIVE_COMBOS.iterator();
        while (iterator.hasNext()) {
            EchoCombo combo = iterator.next();
            if (combo.attacker().getServer() != server || !combo.attacker().isAlive()
                || !combo.target().isAlive() || combo.hitIndex() >= LastEchoProfile.ECHOBOUND_MELEE_ECHO_HITS) {
                iterator.remove();
                continue;
            }

            if (combo.ageTicks() < combo.hitIndex()) {
                combo.advanceAge();
                continue;
            }

            if (combo.attacker().level() instanceof ServerLevel level) {
                DamageSource source = combo.attacker() instanceof Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(combo.attacker());
                float damage = openingOrNormalDamage(combo.hitIndex(),
                    LastEchoProfile.ECHOBOUND_MELEE_ECHO_DAMAGE);
                hurtIgnoringVanillaInterval(combo.target(), source, damage);
                renderEchoImpact(level, combo.target(), combo.impact(), combo.hitIndex());
                playSlashSound(level, combo.target(), combo.hitIndex());
            }

            int next = combo.hitIndex() + 1;
            combo.advanceAge();
            if (next >= LastEchoProfile.ECHOBOUND_MELEE_ECHO_HITS || !combo.target().isAlive()) {
                iterator.remove();
            } else {
                combo.setHitIndex(next);
            }
        }

        tickSonicPulses(server);
        tickForwardWaves(server);
    }

    /**
     * Fires one visible Sonic Boom. Its first five pulses deal 100 damage;
     * the remaining pulses return to 10 damage. Per-hit caps cannot collapse
     * the whole attack.
     */
    public static boolean fireSonicBoom(ServerPlayer player) {
        if (!EmcAccountUtil.remove(player, LastEchoProfile.ECHOBOUND_SONIC_EMC_COST)) {
            EmcFailureMessages.show(player, LastEchoProfile.ECHOBOUND_SONIC_EMC_COST);
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(LastEchoProfile.ECHOBOUND_SONIC_RANGE));
        AABB search = player.getBoundingBox().expandTowards(
            look.scale(LastEchoProfile.ECHOBOUND_SONIC_RANGE)).inflate(15.0D);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != player && entity.isAlive() && isNearBeam(start, look, entity))
            .stream()
            .sorted(Comparator.comparingDouble(entity -> beamDistance(start, look, entity)))
            .toList();

        // SwordItem's LivingEntity hooks never run for crystals, minecarts, boats,
        // item frames, armour stands, and similar attackable entities. Collect those
        // separately so the endgame shockwave behaves like a real weapon rather than
        // passing harmlessly through every non-living combat target.
        List<Entity> breakableEntities = level.getEntities(player, search,
                entity -> !(entity instanceof LivingEntity)
                    && entity.isAlive()
                    && entity.isAttackable()
                    && isNearBeam(start, look, entity))
            .stream()
            .sorted(Comparator.comparingDouble(entity -> beamDistance(start, look, entity)))
            .toList();

        // The beam always travels its full range. Neither blocks nor an earlier
        // entity truncate it, matching the intended fully penetrating attack.
        renderBeam(level, start, end);
        level.sendParticles(ParticleTypes.SONIC_BOOM, end.x, end.y, end.z,
            1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.5F, 0.78F);
        // Emit the real Warden Sonic Boom at the wielder rather than only at the
        // distant beam endpoint. This keeps the attack full-bodied in first person.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.8F, 0.92F);

        for (LivingEntity target : targets) {
            Vec3 impact = target.getEyePosition();
            level.sendParticles(ParticleTypes.SONIC_BOOM, impact.x, impact.y, impact.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            // Do not submit all fifty hurt calls in one game tick: vanilla and
            // compatibility hooks may collapse them. One queued pulse per tick
            // guarantees fifty distinct damage opportunities over 2.5 seconds.
            ACTIVE_SONIC_PULSES.add(new SonicPulseCombo(player, target, 0));
            Vec3 push = look.scale(2.5D);
            target.push(push.x, 0.5D, push.z);
        }

        DamageSource directSource = level.damageSources().playerAttack(player);
        for (Entity target : breakableEntities) {
            Vec3 impact = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.SONIC_BOOM, impact.x, impact.y, impact.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            // One authoritative weapon hit is enough for End Crystals and other
            // vanilla breakable entities. Their own hurt logic still controls
            // explosions, drops, invulnerability, and mod compatibility.
            target.hurt(directSource, LastEchoProfile.ECHOBOUND_EMPOWERED_OPENING_DAMAGE);
        }
        return true;
    }


    /** Fires a 180-degree forward cone that damages every valid target inside it. */
    public static boolean fireForwardCone(ServerPlayer player) {
        if (!EmcAccountUtil.remove(player, LastEchoProfile.ECHOBOUND_SONIC_EMC_COST)) {
            EmcFailureMessages.show(player, LastEchoProfile.ECHOBOUND_SONIC_EMC_COST);
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.getEyePosition();
        Vec3 forward = player.getLookAngle().normalize();
        double range = LastEchoProfile.ECHOBOUND_SONIC_RANGE;
        double minimumDot = Math.cos(Math.toRadians(90.0D)); // 180-degree total cone.
        AABB area = player.getBoundingBox().inflate(range);
        DamageSource source = level.damageSources().playerAttack(player);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e != player && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
            double distance = toTarget.length();
            if (distance <= 0.001D || distance > range) continue;
            if (forward.dot(toTarget.scale(1.0D / distance)) < minimumDot) continue;

            target.invulnerableTime = 0;
            target.hurt(source, 1000.0F);
            Vec3 push = forward.scale(1.6D);
            target.push(push.x, 0.35D, push.z);
        }

        for (Entity target : level.getEntities(player, area,
            e -> !(e instanceof LivingEntity) && e.isAlive() && e.isAttackable())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
            double distance = toTarget.length();
            if (distance <= 0.001D || distance > range) continue;
            if (forward.dot(toTarget.scale(1.0D / distance)) < minimumDot) continue;
            target.hurt(source, 1000.0F);
        }

        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 0.001D) horizontalForward = new Vec3(0.0D, 0.0D, 1.0D);
        ACTIVE_FORWARD_WAVES.add(new ForwardWave(player, level, origin, horizontalForward.normalize(), range, 0));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.8F, 0.78F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 0.62F);
        return true;
    }

    private static void tickForwardWaves(MinecraftServer server) {
        Iterator<ForwardWave> iterator = ACTIVE_FORWARD_WAVES.iterator();
        while (iterator.hasNext()) {
            ForwardWave wave = iterator.next();
            if (wave.owner().getServer() != server || wave.level().getServer() != server
                || !wave.owner().isAlive()) {
                iterator.remove();
                continue;
            }

            double distance = 2.5D + wave.ageTicks() * 3.75D;
            if (distance > wave.range()) {
                iterator.remove();
                continue;
            }

            renderForwardWaveArc(wave.level(), wave.origin(), wave.forward(), distance, wave.ageTicks());
            wave.advanceAge();
        }
    }

    /**
     * Draws a layered 180-degree crescent. No Sonic Boom particles are used here:
     * the wave travels as a bright sculk/ender slash instead of another Warden beam.
     */
    private static void renderForwardWaveArc(ServerLevel level, Vec3 origin, Vec3 forward,
                                             double distance, int ageTicks) {
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        int samples = Math.min(41, Math.max(19, (int)Math.ceil(distance * 0.9D)));
        double verticalRipple = Math.sin(ageTicks * 0.72D) * 0.18D;

        for (int i = 0; i < samples; i++) {
            double progress = samples == 1 ? 0.5D : i / (double)(samples - 1);
            double angle = Math.toRadians(-90.0D + 180.0D * progress);
            Vec3 direction = forward.scale(Math.cos(angle)).add(right.scale(Math.sin(angle))).normalize();
            double crown = Math.sin(Math.PI * progress);
            Vec3 point = origin.add(direction.scale(distance)).add(0.0D, verticalRipple + crown * 0.32D, 0.0D);

            level.sendParticles(ParticleTypes.END_ROD,
                point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if ((i + ageTicks) % 2 == 0) {
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                    point.x, point.y - 0.28D, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (i % Math.max(1, samples / 6) == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y + 0.22D, point.z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
            }
        }

        Vec3 center = origin.add(forward.scale(distance));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
            center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void tickSonicPulses(MinecraftServer server) {
        Iterator<SonicPulseCombo> iterator = ACTIVE_SONIC_PULSES.iterator();
        while (iterator.hasNext()) {
            SonicPulseCombo combo = iterator.next();
            if (combo.owner().getServer() != server || !combo.target().isAlive()
                || combo.pulsesDealt() >= LastEchoProfile.ECHOBOUND_SONIC_DAMAGE_PULSES) {
                iterator.remove();
                continue;
            }

            if (combo.owner().level() instanceof ServerLevel level) {
                DamageSource source = level.damageSources().sonicBoom(combo.owner());
                float damage = openingOrNormalDamage(combo.pulsesDealt(),
                    LastEchoProfile.ECHOBOUND_SONIC_DAMAGE_PER_PULSE);
                hurtIgnoringVanillaInterval(combo.target(), source, damage);
                combo.incrementPulses();

                // Keep the repeated damage legible without spawning a full boom
                // particle and sound on every one of the fifty server ticks.
                if (combo.pulsesDealt() % 5 == 0) {
                    Vec3 impact = combo.target().getEyePosition();
                    level.sendParticles(ParticleTypes.SCULK_SOUL,
                        impact.x, impact.y, impact.z, 2,
                        0.12D, 0.12D, 0.12D, 0.01D);
                }
            }

            if (combo.pulsesDealt() >= LastEchoProfile.ECHOBOUND_SONIC_DAMAGE_PULSES
                || !combo.target().isAlive()) {
                iterator.remove();
            }
        }
    }

    public static void renderCharge(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        level.sendParticles(ParticleTypes.SCULK_SOUL, eye.x, eye.y, eye.z,
            3, 0.22D, 0.22D, 0.22D, 0.02D);
        level.sendParticles(ParticleTypes.PORTAL, eye.x, eye.y, eye.z,
            2, 0.18D, 0.18D, 0.18D, 0.03D);
    }

    /** Charge cue for Echo Wave mode: lateral crescent sparks instead of Sonic energy. */
    public static void renderWaveCharge(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 forward = user.getLookAngle();
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < 0.001D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize();
        Vec3 right = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        Vec3 leftSpark = eye.add(horizontal.scale(0.75D)).add(right.scale(-0.65D));
        Vec3 rightSpark = eye.add(horizontal.scale(0.75D)).add(right.scale(0.65D));
        level.sendParticles(ParticleTypes.END_ROD,
            leftSpark.x, leftSpark.y, leftSpark.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
        level.sendParticles(ParticleTypes.END_ROD,
            rightSpark.x, rightSpark.y, rightSpark.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
            eye.x + horizontal.x, eye.y, eye.z + horizontal.z, 2,
            0.28D, 0.12D, 0.28D, 0.01D);
    }

    private static void hurtIgnoringVanillaInterval(LivingEntity target, DamageSource source, float amount) {
        // Reset only for this weapon's explicitly generated echo hit. Global hurt
        // timing remains untouched, preserving other mods' combat behaviour.
        target.invulnerableTime = 0;
        target.hurt(source, amount);
    }

    private static float openingOrNormalDamage(int zeroBasedHitIndex, float normalDamage) {
        return zeroBasedHitIndex < LastEchoProfile.ECHOBOUND_EMPOWERED_OPENING_HITS
            ? LastEchoProfile.ECHOBOUND_EMPOWERED_OPENING_DAMAGE
            : normalDamage;
    }

    private static boolean isNearBeam(Vec3 start, Vec3 look, Entity entity) {
        Vec3 center = entity.getBoundingBox().getCenter();
        double along = center.subtract(start).dot(look);
        if (along < 0.0D || along > LastEchoProfile.ECHOBOUND_SONIC_RANGE) return false;
        Vec3 closest = start.add(look.scale(along));
        // The Echobound beam is intentionally a wide shockwave, not a thin ray.
        // Keep this in lockstep with the expanded target query above (5x the old width).
        double allowance = Math.max(6.75D, entity.getBbWidth() * 3.75D + 4.0D);
        return center.distanceToSqr(closest) <= allowance * allowance;
    }

    private static double beamDistance(Vec3 start, Vec3 look, Entity entity) {
        return entity.getBoundingBox().getCenter().subtract(start).dot(look);
    }

    private static void renderBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = Math.max(1.0D, delta.length());
        Vec3 forward = delta.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.001D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        // The hit cylinder is roughly five times wider than Resonant Echo. Render
        // nine parallel Sonic Boom lanes so the warning matches the real damage area.
        double inner = 3.2D;
        double outer = 5.2D;
        Vec3 diagonalA = right.add(up).normalize().scale(outer);
        Vec3 diagonalB = right.subtract(up).normalize().scale(outer);
        Vec3[] offsets = {
            Vec3.ZERO,
            right.scale(inner), right.scale(-inner),
            up.scale(inner), up.scale(-inner),
            diagonalA, diagonalA.scale(-1.0D),
            diagonalB, diagonalB.scale(-1.0D)
        };

        double spacing = 4.0D;
        int points = Math.min(32, (int)Math.ceil(length / spacing));
        for (int i = 1; i <= points; i++) {
            Vec3 center = start.add(forward.scale(Math.min(length, i * spacing)));
            for (Vec3 offset : offsets) {
                Vec3 point = center.add(offset);
                level.sendParticles(ParticleTypes.SONIC_BOOM,
                    point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                center.x, center.y, center.z, 4, outer * 0.35D, outer * 0.35D, outer * 0.35D, 0.02D);
        }
    }

    private static void renderEchoImpact(ServerLevel level, LivingEntity target,
                                         Vec3 originalImpact, int hitIndex) {
        // Keep the effect within roughly two or three texture pixels of the hit.
        // Later echo pulses follow the target vertically but retain the original
        // horizontal impact, preventing a large particle shell around the entity.
        double y = target.getY() + target.getBbHeight() * 0.58D;
        int count = 2 + level.random.nextInt(3);
        double spread = 0.10D + level.random.nextDouble() * 0.07D;
        level.sendParticles(hitIndex % 4 == 3 ? ParticleTypes.SCULK_SOUL : ParticleTypes.SCULK_CHARGE_POP,
            originalImpact.x, y, originalImpact.z,
            count, spread, spread, spread, 0.005D);
    }

    private static void playSlashSound(ServerLevel level, LivingEntity target, int hitIndex) {
        double x = target.getX();
        double y = target.getY(0.55D);
        double z = target.getZ();
        switch (hitIndex) {
            case 0 -> level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.72F, 0.78F);
            case 1 -> level.playSound(null, x, y, z, SoundEvents.SCULK_CLICKING,
                SoundSource.PLAYERS, 0.78F, 1.18F);
            case 2 -> level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 0.70F, 0.96F);
            case 3 -> level.playSound(null, x, y, z, SoundEvents.WARDEN_ATTACK_IMPACT,
                SoundSource.PLAYERS, 0.68F, 1.30F);
            default -> level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.82F, 1.46F);
        }
    }

    private static final class ForwardWave {
        private final ServerPlayer owner;
        private final ServerLevel level;
        private final Vec3 origin;
        private final Vec3 forward;
        private final double range;
        private int ageTicks;

        private ForwardWave(ServerPlayer owner, ServerLevel level, Vec3 origin, Vec3 forward,
                            double range, int ageTicks) {
            this.owner = owner;
            this.level = level;
            this.origin = origin;
            this.forward = forward;
            this.range = range;
            this.ageTicks = ageTicks;
        }

        ServerPlayer owner() { return owner; }
        ServerLevel level() { return level; }
        Vec3 origin() { return origin; }
        Vec3 forward() { return forward; }
        double range() { return range; }
        int ageTicks() { return ageTicks; }
        void advanceAge() { ageTicks++; }
    }

    private static final class EchoCombo {
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final Vec3 impact;
        private int hitIndex;
        private int ageTicks;

        private EchoCombo(LivingEntity attacker, LivingEntity target, Vec3 impact, int hitIndex, int ageTicks) {
            this.attacker = attacker;
            this.target = target;
            this.impact = impact;
            this.hitIndex = hitIndex;
            this.ageTicks = ageTicks;
        }

        LivingEntity attacker() { return attacker; }
        LivingEntity target() { return target; }
        Vec3 impact() { return impact; }
        int hitIndex() { return hitIndex; }
        void setHitIndex(int value) { hitIndex = value; }
        int ageTicks() { return ageTicks; }
        void advanceAge() { ageTicks++; }
    }

    private static final class SonicPulseCombo {
        private final ServerPlayer owner;
        private final LivingEntity target;
        private int pulsesDealt;

        private SonicPulseCombo(ServerPlayer owner, LivingEntity target, int pulsesDealt) {
            this.owner = owner;
            this.target = target;
            this.pulsesDealt = pulsesDealt;
        }

        ServerPlayer owner() { return owner; }
        LivingEntity target() { return target; }
        int pulsesDealt() { return pulsesDealt; }
        void incrementPulses() { pulsesDealt++; }
    }
}
