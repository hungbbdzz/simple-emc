package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tier III promotion boss: a giant shulker fortress with gravity and projectile-control phases. */
public final class AethericBulwarkEntity extends Shulker {
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.velorise_emc.aetheric_bulwark"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private int skillClock;
    private int phase;
    private BlockPos arena = BlockPos.ZERO;
    private static final double ARENA_RADIUS = 25.0D;

    public AethericBulwarkEntity(EntityType<? extends Shulker> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("entity.velorise_emc.aetheric_bulwark"));
        setCustomNameVisible(true);
        xpReward = 180;
    }

    public void bindArena(BlockPos center) { arena = center.immutable(); }

    public static AttributeSupplier.Builder createAttributes() {
        return Shulker.createAttributes()
            .add(Attributes.MAX_HEALTH, 2200.0D)
            .add(Attributes.ARMOR, 18.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }

    private int lastPhase = 1;

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        enforceArena(level());
        phase = getHealth() > getMaxHealth()*0.70F ? 1 : getHealth() > getMaxHealth()*0.35F ? 2 : 3;
        if (phase > lastPhase) {
            lastPhase = phase;
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, blockPosition(), SoundEvents.SHULKER_CLOSE, SoundSource.HOSTILE, 3.5F, 0.55F);
                sl.playSound(null, blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.HOSTILE, 2.2F, 0.65F);
                sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.75F);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0D, getZ(), 80, 1.5D, 1.5D, 1.5D, 0.08D);
            }
        }
        if (getTarget() instanceof Player tracked && insideArena(tracked.position(), ARENA_RADIUS)) {
            getLookControl().setLookAt(tracked, 180.0F, 180.0F);
        }
        if (++skillClock < (phase == 1 ? 80 : phase == 2 ? 60 : 40)) return;
        skillClock = 0;
        if (!(level() instanceof ServerLevel level)) return;
        Player nearest = level.getNearestPlayer(this, 64.0D);
        if (!(nearest instanceof ServerPlayer target) || !insideArena(target.position(), ARENA_RADIUS)) return;
        setTarget(target);
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        int choice = random.nextInt(phase == 1 ? 4 : phase == 2 ? 6 : 7);
        if (choice == 0) orbitalBarrage(level, target);
        else if (choice == 1) gravityInversion(level, target);
        else if (choice == 2) spatialSwap(level, target);
        else if (choice == 3) fakeShadowDecoy(level, target);
        else if (choice == 4) gravitySingularity(level, target);
        else if (choice == 5) ironShellGuard(level);
        else chargedBurst(level, target);
    }

    private void orbitalBarrage(ServerLevel level, ServerPlayer target) {
        level.playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2.2F, .55F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12.0;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * 4, getY() + 1.5, getZ() + Math.sin(a) * 4, 8, .15, .15, .15, .03);
        }
        delayedLevitationHit(level, target, 18.0F, 35, 45);
    }

    /** High Gravity Inversion: lifts player high up into the stratosphere with NO Slow Falling / Feather Falling protection! */
    private void gravityInversion(ServerLevel level, ServerPlayer target) {
        level.playSound(null, target.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.0F, 1.6F);
        // Strip Slow Falling so player takes FULL catastrophic fall damage upon landing!
        target.removeEffect(MobEffects.SLOW_FALLING);
        // High Levitation VII (amplifier 6) launches player Y + 35..45 blocks into the sky!
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 6));
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY(), target.getZ(), 120, 2.0D, 3.0D, 2.0D, 0.25D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1.0D, target.getZ(), 80, 1.5D, 2.0D, 1.5D, 0.15D);
    }

    /** Spatial Swap: Swaps boss position directly with player position (NO random player teleports). */
    private void spatialSwap(ServerLevel level, ServerPlayer target) {
        Vec3 oldBoss = position();
        Vec3 targetPos = target.position();
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.7F);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.9F);
        teleportTo(targetPos.x, targetPos.y, targetPos.z);
        target.teleportTo(oldBoss.x, oldBoss.y, oldBoss.z);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, oldBoss.x, oldBoss.y + 1.0D, oldBoss.z, 80, 1.0D, 1.0D, 1.0D, 0.12D);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, targetPos.x, targetPos.y + 1.0D, targetPos.z, 80, 1.0D, 1.0D, 1.0D, 0.12D);
    }

    /** Shulkerling Ambush: spawns a normal Shulker minion behind the target instead of a player-shaped decoy. */
    private void fakeShadowDecoy(ServerLevel level, ServerPlayer target) {
        level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 2.5F, 0.75F);
        Vec3 offset = target.getLookAngle().reverse().scale(2.5D);
        Vec3 spawnPos = target.position().add(offset);

        Shulker minion = EntityType.SHULKER.create(level);
        if (minion == null) return;

        minion.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, target.getYRot(), 0.0F);
        minion.setCustomName(Component.literal("§dAetheric Shulkerling"));
        minion.setCustomNameVisible(true);
        minion.setHealth(Math.min(20.0F, minion.getMaxHealth()));
        minion.setTarget(target);
        minion.addTag("AethericShulkerling");
        level.addFreshEntity(minion);

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z,
            100, 1.0D, 1.5D, 1.0D, 0.08D);
        level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z,
            30, 0.6D, 0.8D, 0.6D, 0.03D);
    }

    /** Gravity Singularity: Pulls players towards a crushing purple void well at target's feet! */
    private void gravitySingularity(ServerLevel level, ServerPlayer target) {
        Vec3 center = target.position();
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5F, 1.5F);
        AABB box = target.getBoundingBox().inflate(14.0D, 6.0D, 14.0D);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            Vec3 dir = center.subtract(p.position()).normalize().scale(0.55D);
            p.setDeltaMovement(p.getDeltaMovement().add(dir.x, 0.25D, dir.z));
            p.hurt(level.damageSources().mobAttack(this), 16.0F);
            p.hasImpulse = true;
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 1.0D, center.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1.0D, center.z, 160, 4.0D, 2.0D, 4.0D, 0.20D);
    }

    /** Iron Shell Guard: Shulker closes shell, gains Resistance V, and fires 8 radial homing bullets! */
    private void ironShellGuard(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.SHULKER_CLOSE, SoundSource.HOSTILE, 3.0F, 0.6F);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 4));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2.0D / 8.0D;
            net.minecraft.world.entity.projectile.ShulkerBullet bullet = new net.minecraft.world.entity.projectile.ShulkerBullet(level, this, getTarget(), net.minecraft.core.Direction.UP.getAxis());
            bullet.moveTo(getX() + Math.cos(angle) * 1.5D, getY() + 1.2D, getZ() + Math.sin(angle) * 1.5D, (float)Math.toDegrees(angle), 0.0F);
            level.addFreshEntity(bullet);
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0D, getZ(), 100, 2.0D, 2.0D, 2.0D, 0.15D);
    }

    private void chargedBurst(ServerLevel level, ServerPlayer target) {
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5F, 1.45F);
        delayedLevitationHit(level, target, 34.0F, 22, 70);
    }

    private void delayedLevitationHit(ServerLevel level, ServerPlayer target, float damage, int visualTicks, int duration) {
        level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        target.hurt(level.damageSources().mobAttack(this), damage);
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration, 0));
    }

    private void enforceArena(Level level) {
        if (arena.equals(BlockPos.ZERO)) return;
        if (!insideArena(position(), ARENA_RADIUS + 3.0D)) {
            teleportTo(arena.getX() + 0.5D, arena.getY() + 2.0D, arena.getZ() + 0.5D);
            setTarget(null);
            return;
        }
        if (getTarget() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) setTarget(null);
    }

    private boolean insideArena(Vec3 position, double radius) {
        if (arena.equals(BlockPos.ZERO)) return true;
        double dx = position.x - (arena.getX() + 0.5D);
        double dz = position.z - (arena.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHULKER_HURT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) return false;
        if (!level().isClientSide && source.getEntity() != null) {
            level().playSound(null, blockPosition(), SoundEvents.SHULKER_HURT, SoundSource.HOSTILE, 2.5F, 0.75F);
        }
        return super.hurt(source, amount);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("Arena", arena.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
    }

    @Override public void die(DamageSource source) {
        if(level() instanceof ServerLevel level) {
            level.playSound(null,blockPosition(),SoundEvents.END_PORTAL_SPAWN,SoundSource.HOSTILE,3F,.45F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY()+1,getZ(),3,1,1,1,0);
        }
        super.die(source);
    }
}
