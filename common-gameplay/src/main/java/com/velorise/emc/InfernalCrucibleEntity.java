package com.velorise.emc;

import net.minecraft.commands.arguments.EntityAnchorArgument;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
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

    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.velorise_emc.infernal_crucible"),
        BossEvent.BossBarColor.RED,
        BossEvent.BossBarOverlay.NOTCHED_10
    );
    private BlockPos arena = BlockPos.ZERO;
    private boolean initializedHealth;
    private float encounterHealth = BASE_ENCOUNTER_HEALTH;
    private float encounterMaxHealth = BASE_ENCOUNTER_HEALTH;
    private int skillClock;
    private int phase = 1;
    private int lastPhase = 1;
    private int lastSkill = -1;

    private int queuedFireballs;
    private int fireballLaunchTimer;

    private int dashTicks;
    private Vec3 dashVelocity = Vec3.ZERO;
    private UUID dashTargetId;
    private boolean dashDealtDamage;

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

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        ensureHealthInitialized();
        float healthRatio = encounterMaxHealth <= 0.0F ? 0.0F : Mth.clamp(encounterHealth / encounterMaxHealth, 0.0F, 1.0F);
        bossBar.setProgress(healthRatio);
        enforceArena();

        phase = healthRatio > 0.66F ? 1 : healthRatio > 0.33F ? 2 : 3;
        if (phase > lastPhase) {
            lastPhase = phase;
            serverLevel.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.5F, 0.65F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.7F);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 80, 1.5D, 1.5D, 1.5D, 0.05D);
        }

        tickQueuedFireballs(serverLevel);
        tickPendingGeysers(serverLevel);

        ServerPlayer target = findCombatTarget(serverLevel);
        setTarget(target);

        if (dashTicks > 0) {
            tickInfernalDash(serverLevel);
            return;
        }

        if (target != null) {
            getLookControl().setLookAt(target, 180.0F, 180.0F);
            lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            flyKeepDistance(serverLevel, target);
        } else {
            hoverIdleAtCenter(serverLevel);
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

    private void ensureHealthInitialized() {
        if (!initializedHealth) {
            encounterMaxHealth = BASE_ENCOUNTER_HEALTH;
            encounterHealth = encounterMaxHealth;
            if (getAttribute(Attributes.MAX_HEALTH) != null) {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHYSICAL_HEALTH_SEGMENT);
            }
            setHealth(PHYSICAL_HEALTH_SEGMENT);
            initializedHealth = true;
        }
    }

    private ServerPlayer findCombatTarget(ServerLevel level) {
        if (getTarget() instanceof ServerPlayer current && isValidArenaTarget(current)) {
            return current;
        }
        Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), 56.0D,
            entity -> entity instanceof ServerPlayer sp && isValidArenaTarget(sp));
        return nearest instanceof ServerPlayer sp ? sp : null;
    }

    private boolean isValidArenaTarget(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
            && insideArena(player.position(), ARENA_RADIUS);
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

        ServerPlayer target = findCombatTarget(level);
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
    private void infernalDash(ServerLevel level, ServerPlayer target) {
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
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0D, getZ(), 5, 0.35D, 0.35D, 0.35D, 0.05D);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0D, getZ(), 10, 0.45D, 0.45D, 0.45D, 0.06D);

        if (!dashDealtDamage) {
            AABB hitBox = getBoundingBox().inflate(2.2D, 1.8D, 2.2D);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, hitBox, this::isValidArenaTarget)) {
                dashDealtDamage = true;
                player.hurt(level.damageSources().mobAttack(this), 22.0F + phase * 3.0F);
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 100));
                Vec3 push = dashVelocity.normalize().scale(1.45D);
                player.push(push.x, 0.45D, push.z);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.2F);
            }
        }

        if (dashTicks <= 0 && dashTargetId != null) {
            Player p = level.getPlayerByUUID(dashTargetId);
            if (p instanceof ServerPlayer sp && isValidArenaTarget(sp)) {
                getLookControl().setLookAt(sp, 180.0F, 180.0F);
            }
            dashTargetId = null;
        }
    }

    /** Smooth levitating flight strafing keeping 12..16 blocks distance in mid-air while staying inside the arena. */
    private void flyKeepDistance(ServerLevel level, Player target) {
        Vec3 bossPos = position();
        Vec3 targetPos = target.position();
        Vec3 toTarget = targetPos.subtract(bossPos);
        Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
        double dist = horizontalToTarget.length();
        Vec3 radialDir = dist > 1.0E-4D ? horizontalToTarget.scale(1.0D / dist) : new Vec3(1.0D, 0.0D, 0.0D);

        // True tangent perpendicular to radial direction so the boss orbits around the player.
        double orbitSign = ((tickCount / 90) & 1) == 0 ? 1.0D : -1.0D;
        Vec3 tangentDir = new Vec3(-radialDir.z * orbitSign, 0.0D, radialDir.x * orbitSign);

        double targetY = arena.equals(BlockPos.ZERO) ? target.getY() + 4.5D : arena.getY() + 5.5D + Math.sin(tickCount * 0.06D) * 1.0D;
        double dy = Mth.clamp((targetY - getY()) * 0.14D, -0.35D, 0.35D);

        double radialSpeed = 0.0D;
        if (dist < 10.0D) {
            radialSpeed = -0.34D;
        } else if (dist > 17.5D) {
            radialSpeed = 0.36D;
        }
        double tangentSpeed = 0.28D + phase * 0.04D;

        Vec3 moveVec = radialDir.scale(radialSpeed).add(tangentDir.scale(tangentSpeed)).add(0.0D, dy, 0.0D);

        // Prevent the player from pushing the boss out of the arena boundary to force an arena reset.
        if (!arena.equals(BlockPos.ZERO)) {
            Vec3 center = new Vec3(arena.getX() + 0.5D, getY(), arena.getZ() + 0.5D);
            Vec3 nextHorizontal = new Vec3(bossPos.x + moveVec.x - center.x, 0.0D, bossPos.z + moveVec.z - center.z);
            double maxOrbitRadius = ARENA_RADIUS - 3.0D;
            if (nextHorizontal.lengthSqr() > maxOrbitRadius * maxOrbitRadius) {
                Vec3 inward = center.subtract(bossPos).normalize().scale(0.38D);
                moveVec = new Vec3(inward.x + tangentDir.x * tangentSpeed, dy, inward.z + tangentDir.z * tangentSpeed);
            }
        }

        setDeltaMovement(moveVec);
        hasImpulse = true;

        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.3D, getZ(), 4, 0.3D, 0.3D, 0.3D, 0.02D);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.2D, getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
        }
    }

    private void hoverIdleAtCenter(ServerLevel level) {
        if (arena.equals(BlockPos.ZERO)) {
            setDeltaMovement(getDeltaMovement().scale(0.6D));
            return;
        }
        Vec3 home = new Vec3(arena.getX() + 0.5D, arena.getY() + 5.5D + Math.sin(tickCount * 0.05D) * 0.6D, arena.getZ() + 0.5D);
        Vec3 delta = home.subtract(position());
        if (delta.lengthSqr() > 0.25D) {
            Vec3 step = delta.normalize().scale(Math.min(0.30D, delta.length() * 0.15D));
            setDeltaMovement(step);
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        hasImpulse = true;
    }

    /** Rapid Fireball Barrage: Fires a stream of rapid SmallFireballs directly at target player. */
    private void rapidFireballBarrage(ServerLevel level, ServerPlayer target) {
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
    private void summonMiniBlazes(ServerLevel level, ServerPlayer target) {
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
    private void lavaGeyser(ServerLevel level, ServerPlayer target) {
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
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, this::isValidArenaTarget)) {
                double dx = player.getX() - pos.x;
                double dz = player.getZ() - pos.z;
                if (dx * dx + dz * dz <= 3.4D * 3.4D) {
                    player.hurt(level.damageSources().mobAttack(this), 18.0F + phase * 2.0F);
                    player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 100));
                    player.push(0.0D, 0.55D, 0.0D);
                }
            }
            it.remove();
        }
    }

    /** Heavy Fireball Volley: Fires a massive 3-way LargeFireball cluster! */
    private void heavyFireballVolley(ServerLevel level, ServerPlayer target) {
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
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(blockPosition()).inflate(16.0D), this::isValidArenaTarget)) {
            if (player.distanceToSqr(this) <= 16.0D * 16.0D) {
                player.hurt(level.damageSources().mobAttack(this), 24.0F);
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 100));
            }
        }
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
    }

    private void enforceArena() {
        if (arena.equals(BlockPos.ZERO)) return;
        if (!insideArena(position(), ARENA_RADIUS + 5.0D) || getY() < arena.getY() - 4.0D || getY() > arena.getY() + 26.0D) {
            teleportTo(arena.getX() + 0.5D, arena.getY() + 5.0D, arena.getZ() + 0.5D);
            setDeltaMovement(Vec3.ZERO);
            dashTicks = 0;
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

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        if (!(level() instanceof ServerLevel)) return super.hurt(source, amount);
        boolean bypasses = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypasses && source.getEntity() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) {
            return false;
        }

        ensureHealthInitialized();
        if (bypasses && amount >= encounterHealth) {
            encounterHealth = 0.0F;
            setHealth(0.0F);
            die(source);
            return true;
        }

        setHealth(PHYSICAL_HEALTH_SEGMENT);
        boolean accepted = super.hurt(source, Math.min(Math.max(0.0F, amount), PHYSICAL_HEALTH_SEGMENT - 1.0F));
        if (!accepted) return false;

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
        tag.putFloat("EncounterHealth", encounterHealth);
        tag.putFloat("EncounterMaxHealth", encounterMaxHealth);
        tag.putInt("LastPhase", lastPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
        initializedHealth = tag.getBoolean("InitializedHealth");
        if (tag.contains("EncounterHealth")) encounterHealth = tag.getFloat("EncounterHealth");
        if (tag.contains("EncounterMaxHealth")) encounterMaxHealth = tag.getFloat("EncounterMaxHealth");
        if (tag.contains("LastPhase")) lastPhase = tag.getInt("LastPhase");
        if (initializedHealth && encounterMaxHealth > 0.0F && encounterHealth > 0.0F) {
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
