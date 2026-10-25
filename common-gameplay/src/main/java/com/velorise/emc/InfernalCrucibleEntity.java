package com.velorise.emc;

import com.velorise.emc.common.GameplayConfig;
import com.velorise.emc.common.ThreatScalingProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Tier II promotion boss: a flying Nether Blaze deity with fireball barrages, distance strafing, and minion summoning. */
public final class InfernalCrucibleEntity extends Blaze {
    public static final String MINION_TAG = "velorise_emc.infernal_minion";
    private static final double ARENA_RADIUS = 34.0D;
    private static final float BASE_ENCOUNTER_HEALTH = 1_800.0F;
    /** Vanilla clamps MAX_HEALTH to 1024, so damage is resolved against a 1000-HP physical segment. */
    private static final float PHYSICAL_HEALTH_SEGMENT = 1_000.0F;
    private static final int MAX_ACTIVE_MINIONS = 6;
    private static final byte EVENT_CAST_PULSE = 60;
    private static final byte EVENT_DASH_BURST = 61;

    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.velorise_emc.infernal_crucible"),
        BossEvent.BossBarColor.RED,
        BossEvent.BossBarOverlay.NOTCHED_10
    );
    private BlockPos arena = BlockPos.ZERO;
    private boolean initializedHealth;
    private boolean hadCombatEngagement;
    private int inactiveTicks;
    private float encounterHealth = BASE_ENCOUNTER_HEALTH;
    private float encounterMaxHealth = BASE_ENCOUNTER_HEALTH;
    private int skillClock;
    private int phase = 1;
    private int lastPhase = 1;
    private int lastSkill = -1;

    private int queuedFireballs;
    private int fireballLaunchTimer;
    private int flareTicks;

    private int dashTicks;
    private Vec3 dashVelocity = Vec3.ZERO;
    private UUID dashTargetId;
    private boolean dashDealtDamage;
    private UUID retaliationTargetId;
    private int lookAroundTimer;
    private float idleYawOffset;

    private int clientCastPulseTicks;
    private int clientDashTicks;

    private static net.minecraft.network.syncher.EntityDataAccessor<Byte> BLAZE_FLAGS;
    static {
        try {
            for (java.lang.reflect.Field f : Blaze.class.getDeclaredFields()) {
                if (f.getType() == net.minecraft.network.syncher.EntityDataAccessor.class) {
                    f.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    net.minecraft.network.syncher.EntityDataAccessor<Byte> accessor =
                        (net.minecraft.network.syncher.EntityDataAccessor<Byte>) f.get(null);
                    BLAZE_FLAGS = accessor;
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }

    public void setCharged(boolean charged) {
        if (BLAZE_FLAGS != null) {
            byte b = this.entityData.get(BLAZE_FLAGS);
            if (charged) b = (byte) (b | 1);
            else b = (byte) (b & -2);
            this.entityData.set(BLAZE_FLAGS, b);
        }
    }

    private final List<PendingGeyser> pendingGeysers = new ArrayList<>();

    public InfernalCrucibleEntity(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("entity.velorise_emc.infernal_crucible"));
        setCustomNameVisible(true);
        setPersistenceRequired();
        xpReward = 180;
        setNoGravity(true);
    }

    public void bindArena(BlockPos center) {
        arena = center.immutable();
    }

    public int clientCastPulseTicks() {
        return clientCastPulseTicks;
    }

    public int clientDashTicks() {
        return clientDashTicks;
    }

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

    @Override
    protected void registerGoals() {
        // Omit Vanilla BlazeAttackGoal and random stroll so custom boss AI has sole control over flight and attacks.
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CAST_PULSE) {
            clientCastPulseTicks = 18;
        } else if (id == EVENT_DASH_BURST) {
            clientDashTicks = 14;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (clientCastPulseTicks > 0) clientCastPulseTicks--;
            if (clientDashTicks > 0) clientDashTicks--;
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;

        ensureHealthInitialized(serverLevel);
        float healthRatio = encounterMaxHealth <= 0.0F ? 0.0F : Mth.clamp(encounterHealth / encounterMaxHealth, 0.0F, 1.0F);
        bossBar.setProgress(healthRatio);
        enforceArena(serverLevel);

        if (flareTicks > 0) flareTicks--;

        phase = healthRatio > 0.66F ? 1 : healthRatio > 0.33F ? 2 : 3;
        if (phase < lastPhase) {
            lastPhase = phase;
        } else if (phase > lastPhase) {
            lastPhase = phase;
            flareTicks = 60;
            serverLevel.broadcastEntityEvent(this, EVENT_CAST_PULSE);
            serverLevel.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.5F, 0.65F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.7F);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 80, 1.5D, 1.5D, 1.5D, 0.05D);
        }

        tickQueuedFireballs(serverLevel);
        tickPendingGeysers(serverLevel);

        LivingEntity target = findCombatTarget(serverLevel);
        setTarget(target);

        // Drive authentic Vanilla Blaze charged/on-fire visual state so client renders intense flames and rod flare
        boolean visiblyCharged = target != null && (phase >= 2 || queuedFireballs > 0 || dashTicks > 0 || flareTicks > 0 || skillClock > 32);
        setCharged(visiblyCharged);
        setSharedFlagOnFire(visiblyCharged || target != null);

        if (dashTicks > 0) {
            tickInfernalDash(serverLevel);
            return;
        }

        if (target != null) {
            hadCombatEngagement = true;
            inactiveTicks = 0;
            smoothFaceTarget(target, 32.0F, 28.0F);
            flyKeepDistance(serverLevel, target);
        } else {
            inactiveTicks++;
            queuedFireballs = 0;
            pendingGeysers.clear();
            if (hadCombatEngagement && inactiveTicks >= 200 && encounterHealth < encounterMaxHealth) {
                encounterHealth = Math.min(encounterMaxHealth, encounterHealth + encounterMaxHealth * 0.025F);
                if (encounterHealth >= encounterMaxHealth) {
                    hadCombatEngagement = false;
                    phase = 1;
                    lastPhase = 1;
                }
            }
            wanderAroundArena(serverLevel);
            return;
        }

        int cooldown = phase == 1 ? 68 : phase == 2 ? 48 : 34;
        if (++skillClock < cooldown) return;
        skillClock = 0;

        int poolSize = phase == 1 ? 6 : phase == 2 ? 7 : 8;
        int skill = random.nextInt(poolSize);
        if (skill == lastSkill) {
            skill = (skill + 1 + random.nextInt(poolSize - 1)) % poolSize;
        }
        lastSkill = skill;
        flareTicks = Math.max(flareTicks, 26);
        serverLevel.broadcastEntityEvent(this, EVENT_CAST_PULSE);
        swing(InteractionHand.MAIN_HAND);

        switch (skill) {
            case 0 -> chargeCrownFireballs(serverLevel);
            case 1 -> fireballNova(serverLevel);
            case 2 -> infernalDash(serverLevel, target);
            case 3 -> summonMiniBlazes(serverLevel, target);
            case 4 -> lavaGeyser(serverLevel, target);
            case 5 -> rapidFireballBarrage(serverLevel, target);
            case 6 -> heavyFireballVolley(serverLevel, target);
            default -> whiteHeat(serverLevel);
        }
    }

    private void smoothFaceTarget(LivingEntity target, float maxYawTurn, float maxPitchTurn) {
        Vec3 eye = target.getEyePosition();
        getLookControl().setLookAt(eye.x, eye.y, eye.z, maxYawTurn, maxPitchTurn);
        double dx = eye.x - getX();
        double dz = eye.z - getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.26F, getYRot(), desiredYaw));
            yBodyRot = Mth.rotLerp(0.22F, yBodyRot, getYRot());
            yHeadRot = Mth.rotLerp(0.32F, yHeadRot, desiredYaw);
        }
    }

    private void ensureHealthInitialized(ServerLevel serverLevel) {
        if (!initializedHealth) {
            float bonus = 0.0F;
            GameplayConfig.BossPlayerScaling cfg = GameplayConfig.current().world.bossPlayerScaling;
            if (cfg.enabled && serverLevel.getServer() != null) {
                int online = 0;
                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    if (!player.isCreative() && !player.isSpectator()) online++;
                }
                int counted = ThreatScalingProfile.clampPlayerCount(online);
                if (counted > 0) {
                    bonus = (float) (ThreatScalingProfile.bossBonusHealth(counted) * 0.5D);
                }
            }
            encounterMaxHealth = BASE_ENCOUNTER_HEALTH + bonus;
            encounterHealth = encounterMaxHealth;
            if (getAttribute(Attributes.MAX_HEALTH) != null) {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHYSICAL_HEALTH_SEGMENT);
            }
            setHealth(PHYSICAL_HEALTH_SEGMENT);
            initializedHealth = true;
        }
    }

    private LivingEntity findCombatTarget(ServerLevel level) {
        LivingEntity current = getTarget();
        if (current != null && isValidCombatTarget(current)) {
            return current;
        }

        if (retaliationTargetId != null) {
            Entity ent = level.getEntity(retaliationTargetId);
            if (ent instanceof LivingEntity le && isValidCombatTarget(le)) {
                return le;
            }
            retaliationTargetId = null;
        }

        Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), 56.0D,
            entity -> entity instanceof ServerPlayer sp && isValidArenaTarget(sp));
        if (nearest instanceof ServerPlayer sp) {
            return sp;
        }

        LivingEntity lastHurt = getLastHurtByMob();
        if (lastHurt != null && isValidCombatTarget(lastHurt)) {
            return lastHurt;
        }

        return null;
    }

    private boolean isValidArenaTarget(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
            && insideArena(player.position(), ARENA_RADIUS + 4.0D);
    }

    private boolean isValidCombatTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == this) return false;
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && insideArena(player.position(), ARENA_RADIUS + 4.0D);
        }
        if (entity.getTags().contains(MINION_TAG)) return false;
        return insideArena(entity.position(), ARENA_RADIUS + 6.0D);
    }

    /** Gradually launches queued fireballs from above the boss's head one by one. */
    private void tickQueuedFireballs(ServerLevel level) {
        if (queuedFireballs <= 0) return;

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

        LivingEntity target = findCombatTarget(level);
        if (target == null) {
            queuedFireballs = 0;
            return;
        }

        Vec3 spawnPos = new Vec3(getX(), getY() + 2.4D, getZ());
        Vec3 leadPos = target.getEyePosition().add(target.getDeltaMovement().scale(4.0D));
        Vec3 dir = leadPos.subtract(spawnPos);
        if (dir.lengthSqr() < 1.0E-4D) dir = new Vec3(0.0D, -1.0D, 0.0D);
        else dir = dir.normalize();

        spawnSmallFireball(level, spawnPos, dir, 1.25D);
        level.playSound(null, getX(), getY() + 2.4D, getZ(), SoundEvents.BLAZE_SHOOT,
            SoundSource.HOSTILE, 2.0F, 1.0F + random.nextFloat() * 0.3F);
        queuedFireballs--;
    }

    /** Spawns a crown of 4..8 fireballs hovering above the boss's head, to be launched gradually. */
    private void chargeCrownFireballs(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 3.0F, 0.7F);
        int add = 4 + phase * 2;
        queuedFireballs = Math.min(12, queuedFireballs + add);
        fireballLaunchTimer = 5;
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2.4D, getZ(), 45, 1.2D, 0.5D, 1.2D, 0.08D);
    }

    /** Infernal Dash: Propels the boss across the arena through the target over 14 ticks. */
    private void infernalDash(ServerLevel level, LivingEntity target) {
        level.broadcastEntityEvent(this, EVENT_DASH_BURST);
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.5F, 0.8F);

        Vec3 aim = target.position().add(0.0D, 0.6D, 0.0D).subtract(position());
        if (aim.lengthSqr() < 1.0E-4D) aim = new Vec3(1.0D, 0.0D, 0.0D);
        dashVelocity = aim.normalize().scale(1.65D + phase * 0.12D);
        dashTicks = 14;
        dashTargetId = target.getUUID();
        dashDealtDamage = false;
        setDeltaMovement(dashVelocity);
        hasImpulse = true;
    }

    private void tickInfernalDash(ServerLevel level) {
        dashTicks--;
        if (!arena.equals(BlockPos.ZERO)) {
            Vec3 nextPos = position().add(dashVelocity);
            if (!insideArena(nextPos, ARENA_RADIUS - 1.5D)) {
                dashTicks = 0;
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }

        setDeltaMovement(dashVelocity);
        hasImpulse = true;
        if (dashVelocity.horizontalDistanceSqr() > 1.0E-4D) {
            float yaw = (float) (Mth.atan2(dashVelocity.z, dashVelocity.x) * (180.0D / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.45F, getYRot(), yaw));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0D, getZ(), 5, 0.35D, 0.35D, 0.35D, 0.05D);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0D, getZ(), 10, 0.45D, 0.45D, 0.45D, 0.06D);

        if (!dashDealtDamage) {
            AABB hitBox = getBoundingBox().inflate(2.2D, 1.8D, 2.2D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox, this::isValidCombatTarget)) {
                dashDealtDamage = true;
                victim.hurt(level.damageSources().mobAttack(this), 22.0F + phase * 3.0F);
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 100));
                Vec3 push = dashVelocity.normalize().scale(1.45D);
                victim.push(push.x, 0.45D, push.z);
                victim.hasImpulse = true;
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.2F);
            }
        }

        if (dashTicks <= 0 && dashTargetId != null) {
            Entity ent = level.getEntity(dashTargetId);
            if (ent instanceof LivingEntity le && isValidCombatTarget(le)) {
                smoothFaceTarget(le, 60.0F, 45.0F);
            }
            dashTargetId = null;
        }
    }

    /** Smooth levitating flight strafing keeping 12..16 blocks distance in mid-air while staying inside the arena. */
    private void flyKeepDistance(ServerLevel level, LivingEntity target) {
        Vec3 bossPos = position();
        Vec3 targetPos = target.position();
        Vec3 toTarget = targetPos.subtract(bossPos);
        Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
        double dist = horizontalToTarget.length();
        Vec3 radialDir = dist > 1.0E-4D ? horizontalToTarget.scale(1.0D / dist) : new Vec3(1.0D, 0.0D, 0.0D);

        // Smoothly vary orbit direction and speed so flight feels organic rather than robotic
        double orbitSign = Math.sin(tickCount * 0.032D) >= 0.0D ? 1.0D : -1.0D;
        Vec3 tangentDir = new Vec3(-radialDir.z * orbitSign, 0.0D, radialDir.x * orbitSign);

        double baseHoverY = arena.equals(BlockPos.ZERO) ? target.getY() + 4.2D : Math.max(arena.getY() + 4.5D, target.getY() + 3.8D);
        double targetY = baseHoverY + Math.sin(tickCount * 0.075D) * 1.15D;
        double dy = Mth.clamp((targetY - getY()) * 0.14D, -0.35D, 0.35D);

        double radialSpeed = 0.0D;
        if (dist < 10.0D) {
            radialSpeed = -0.34D * ((10.0D - dist) / 4.0D + 0.4D);
        } else if (dist > 17.5D) {
            radialSpeed = 0.36D * Math.min(1.5D, (dist - 17.5D) / 6.0D + 0.5D);
        }
        double tangentSpeed = 0.28D + phase * 0.04D;

        Vec3 desiredVel = radialDir.scale(radialSpeed).add(tangentDir.scale(tangentSpeed)).add(0.0D, dy, 0.0D);

        // Prevent pushing the boss out of the arena boundary to force an arena reset.
        if (!arena.equals(BlockPos.ZERO)) {
            Vec3 center = new Vec3(arena.getX() + 0.5D, getY(), arena.getZ() + 0.5D);
            Vec3 nextHorizontal = new Vec3(bossPos.x + desiredVel.x - center.x, 0.0D, bossPos.z + desiredVel.z - center.z);
            double maxOrbitRadius = ARENA_RADIUS - 3.0D;
            if (nextHorizontal.lengthSqr() > maxOrbitRadius * maxOrbitRadius) {
                Vec3 inward = center.subtract(bossPos).normalize().scale(0.38D);
                desiredVel = new Vec3(inward.x + tangentDir.x * tangentSpeed, dy, inward.z + tangentDir.z * tangentSpeed);
            }
        }

        // Smooth velocity damping so flight acceleration/deceleration is silky smooth
        Vec3 moveVec = getDeltaMovement().scale(0.68D).add(desiredVel.scale(0.32D));
        setDeltaMovement(moveVec);
        hasImpulse = true;

        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.3D, getZ(), 4, 0.3D, 0.3D, 0.3D, 0.02D);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.2D, getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
        }
    }

    private void wanderAroundArena(ServerLevel level) {
        if (arena.equals(BlockPos.ZERO)) {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
            return;
        }

        double patrolAngle = (tickCount * 0.022D);
        double patrolRadius = 12.0D + Math.sin(tickCount * 0.01D) * 5.0D;
        double targetX = arena.getX() + 0.5D + Math.cos(patrolAngle) * patrolRadius;
        double targetZ = arena.getZ() + 0.5D + Math.sin(patrolAngle) * patrolRadius;
        double targetY = arena.getY() + 5.5D + Math.sin(tickCount * 0.04D) * 1.2D;

        Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
        Vec3 toTarget = targetPos.subtract(position());
        double dist = toTarget.length();

        Vec3 desiredVel;
        if (dist > 0.3D) {
            desiredVel = toTarget.normalize().scale(Math.min(0.26D, dist * 0.1D));
        } else {
            desiredVel = new Vec3(-Math.sin(patrolAngle) * 0.22D, Math.cos(tickCount * 0.04D) * 0.04D, Math.cos(patrolAngle) * 0.22D);
        }

        Vec3 moveVec = getDeltaMovement().scale(0.75D).add(desiredVel.scale(0.25D));
        setDeltaMovement(moveVec);
        hasImpulse = true;

        if (--lookAroundTimer <= 0) {
            lookAroundTimer = 35 + random.nextInt(35);
            idleYawOffset = (random.nextFloat() - 0.5F) * 45.0F;
        }

        if (moveVec.horizontalDistanceSqr() > 0.003D) {
            float travelYaw = (float) (Mth.atan2(moveVec.z, moveVec.x) * (180.0D / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.18F, getYRot(), travelYaw));
            yBodyRot = Mth.rotLerp(0.16F, yBodyRot, getYRot());
            yHeadRot = Mth.rotLerp(0.20F, yHeadRot, travelYaw + idleYawOffset);
        }

        if (tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.2D, getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.1D, getZ(), 1, 0.15D, 0.15D, 0.15D, 0.01D);
        }
    }

    /** Rapid Fireball Barrage: Fires a stream of rapid SmallFireballs directly at target player. */
    private void rapidFireballBarrage(ServerLevel level, LivingEntity target) {
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5F, 1.0F);
        Vec3 origin = new Vec3(getX(), getY() + 1.2D, getZ());
        Vec3 dir = target.getEyePosition().subtract(origin).normalize();
        int count = 3 + phase;
        for (int i = 0; i < count; i++) {
            double spreadX = dir.x + (random.nextDouble() - 0.5D) * 0.12D;
            double spreadY = dir.y + (random.nextDouble() - 0.5D) * 0.10D;
            double spreadZ = dir.z + (random.nextDouble() - 0.5D) * 0.12D;
            Vec3 spreadDir = new Vec3(spreadX, spreadY, spreadZ).normalize();
            spawnSmallFireball(level, origin, spreadDir, 1.15D);
        }
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.2D, getZ(), 40, 0.8D, 0.8D, 0.8D, 0.12D);
    }

    /** 360-Degree Fireball Nova: Fires 12 fireballs radiating in a full circle, angled toward target elevation. */
    private void fireballNova(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 2.8F, 0.9F);
        Vec3 origin = new Vec3(getX(), getY() + 1.0D, getZ());
        double pitchY = 0.0D;
        if (getTarget() != null) {
            Vec3 toTarget = getTarget().getEyePosition().subtract(origin);
            double horiz = Math.max(1.0D, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
            pitchY = Mth.clamp(toTarget.y / horiz, -0.45D, 0.25D);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0D / 12.0D;
            Vec3 dir = new Vec3(Math.cos(angle), pitchY, Math.sin(angle)).normalize();
            spawnSmallFireball(level, origin, dir, 0.95D);
        }
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0D, getZ(), 60, 2.0D, 1.0D, 2.0D, 0.10D);
    }

    /** Summon Mini Blazes: Spawns 2..3 aggressive Blaze minions around the boss, capped at MAX_ACTIVE_MINIONS. */
    private void summonMiniBlazes(ServerLevel level, LivingEntity target) {
        List<Blaze> activeMinions = level.getEntitiesOfClass(Blaze.class,
            getBoundingBox().inflate(ARENA_RADIUS + 8.0D),
            e -> e != this && e.isAlive() && e.getTags().contains(MINION_TAG));
        int availableSlots = MAX_ACTIVE_MINIONS - activeMinions.size();
        if (availableSlots <= 0) {
            rapidFireballBarrage(level, target);
            return;
        }

        level.playSound(null, blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 3.5F, 1.2F);
        int count = Math.min(availableSlots, 2 + (phase >= 3 ? 1 : 0));
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0D / count;
            double ox = Math.cos(angle) * 3.0D;
            double oz = Math.sin(angle) * 3.0D;
            Blaze minion = EntityType.BLAZE.create(level);
            if (minion != null) {
                minion.moveTo(getX() + ox, getY() + 0.5D, getZ() + oz, random.nextFloat() * 360.0F, 0.0F);
                minion.setCustomName(Component.literal("§cInfernal Minion"));
                minion.setCustomNameVisible(true);
                minion.addTag(MINION_TAG);
                minion.setTarget(target);
                level.addFreshEntity(minion);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0D, getZ(), 100, 3.0D, 2.0D, 3.0D, 0.15D);
    }

    /** Lava Geyser: Marks 1..3 ground locations with a 28-tick warning telegraph before erupting. */
    private void lavaGeyser(ServerLevel level, LivingEntity target) {
        level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.8F, 0.7F);
        double floorY = arena.equals(BlockPos.ZERO) ? target.getY() : Math.max(arena.getY(), target.getY());
        pendingGeysers.add(new PendingGeyser(new Vec3(target.getX(), floorY, target.getZ()), 28));

        int extraGeysers = phase - 1;
        for (int i = 0; i < extraGeysers; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = 3.5D + random.nextDouble() * 4.5D;
            Vec3 candidate = new Vec3(target.getX() + Math.cos(angle) * dist, floorY, target.getZ() + Math.sin(angle) * dist);
            if (insideArena(candidate, ARENA_RADIUS - 2.0D)) {
                pendingGeysers.add(new PendingGeyser(candidate, 28 + (i + 1) * 6));
            }
        }
    }

    private void tickPendingGeysers(ServerLevel level) {
        if (pendingGeysers.isEmpty()) return;
        Iterator<PendingGeyser> it = pendingGeysers.iterator();
        while (it.hasNext()) {
            PendingGeyser geyser = it.next();
            Vec3 pos = geyser.position;
            if (geyser.ticksRemaining % 3 == 0) {
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI * 2.0D / 16.0D;
                    level.sendParticles(ParticleTypes.FLAME,
                        pos.x + Math.cos(a) * 3.2D, pos.y + 0.15D, pos.z + Math.sin(a) * 3.2D,
                        1, 0.04D, 0.04D, 0.04D, 0.0D);
                }
                level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.2D, pos.z, 4, 1.2D, 0.1D, 1.2D, 0.02D);
            }
            if (--geyser.ticksRemaining > 0) continue;

            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 0.85F);
            level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.4D, pos.z, 90, 2.2D, 1.2D, 2.2D, 0.12D);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.2D, pos.z, 70, 2.0D, 2.2D, 2.0D, 0.08D);

            AABB area = new AABB(pos.x - 3.4D, pos.y - 1.5D, pos.z - 3.4D, pos.x + 3.4D, pos.y + 5.0D, pos.z + 3.4D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isValidCombatTarget)) {
                double dx = victim.getX() - pos.x;
                double dz = victim.getZ() - pos.z;
                if (dx * dx + dz * dz <= 3.4D * 3.4D) {
                    victim.hurt(level.damageSources().mobAttack(this), 18.0F + phase * 2.0F);
                    victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 100));
                    victim.push(0.0D, 0.55D, 0.0D);
                    victim.hasImpulse = true;
                }
            }
            it.remove();
        }
    }

    /** Heavy Fireball Volley: Fires a massive 3-way LargeFireball cluster! */
    private void heavyFireballVolley(ServerLevel level, LivingEntity target) {
        level.playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 3.0F, 0.75F);
        Vec3 origin = new Vec3(getX(), getY() + 1.5D, getZ());
        Vec3 dir = target.getEyePosition().subtract(origin).normalize();
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        if (side.lengthSqr() < 1.0E-4D) side = new Vec3(1.0D, 0.0D, 0.0D);
        else side = side.normalize();

        for (double offset : new double[]{-0.22D, 0.0D, 0.22D}) {
            Vec3 spreadDir = dir.add(side.scale(offset)).normalize();
            LargeFireball large = EntityType.FIREBALL.create(level);
            if (large != null) {
                large.setOwner(this);
                Vec3 spawn = origin.add(side.scale(offset * 3.5D));
                large.setPos(spawn.x, spawn.y, spawn.z);
                large.setDeltaMovement(spreadDir.scale(1.25D));
                level.addFreshEntity(large);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.5D, getZ(), 4, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    private void spawnSmallFireball(ServerLevel level, Vec3 pos, Vec3 dir, double speed) {
        SmallFireball fireball = EntityType.SMALL_FIREBALL.create(level);
        if (fireball != null) {
            fireball.setOwner(this);
            fireball.setPos(pos.x, pos.y, pos.z);
            fireball.setDeltaMovement(dir.scale(speed));
            level.addFreshEntity(fireball);
        }
    }

    private void whiteHeat(ServerLevel level) {
        if (phase < 3) return;
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.5F, 1.4F);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.5D, getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0D, getZ(), 160, 6.0D, 2.0D, 6.0D, 0.08D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(16.0D), this::isValidCombatTarget)) {
            if (victim.distanceToSqr(this) <= 16.0D * 16.0D) {
                victim.hurt(level.damageSources().mobAttack(this), 24.0F);
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 100));
            }
        }
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
    }

    private void enforceArena(ServerLevel level) {
        if (arena.equals(BlockPos.ZERO)) {
            BlockPos nearestArena = BossArenaRegistryData.get(level.getServer())
                .nearest(BossArenaRegistryData.Kind.INFERNAL, blockPosition());
            if (nearestArena != null && nearestArena.distSqr(blockPosition()) <= (ARENA_RADIUS + 16.0D) * (ARENA_RADIUS + 16.0D)) {
                arena = nearestArena.immutable();
            } else {
                return;
            }
        }
        if (!insideArena(position(), ARENA_RADIUS + 5.0D) || getY() < arena.getY() - 4.0D || getY() > arena.getY() + 26.0D) {
            teleportTo(arena.getX() + 0.5D, arena.getY() + 5.0D, arena.getZ() + 0.5D);
            setDeltaMovement(Vec3.ZERO);
            dashTicks = 0;
            getNavigation().stop();
            setTarget(null);
            retaliationTargetId = null;
        }
        if (getTarget() != null && !isValidCombatTarget(getTarget())) {
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

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        if (!(level() instanceof ServerLevel serverLevel)) return super.hurt(source, amount);
        boolean bypasses = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypasses && source.getEntity() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS + 4.0D)) {
            return false;
        }

        ensureHealthInitialized(serverLevel);
        hadCombatEngagement = true;
        inactiveTicks = 0;
        if (bypasses && amount >= encounterHealth) {
            encounterHealth = 0.0F;
            setHealth(0.0F);
            die(source);
            return true;
        }

        setHealth(PHYSICAL_HEALTH_SEGMENT);
        boolean accepted = super.hurt(source, Math.min(Math.max(0.0F, amount), PHYSICAL_HEALTH_SEGMENT - 1.0F));
        if (!accepted) return false;

        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le
            : (source.getDirectEntity() instanceof LivingEntity dle ? dle : null);
        if (attacker != null && attacker != this && !attacker.getTags().contains(MINION_TAG)) {
            retaliationTargetId = attacker.getUUID();
            setTarget(attacker);
            setLastHurtByMob(attacker);
        }

        if (source.getEntity() != null) {
            level().playSound(null, blockPosition(), SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, 2.5F, 0.7F);
        }

        float dealt = isDeadOrDying() ? amount : Math.max(0.0F, PHYSICAL_HEALTH_SEGMENT - getHealth());
        encounterHealth = Math.max(0.0F, encounterHealth - dealt);
        if (encounterHealth <= 0.0F) {
            if (!isDeadOrDying()) {
                setHealth(0.0F);
                die(source);
            }
        } else {
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("Arena", arena.asLong());
        tag.putBoolean("InitializedHealth", initializedHealth);
        tag.putBoolean("HadCombatEngagement", hadCombatEngagement);
        tag.putFloat("EncounterHealth", encounterHealth);
        tag.putFloat("EncounterMaxHealth", encounterMaxHealth);
        tag.putInt("LastPhase", lastPhase);
        if (retaliationTargetId != null) tag.putUUID("RetaliationTarget", retaliationTargetId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
        initializedHealth = tag.getBoolean("InitializedHealth");
        hadCombatEngagement = tag.getBoolean("HadCombatEngagement");
        if (tag.contains("EncounterHealth")) encounterHealth = tag.getFloat("EncounterHealth");
        if (tag.contains("EncounterMaxHealth")) encounterMaxHealth = tag.getFloat("EncounterMaxHealth");
        if (tag.contains("LastPhase")) lastPhase = tag.getInt("LastPhase");
        if (tag.hasUUID("RetaliationTarget")) retaliationTargetId = tag.getUUID("RetaliationTarget");
        if (initializedHealth && encounterMaxHealth > 0.0F && encounterHealth > 0.0F) {
            if (getAttribute(Attributes.MAX_HEALTH) != null) {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHYSICAL_HEALTH_SEGMENT);
            }
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        } else {
            initializedHealth = false;
        }
    }

    @Override
    public void die(DamageSource source) {
        pendingGeysers.clear();
        queuedFireballs = 0;
        dashTicks = 0;
        if (level() instanceof ServerLevel level) {
            for (Blaze minion : level.getEntitiesOfClass(Blaze.class, getBoundingBox().inflate(ARENA_RADIUS + 16.0D),
                e -> e != this && e.getTags().contains(MINION_TAG))) {
                minion.discard();
            }
            level.playSound(null, blockPosition(), SoundEvents.BLAZE_DEATH, SoundSource.HOSTILE, 4.0F, 0.45F);
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0D, getZ(), 220, 4.0D, 3.0D, 4.0D, 0.15D);
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1.0D, getZ(), new ItemStack(Items.NETHER_STAR)));
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1.0D, getZ(), new ItemStack(Items.BLAZE_ROD, 8 + random.nextInt(5))));
        }
        super.die(source);
    }

    private static final class PendingGeyser {
        private final Vec3 position;
        private int ticksRemaining;

        private PendingGeyser(Vec3 position, int ticksRemaining) {
            this.position = position;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
