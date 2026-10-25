package com.velorise.emc;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Tier III promotion boss: a giant shulker fortress with gravity and projectile-control phases. */
public final class AethericBulwarkEntity extends Shulker {
    public static final String MINION_TAG = "AethericShulkerling";
    private static final double ARENA_RADIUS = 25.0D;
    private static final float BASE_ENCOUNTER_HEALTH = 2_200.0F;
    /** Vanilla clamps MAX_HEALTH to 1024, so damage is resolved against a 1000-HP physical segment. */
    private static final float PHYSICAL_HEALTH_SEGMENT = 1_000.0F;
    private static final int MAX_ACTIVE_MINIONS = 4;

    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.velorise_emc.aetheric_bulwark"),
        BossEvent.BossBarColor.PURPLE,
        BossEvent.BossBarOverlay.PROGRESS
    );
    private BlockPos arena = BlockPos.ZERO;
    private boolean initializedHealth;
    private float encounterHealth = BASE_ENCOUNTER_HEALTH;
    private float encounterMaxHealth = BASE_ENCOUNTER_HEALTH;
    private int skillClock;
    private int phase = 1;
    private int lastPhase = 1;
    private int lastSkill = -1;
    private int shellGuardCooldown;
    private UUID retaliationTargetId;
    private int wanderTeleportCooldown = 120;
    private final List<PendingStrike> pendingStrikes = new ArrayList<>();

    public AethericBulwarkEntity(EntityType<? extends Shulker> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("entity.velorise_emc.aetheric_bulwark"));
        setCustomNameVisible(true);
        setPersistenceRequired();
        xpReward = 180;
    }

    public void bindArena(BlockPos center) {
        arena = center.immutable();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Shulker.createAttributes()
            .add(Attributes.MAX_HEALTH, 2200.0D)
            .add(Attributes.ARMOR, 18.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }

    @Override
    protected boolean teleportSomewhere() {
        // Prevent Vanilla Shulker from randomly teleporting onto high outer obelisk roofs or outside the arena.
        if (level().isClientSide || arena.equals(BlockPos.ZERO)) return false;
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel level)) return;

        ensureHealthInitialized();
        float healthRatio = encounterMaxHealth <= 0.0F ? 0.0F : Mth.clamp(encounterHealth / encounterMaxHealth, 0.0F, 1.0F);
        bossBar.setProgress(healthRatio);
        enforceArena(level);

        if (shellGuardCooldown > 0) shellGuardCooldown--;
        tickPendingStrikes(level);

        phase = healthRatio > 0.70F ? 1 : healthRatio > 0.35F ? 2 : 3;
        if (phase > lastPhase) {
            lastPhase = phase;
            level.playSound(null, blockPosition(), SoundEvents.SHULKER_CLOSE, SoundSource.HOSTILE, 3.5F, 0.55F);
            level.playSound(null, blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.HOSTILE, 2.2F, 0.65F);
            level.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.75F);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0D, getZ(), 80, 1.5D, 1.5D, 1.5D, 0.08D);
        }

        LivingEntity target = findCombatTarget(level);
        if (target == null) {
            setTarget(null);
            wanderIdle(level);
            return;
        }
        setTarget(target);
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        if (++skillClock < (phase == 1 ? 76 : phase == 2 ? 56 : 40)) return;
        skillClock = 0;

        int poolSize = phase == 1 ? 4 : phase == 2 ? 6 : 7;
        int choice = random.nextInt(poolSize);
        if (choice == lastSkill) {
            choice = (choice + 1 + random.nextInt(poolSize - 1)) % poolSize;
        }
        if (choice == 5 && shellGuardCooldown > 0) {
            choice = 0;
        }
        lastSkill = choice;

        if (choice == 0) orbitalBarrage(level, target);
        else if (choice == 1) gravityInversion(level, target);
        else if (choice == 2) spatialSwap(level, target);
        else if (choice == 3) fakeShadowDecoy(level, target);
        else if (choice == 4) gravitySingularity(level, target);
        else if (choice == 5) ironShellGuard(level, target);
        else chargedBurst(level, target);
    }

    private void wanderIdle(ServerLevel level) {
        if (arena.equals(BlockPos.ZERO)) return;

        if (tickCount % 60 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.SHULKER_AMBIENT, SoundSource.HOSTILE, 1.2F, 0.8F);
        }

        if (--wanderTeleportCooldown <= 0) {
            wanderTeleportCooldown = 130 + random.nextInt(70);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = 5.0D + random.nextDouble() * (ARENA_RADIUS - 9.0D);
            Vec3 candidate = new Vec3(arena.getX() + 0.5D + Math.cos(angle) * dist, arena.getY() + 1.0D, arena.getZ() + 0.5D + Math.sin(angle) * dist);
            Vec3 floorPos = snapToArenaFloor(level, candidate);
            if (floorPos != null && insideArena(floorPos, ARENA_RADIUS - 2.0D)) {
                level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5D, getZ(), 30, 0.5D, 0.5D, 0.5D, 0.1D);
                teleportTo(floorPos.x, floorPos.y, floorPos.z);
                level.playSound(null, blockPosition(), SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.9F);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5D, getZ(), 40, 0.6D, 0.6D, 0.6D, 0.05D);
            }
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

        Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), 64.0D,
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
            && insideArena(player.position(), ARENA_RADIUS);
    }

    private boolean isValidCombatTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == this) return false;
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && insideArena(player.position(), ARENA_RADIUS);
        }
        if (entity.getTags().contains(MINION_TAG)) return false;
        return insideArena(entity.position(), ARENA_RADIUS + 4.0D);
    }

    private void orbitalBarrage(ServerLevel level, LivingEntity target) {
        level.playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2.2F, 0.55F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0D / 12.0D;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * 4.0D, getY() + 1.5D, getZ() + Math.sin(a) * 4.0D, 8, 0.15D, 0.15D, 0.15D, 0.03D);
        }
        delayedLevitationHit(level, target, 18.0F, 30, 45, false);
    }

    /** High Gravity Inversion: queues a brief high-gravity warning then launches the target skyward. */
    private void gravityInversion(ServerLevel level, LivingEntity target) {
        level.playSound(null, target.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.0F, 1.6F);
        target.removeEffect(MobEffects.SLOW_FALLING);
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 55, 5));
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY(), target.getZ(), 120, 2.0D, 3.0D, 2.0D, 0.25D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1.0D, target.getZ(), 80, 1.5D, 2.0D, 1.5D, 0.15D);
    }

    /** Spatial Swap: Swaps boss position with a ground-snapped arena position under the target. */
    private void spatialSwap(ServerLevel level, LivingEntity target) {
        Vec3 oldBoss = position();
        Vec3 safeBossDest = snapToArenaFloor(level, target.position());
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.7F);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.9F);
        teleportTo(safeBossDest.x, safeBossDest.y, safeBossDest.z);
        target.teleportTo(oldBoss.x, Math.max(oldBoss.y, arena.equals(BlockPos.ZERO) ? oldBoss.y : arena.getY() + 1.0D), oldBoss.z);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, oldBoss.x, oldBoss.y + 1.0D, oldBoss.z, 80, 1.0D, 1.0D, 1.0D, 0.12D);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, safeBossDest.x, safeBossDest.y + 1.0D, safeBossDest.z, 80, 1.0D, 1.0D, 1.0D, 0.12D);
    }

    private Vec3 snapToArenaFloor(ServerLevel level, Vec3 candidate) {
        double x = candidate.x;
        double z = candidate.z;
        if (!arena.equals(BlockPos.ZERO)) {
            double cx = arena.getX() + 0.5D;
            double cz = arena.getZ() + 0.5D;
            double dx = x - cx;
            double dz = z - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double maxR = ARENA_RADIUS - 3.0D;
            if (dist > maxR && dist > 1.0E-4D) {
                x = cx + dx * (maxR / dist);
                z = cz + dz * (maxR / dist);
            }
        }
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int startY = Mth.floor(candidate.y);
        int minY = arena.equals(BlockPos.ZERO) ? level.getMinBuildHeight() + 1 : arena.getY() - 1;
        BlockPos pos = new BlockPos(bx, startY, bz);
        while (pos.getY() > minY && level.getBlockState(pos.below()).isAir()) {
            pos = pos.below();
        }
        while (pos.getY() < startY + 6 && !level.getBlockState(pos).isAir()) {
            pos = pos.above();
        }
        return new Vec3(bx + 0.5D, pos.getY(), bz + 0.5D);
    }

    /** Shulkerling Ambush: spawns a normal Shulker minion behind the target, capped at MAX_ACTIVE_MINIONS. */
    private void fakeShadowDecoy(ServerLevel level, LivingEntity target) {
        List<Shulker> activeMinions = level.getEntitiesOfClass(Shulker.class,
            getBoundingBox().inflate(ARENA_RADIUS + 8.0D),
            e -> e != this && e.isAlive() && e.getTags().contains(MINION_TAG));
        if (activeMinions.size() >= MAX_ACTIVE_MINIONS) {
            orbitalBarrage(level, target);
            return;
        }

        level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 2.5F, 0.75F);
        Vec3 look = target.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        else horizontalLook = horizontalLook.normalize();
        Vec3 rawSpawn = target.position().subtract(horizontalLook.scale(2.5D));
        Vec3 spawnPos = snapToArenaFloor(level, rawSpawn);

        Shulker minion = EntityType.SHULKER.create(level);
        if (minion == null) return;

        minion.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, target.getYRot(), 0.0F);
        minion.setCustomName(Component.literal("§dAetheric Shulkerling"));
        minion.setCustomNameVisible(true);
        minion.setHealth(Math.min(20.0F, minion.getMaxHealth()));
        minion.setTarget(target);
        minion.addTag(MINION_TAG);
        level.addFreshEntity(minion);

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z,
            100, 1.0D, 1.5D, 1.0D, 0.08D);
        level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z,
            30, 0.6D, 0.8D, 0.6D, 0.03D);
    }

    /** Gravity Singularity: Pulls combat targets towards a crushing purple void well at target's feet! */
    private void gravitySingularity(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position();
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5F, 1.5F);
        AABB box = target.getBoundingBox().inflate(14.0D, 6.0D, 14.0D);
        for (LivingEntity p : level.getEntitiesOfClass(LivingEntity.class, box, this::isValidCombatTarget)) {
            Vec3 diff = center.subtract(p.position());
            Vec3 dir = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(0.55D) : Vec3.ZERO;
            p.setDeltaMovement(p.getDeltaMovement().add(dir.x, 0.25D, dir.z));
            p.hurt(level.damageSources().mobAttack(this), 16.0F);
            p.hasImpulse = true;
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 1.0D, center.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1.0D, center.z, 160, 4.0D, 2.0D, 4.0D, 0.20D);
    }

    /** Iron Shell Guard: Shulker closes shell, gains Resistance III (60% DR), and fires 8 radial homing bullets! */
    private void ironShellGuard(ServerLevel level, LivingEntity target) {
        shellGuardCooldown = 160;
        level.playSound(null, blockPosition(), SoundEvents.SHULKER_CLOSE, SoundSource.HOSTILE, 3.0F, 0.6F);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 2));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2.0D / 8.0D;
            ShulkerBullet bullet = new ShulkerBullet(level, this, target, Direction.UP.getAxis());
            bullet.moveTo(getX() + Math.cos(angle) * 1.5D, getY() + 1.2D, getZ() + Math.sin(angle) * 1.5D,
                (float) Math.toDegrees(angle), 0.0F);
            level.addFreshEntity(bullet);
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0D, getZ(), 100, 2.0D, 2.0D, 2.0D, 0.15D);
    }

    private void chargedBurst(ServerLevel level, LivingEntity target) {
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5F, 1.45F);
        delayedLevitationHit(level, target, 32.0F, 22, 65, true);
    }

    private void delayedLevitationHit(ServerLevel level, LivingEntity target, float damage,
                                      int visualTicks, int duration, boolean trackTarget) {
        pendingStrikes.add(new PendingStrike(
            target.getUUID(),
            target.position(),
            damage,
            Math.max(1, visualTicks),
            Math.max(1, visualTicks),
            duration,
            trackTarget
        ));
    }

    private void tickPendingStrikes(ServerLevel level) {
        if (pendingStrikes.isEmpty()) return;
        Iterator<PendingStrike> it = pendingStrikes.iterator();
        while (it.hasNext()) {
            PendingStrike strike = it.next();
            Entity ent = level.getEntity(strike.targetId);
            LivingEntity tracked = ent instanceof LivingEntity le && isValidCombatTarget(le) ? le : null;
            if (strike.trackTarget && tracked != null && strike.ticksRemaining > 6) {
                strike.impactPos = tracked.position();
            }

            Vec3 center = strike.impactPos;
            double progress = 1.0D - (strike.ticksRemaining / (double) strike.totalTicks);
            double ringRadius = Mth.lerp(progress, 3.6D, 0.5D);
            if (strike.ticksRemaining % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2.0D / 12.0D + progress * Math.PI;
                    level.sendParticles(ParticleTypes.END_ROD,
                        center.x + Math.cos(a) * ringRadius,
                        center.y + 0.25D,
                        center.z + Math.sin(a) * ringRadius,
                        1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }

            if (--strike.ticksRemaining > 0) continue;

            level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 1.0D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1.0D, center.z, 45, 1.6D, 1.0D, 1.6D, 0.08D);
            level.playSound(null, BlockPos.containing(center.x, center.y, center.z),
                SoundEvents.SHULKER_BULLET_HIT, SoundSource.HOSTILE, 2.5F, 0.75F);

            AABB hitBox = new AABB(center.x - 3.2D, center.y - 2.0D, center.z - 3.2D,
                center.x + 3.2D, center.y + 4.0D, center.z + 3.2D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox, this::isValidCombatTarget)) {
                if (victim.position().distanceToSqr(center) <= 3.2D * 3.2D) {
                    victim.hurt(level.damageSources().mobAttack(this), strike.damage);
                    victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, strike.levitationDuration, 0));
                }
            }
            it.remove();
        }
    }

    private void enforceArena(Level level) {
        if (arena.equals(BlockPos.ZERO)) return;
        if (!insideArena(position(), ARENA_RADIUS + 3.0D) || getY() < arena.getY() - 2.0D) {
            // Altar is at arena.above(2); place the boss cleanly on top at arena.getY() + 3.0D.
            teleportTo(arena.getX() + 0.5D, arena.getY() + 3.0D, arena.getZ() + 0.5D);
            setTarget(null);
            retaliationTargetId = null;
            return;
        }
        if (getTarget() != null && !isValidCombatTarget(getTarget())) {
            setTarget(null);
        }
    }

    private boolean insideArena(Vec3 position, double radius) {
        if (arena.equals(BlockPos.ZERO)) return true;
        double dx = position.x - (arena.getX() + 0.5D);
        double dz = position.z - (arena.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHULKER_HURT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
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

        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le
            : (source.getDirectEntity() instanceof LivingEntity dle ? dle : null);
        if (attacker != null && attacker != this && !attacker.getTags().contains(MINION_TAG)) {
            retaliationTargetId = attacker.getUUID();
            setTarget(attacker);
            setLastHurtByMob(attacker);
        }

        if (source.getEntity() != null) {
            level().playSound(null, blockPosition(), SoundEvents.SHULKER_HURT, SoundSource.HOSTILE, 2.5F, 0.75F);
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
        if (retaliationTargetId != null) tag.putUUID("RetaliationTarget", retaliationTargetId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
        initializedHealth = tag.getBoolean("InitializedHealth");
        if (tag.contains("EncounterHealth")) encounterHealth = tag.getFloat("EncounterHealth");
        if (tag.contains("EncounterMaxHealth")) encounterMaxHealth = tag.getFloat("EncounterMaxHealth");
        if (tag.contains("LastPhase")) lastPhase = tag.getInt("LastPhase");
        if (tag.hasUUID("RetaliationTarget")) retaliationTargetId = tag.getUUID("RetaliationTarget");
        if (initializedHealth && encounterMaxHealth > 0.0F && encounterHealth > 0.0F) {
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        } else {
            initializedHealth = false;
        }
    }

    @Override
    public void die(DamageSource source) {
        pendingStrikes.clear();
        if (level() instanceof ServerLevel level) {
            for (Shulker minion : level.getEntitiesOfClass(Shulker.class, getBoundingBox().inflate(ARENA_RADIUS + 16.0D),
                e -> e != this && e.getTags().contains(MINION_TAG))) {
                minion.discard();
            }
            level.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3.0F, 0.45F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.0D, getZ(), 3, 1.0D, 1.0D, 1.0D, 0.0D);
        }
        super.die(source);
    }

    private static final class PendingStrike {
        private final UUID targetId;
        private Vec3 impactPos;
        private final float damage;
        private int ticksRemaining;
        private final int totalTicks;
        private final int levitationDuration;
        private final boolean trackTarget;

        private PendingStrike(UUID targetId, Vec3 impactPos, float damage,
                              int ticksRemaining, int totalTicks, int levitationDuration, boolean trackTarget) {
            this.targetId = targetId;
            this.impactPos = impactPos;
            this.damage = damage;
            this.ticksRemaining = ticksRemaining;
            this.totalTicks = totalTicks;
            this.levitationDuration = levitationDuration;
            this.trackTarget = trackTarget;
        }
    }
}
