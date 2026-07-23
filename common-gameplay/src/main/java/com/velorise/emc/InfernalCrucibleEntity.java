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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tier II promotion boss: a flying Nether Blaze deity with fireball barrages, distance strafing, and minion summoning. */
public final class InfernalCrucibleEntity extends Blaze {
    private static final double ARENA_RADIUS = 34.0D;
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.velorise_emc.infernal_crucible"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private BlockPos arena = BlockPos.ZERO;
    private int skillClock;
    private int phase = 1;
    private int lastPhase = 1;

    public InfernalCrucibleEntity(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("entity.velorise_emc.infernal_crucible"));
        setCustomNameVisible(true);
        xpReward = 180;
        setNoGravity(true);
    }

    public void bindArena(BlockPos center) { arena = center.immutable(); }

    public static AttributeSupplier.Builder createAttributes() {
        return Blaze.createAttributes()
            .add(Attributes.MAX_HEALTH, 1800.0D)
            .add(Attributes.ATTACK_DAMAGE, 24.0D)
            .add(Attributes.ARMOR, 18.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.32D)
            .add(Attributes.FLYING_SPEED, 0.35D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }

    private int queuedFireballs = 0;
    private int fireballLaunchTimer = 0;

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        enforceArena();
        phase = getHealth() > getMaxHealth() * 0.66F ? 1 : getHealth() > getMaxHealth() * 0.33F ? 2 : 3;

        // Process gradual fireball throwing from above head
        tickQueuedFireballs();

        if (phase > lastPhase) {
            lastPhase = phase;
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.5F, 0.65F);
                sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
                sl.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.7F);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 80, 1.5D, 1.5D, 1.5D, 0.05D);
            }
        }

        // Smooth levitating flight & distance keeping (maintain 12..16 blocks standoff distance)
        if (getTarget() instanceof Player tracked && insideArena(tracked.position(), ARENA_RADIUS)) {
            getLookControl().setLookAt(tracked, 180.0F, 180.0F);
            flyKeepDistance(tracked);
        }

        if (++skillClock < (phase == 1 ? 70 : phase == 2 ? 50 : 35)) return;
        skillClock = 0;

        if (!(level() instanceof ServerLevel serverLevel)) return;
        Player nearest = serverLevel.getNearestPlayer(this, 56.0D);
        if (!(nearest instanceof ServerPlayer target) || !insideArena(target.position(), ARENA_RADIUS)) return;
        setTarget(target);
        getLookControl().setLookAt(target, 180.0F, 180.0F);

        int skill = random.nextInt(phase == 1 ? 5 : phase == 2 ? 6 : 7);
        if (skill == 0) chargeCrownFireballs(serverLevel, target);
        else if (skill == 1) fireballNova(serverLevel);
        else if (skill == 2) infernalDash(serverLevel, target);
        else if (skill == 3) summonMiniBlazes(serverLevel);
        else if (skill == 4) lavaGeyser(serverLevel, target);
        else if (skill == 5) heavyFireballVolley(serverLevel, target);
        else whiteHeat(serverLevel);
    }

    /** Gradually launches queued fireballs from above the boss's head one by one. */
    private void tickQueuedFireballs() {
        if (queuedFireballs <= 0 || !(level() instanceof ServerLevel level)) return;

        if (tickCount % 2 == 0) {
            for (int i = 0; i < queuedFireballs; i++) {
                double angle = (tickCount * 0.1D) + (i * Math.PI * 2.0D / Math.max(1, queuedFireballs));
                double px = getX() + Math.cos(angle) * 1.2D;
                double py = getY() + 2.4D + Math.sin(tickCount * 0.15D) * 0.2D;
                double pz = getZ() + Math.sin(angle) * 1.2D;
                level.sendParticles(ParticleTypes.FLAME, px, py, pz, 2, 0.05D, 0.05D, 0.05D, 0.01D);
            }
        }

        if (--fireballLaunchTimer > 0) return;
        fireballLaunchTimer = 7;

        Player target = getTarget() instanceof Player p && p.isAlive() ? p : level.getNearestPlayer(this, 48.0D);
        if (target == null) return;

        Vec3 eyePos = target.getEyePosition();
        Vec3 spawnPos = new Vec3(getX(), getY() + 2.4D, getZ());
        Vec3 dir = eyePos.subtract(spawnPos).normalize();

        SmallFireball fireball = EntityType.SMALL_FIREBALL.create(level);
        if (fireball != null) {
            fireball.setOwner(this);
            fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            fireball.setDeltaMovement(dir.scale(1.15D));
            level.addFreshEntity(fireball);
            level.playSound(null, getX(), getY() + 2.4D, getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 1.0F + random.nextFloat() * 0.3F);
        }

        queuedFireballs--;
    }

    /** Spawns a crown of 4..8 fireballs hovering above the boss's head, to be launched gradually. */
    private void chargeCrownFireballs(ServerLevel level, ServerPlayer target) {
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 3.0F, 0.7F);
        int add = 4 + phase * 2;
        queuedFireballs = Math.min(12, queuedFireballs + add);
        fireballLaunchTimer = 5;
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2.4D, getZ(), 45, 1.2D, 0.5D, 1.2D, 0.08D);
    }

    /** Infernal Dash: Charges briefly in mid-air then dashes directly at the player at high speed! */
    private void infernalDash(ServerLevel level, ServerPlayer target) {
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.5F, 0.8F);

        Vec3 dashDir = target.position().add(0, 0.5D, 0).subtract(position()).normalize();
        setDeltaMovement(dashDir.scale(1.85D));
        hasImpulse = true;

        for (int step = 1; step <= 12; step++) {
            double tx = getX() + dashDir.x * (step * 0.8D);
            double ty = getY() + dashDir.y * (step * 0.8D);
            double tz = getZ() + dashDir.z * (step * 0.8D);
            level.sendParticles(ParticleTypes.LAVA, tx, ty, tz, 6, 0.3D, 0.3D, 0.3D, 0.05D);
            level.sendParticles(ParticleTypes.FLAME, tx, ty, tz, 10, 0.4D, 0.4D, 0.4D, 0.08D);
        }

        if (distanceToSqr(target) <= 12.0D * 12.0D) {
            target.hurt(level.damageSources().mobAttack(this), 22.0F + phase * 3.0F);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            target.push(dashDir.x * 1.5D, 0.45D, dashDir.z * 1.5D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.2F);
        }
    }

    /** Smooth levitating flight strafing keeping 12..16 blocks distance in mid-air. */
    private void flyKeepDistance(Player target) {
        Vec3 bossPos = position();
        Vec3 targetPos = target.position();
        double dist = bossPos.distanceTo(targetPos);

        double idealDist = 13.5D;
        double targetY = arena.equals(BlockPos.ZERO) ? target.getY() + 4.5D : arena.getY() + 5.5D;
        double dy = (targetY - getY()) * 0.1D;

        double strafeAngle = (tickCount * 0.05D);
        double strafeX = Math.cos(strafeAngle) * 0.25D;
        double strafeZ = Math.sin(strafeAngle) * 0.25D;

        Vec3 moveVec;
        if (dist < 10.0D) {
            // Push away from target if too close
            Vec3 away = bossPos.subtract(targetPos).normalize().scale(0.35D);
            moveVec = new Vec3(away.x + strafeX, dy, away.z + strafeZ);
        } else if (dist > 18.0D) {
            // Move toward target if too far
            Vec3 toward = targetPos.subtract(bossPos).normalize().scale(0.35D);
            moveVec = new Vec3(toward.x + strafeX, dy, toward.z + strafeZ);
        } else {
            // Circle around target at ideal standoff distance
            moveVec = new Vec3(strafeX, dy, strafeZ);
        }

        setDeltaMovement(moveVec);
        hasImpulse = true;

        if (tickCount % 4 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.3D, getZ(), 4, 0.3D, 0.3D, 0.3D, 0.02D);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.2D, getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
        }
    }

    /** Rapid Fireball Barrage: Fires a stream of rapid SmallFireballs directly at target player. */
    private void rapidFireballBarrage(ServerLevel level, ServerPlayer target) {
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5F, 1.0F);
        Vec3 dir = target.getEyePosition().subtract(getX(), getY() + 1.2D, getZ()).normalize();
        int count = 3 + phase;
        for (int i = 0; i < count; i++) {
            double spreadX = dir.x + (random.nextDouble() - 0.5D) * 0.1D;
            double spreadY = dir.y + (random.nextDouble() - 0.5D) * 0.1D;
            double spreadZ = dir.z + (random.nextDouble() - 0.5D) * 0.1D;
            Vec3 spreadDir = new Vec3(spreadX, spreadY, spreadZ).normalize();
            SmallFireball fireball = EntityType.SMALL_FIREBALL.create(level);
            if (fireball != null) {
                fireball.setOwner(this);
                fireball.setPos(getX(), getY() + 1.2D, getZ());
                fireball.setDeltaMovement(spreadDir.scale(1.1D));
                level.addFreshEntity(fireball);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.2D, getZ(), 40, 0.8D, 0.8D, 0.8D, 0.12D);
    }

    /** 360-Degree Fireball Nova: Fires 12 fireballs radiating in a full circle! */
    private void fireballNova(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 2.8F, 0.9F);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0D / 12.0D;
            Vec3 dir = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            SmallFireball fireball = EntityType.SMALL_FIREBALL.create(level);
            if (fireball != null) {
                fireball.setOwner(this);
                fireball.setPos(getX(), getY() + 1.0D, getZ());
                fireball.setDeltaMovement(dir.scale(0.85D));
                level.addFreshEntity(fireball);
            }
        }
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0D, getZ(), 60, 2.0D, 1.0D, 2.0D, 0.10D);
    }

    /** Summon Mini Blazes: Spawns 2..4 aggressive Blaze minions around the boss. */
    private void summonMiniBlazes(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 3.5F, 1.2F);
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0D / count;
            double ox = Math.cos(angle) * 3.0D;
            double oz = Math.sin(angle) * 3.0D;
            Blaze minion = EntityType.BLAZE.create(level);
            if (minion != null) {
                minion.moveTo(getX() + ox, getY() + 0.5D, getZ() + oz, random.nextFloat() * 360.0F, 0.0F);
                minion.setCustomName(Component.literal("§cInfernal Minion"));
                minion.setCustomNameVisible(true);
                level.addFreshEntity(minion);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0D, getZ(), 100, 3.0D, 2.0D, 3.0D, 0.15D);
    }

    /** Lava Geyser: Targets player location with erupting lava/magma pillars. */
    private void lavaGeyser(ServerLevel level, ServerPlayer target) {
        level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.8F, 0.7F);
        Vec3 tPos = target.position();
        level.sendParticles(ParticleTypes.LAVA, tPos.x, tPos.y + 0.4D, tPos.z, 90, 3.0D, 0.3D, 3.0D, 0.1D);
        level.sendParticles(ParticleTypes.FLAME, tPos.x, tPos.y + 0.2D, tPos.z, 60, 2.5D, 1.5D, 2.5D, 0.08D);
        if (distanceToSqr(target) <= 24.0D * 24.0D) {
            target.hurt(level.damageSources().mobAttack(this), 18.0F + phase * 2.0F);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
        }
    }

    /** Heavy Fireball Volley: Fires a massive 3-way LargeFireball cluster! */
    private void heavyFireballVolley(ServerLevel level, ServerPlayer target) {
        level.playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 3.0F, 0.75F);
        Vec3 dir = target.getEyePosition().subtract(getX(), getY() + 1.5D, getZ()).normalize();
        for (double offset : new double[]{-0.25D, 0.0D, 0.25D}) {
            Vec3 spreadDir = new Vec3(dir.x + offset, dir.y, dir.z - offset).normalize();
            LargeFireball large = EntityType.FIREBALL.create(level);
            if (large != null) {
                large.setOwner(this);
                large.setPos(getX() + offset * 2.0D, getY() + 1.5D, getZ() + offset * 2.0D);
                large.setDeltaMovement(spreadDir.scale(1.1D));
                level.addFreshEntity(large);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.5D, getZ(), 4, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    private void whiteHeat(ServerLevel level) {
        if (phase < 3) return;
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.5F, 1.4F);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.5D, getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0D, getZ(), 160, 6.0D, 2.0D, 6.0D, 0.08D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(blockPosition()).inflate(16.0D))) {
            if (!insideArena(player.position(), ARENA_RADIUS)) continue;
            player.hurt(level.damageSources().mobAttack(this), 24.0F);
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 100));
        }
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
    }

    private void enforceArena() {
        if (arena.equals(BlockPos.ZERO)) return;
        if (!insideArena(position(), ARENA_RADIUS + 5.0D)) {
            teleportTo(arena.getX() + 0.5D, arena.getY() + 5.0D, arena.getZ() + 0.5D);
            getNavigation().stop();
            setTarget(null);
        }
        if (getTarget() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    private boolean insideArena(Vec3 pos, double radius) {
        if (arena.equals(BlockPos.ZERO)) return true;
        double dx = pos.x - (arena.getX() + 0.5D);
        double dz = pos.z - (arena.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        if (source.getEntity() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) return false;
        if (!level().isClientSide && source.getEntity() != null) {
            level().playSound(null, blockPosition(), SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, 2.5F, 0.7F);
        }
        return super.hurt(source, amount);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putLong("Arena", arena.asLong()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena")); }

    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.BLAZE_DEATH, SoundSource.HOSTILE, 4.0F, 0.45F);
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0D, getZ(), 220, 4.0D, 3.0D, 4.0D, 0.15D);
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1.0D, getZ(), new ItemStack(Items.NETHER_STAR)));
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1.0D, getZ(), new ItemStack(Items.BLAZE_ROD, 8 + random.nextInt(5))));
        }
        super.die(source);
    }
}
