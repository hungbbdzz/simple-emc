package com.velorise.emc;

import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Compatibility-safe flying boss: independent from vanilla EnderDragonFight and the central island. */
public final class EchoboundDragonEntity extends Phantom {
    public static final String CRYSTAL_TAG = "velorise_emc.echo_crystal";
    private static final byte EVENT_SLAM_START = 60;
    private static final byte EVENT_SLAM_IMPACT = 61;
    private static final String SOUL_TAG = "velorise_emc.echo_soul";
    private static final int SKILL_BEAM = 0;
    private static final int SKILL_VOLLEY = 1;
    private static final int SKILL_DIVE = 2;
    private static final int SKILL_SLAM = 3;
    private static final int SKILL_RAIN = 4;
    private static final int SKILL_CYCLONE = 5;
    private static final int SKILL_CHAINS = 6;
    private static final int SKILL_SWEEP = 7;
    private static final int SKILL_HARVEST = 8;
    private static final int SKILL_COLLAPSE = 9;
    /** Vanilla clamps MAX_HEALTH to 1024, so this is only the damage-processing segment. */
    private static final float PHYSICAL_HEALTH_SEGMENT = 1_000.0F;
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.velorise_emc.echobound_dragon"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos arena = BlockPos.ZERO;
    private int spawnedPhaseMask;
    private boolean scaled;
    private float encounterHealth;
    private float encounterMaxHealth;
    private int queuedProjectileKind;
    private int queuedShots;
    private int nextQueuedShotTick;
    private UUID queuedTarget;
    private int breathDiveTicks;
    private UUID breathTarget;
    private int slamTicks;
    private double slamX;
    private double slamY;
    private double slamZ;
    private int enderRainMarksRemaining;
    private int nextEnderRainMarkTick;
    private int enderRainEndTick;
    private UUID enderRainTarget;
    private boolean enderRainFinalQueued;
    private final List<EnderRainStrike> pendingEnderRain = new ArrayList<>();
    private final List<MeteorImpact> pendingMeteorImpacts = new ArrayList<>();
    private int clientSlamTicks;
    private final int[] skillCooldowns = new int[10];
    private int lastSkill = -1;
    private int beamChargeTicks;
    private UUID beamTarget;
    private Vec3 beamAim = Vec3.ZERO;
    private int wingCycloneTicks;
    private int resonanceSweepTicks;
    private float resonanceSweepAngle;
    private int voidCollapseTicks;
    private int soulHarvestTicks;
    private int chainTicks;
    private final List<UUID> chainedPlayers = new ArrayList<>();
    private Vec3 chainAnchor = Vec3.ZERO;
    private int comboId;
    private int comboStep;
    private int comboNextTick;
    private int previousCrystalCount;
    private int crystalCountPhase = -1;
    private int lastObservedPhase = -1;
    private final List<Vec3> phaseCrystalSites = new ArrayList<>();
    private final List<DelayedBlast> pendingCrystalBlasts = new ArrayList<>();
    private final Set<UUID> sweepHitThisPulse = new HashSet<>();
    private boolean finalRefrainUsed;
    private Vec3 lastMovementCheck = Vec3.ZERO;
    private int stuckTicks;
    private int emergencyTeleportCooldown;
    private int combatTeleportCooldown;
    private static final double ARENA_RADIUS = 58.0D;

    public EchoboundDragonEntity(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.translatable("entity.velorise_emc.echobound_dragon")
            .withStyle(ChatFormatting.DARK_PURPLE));
        xpReward = 5_000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, PHYSICAL_HEALTH_SEGMENT)
            .add(Attributes.ATTACK_DAMAGE, 28.0D)
            .add(Attributes.FOLLOW_RANGE, 192.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.35D)
            .add(Attributes.ARMOR, 20.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty: Vanilla Phantom goals steer toward anchorPoint (0, 0, 0) via
        // PhantomMoveControl.moveTargetPoint, which pulls the dragon out of outer-End citadels.
    }

    @Override
    protected void customServerAiStep() {
        // Bypass Vanilla Phantom MoveControl so it cannot overwrite deltaMovement or spin 180 degrees on collision.
    }

    @Override protected boolean isSunBurnTick() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) { return false; }

    public void bindArena(BlockPos center) { arena = center.immutable(); }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (isDeadOrDying()) {
            setNoAi(true);
            if (!level().isClientSide) {
                bossBar.setProgress(0.0F);
                bossBar.setVisible(false);
            }
            return;
        }
        if (level().isClientSide) {
            if (clientSlamTicks > 0) clientSlamTicks--;
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (!scaled) scaleForPlayers(serverLevel);
        int phase = LastEchoProfile.phase(encounterHealth, encounterMaxHealth);
        if (lastObservedPhase < 0) {
            lastObservedPhase = phase;
        } else if (phase > lastObservedPhase) {
            playPhaseStrengthSound(serverLevel, phase);
            lastObservedPhase = phase;
        } else if (phase < lastObservedPhase) {
            lastObservedPhase = phase;
        }
        setPhantomSize(phase);
        bossBar.setProgress(encounterMaxHealth <= 0.0F ? 1.0F : Mth.clamp(encounterHealth / encounterMaxHealth, 0.0F, 1.0F));
        bossBar.setName(Component.translatable("entity.velorise_emc.echobound_dragon")
            .withStyle(ChatFormatting.DARK_PURPLE)
            .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(Mth.ceil(encounterHealth) + " / "
                + Mth.ceil(encounterMaxHealth) + " HP").withStyle(ChatFormatting.AQUA)));
        ensurePhaseCrystals(serverLevel, phase);
        List<EndCrystal> crystals = activeCrystals(serverLevel);
        if (crystalCountPhase != phase) {
            crystalCountPhase = phase;
            previousCrystalCount = crystals.size();
        } else if (crystals.size() < previousCrystalCount) {
            int destroyed = previousCrystalCount - crystals.size();
            float crystalDamage = encounterMaxHealth * 0.02F * destroyed;
            encounterHealth = Math.max(1.0F, encounterHealth - crystalDamage);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_HURT,
                SoundSource.HOSTILE, 4.5F, 0.85F);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 1.5D, getZ(),
                36 * destroyed, 2.4D, 1.4D, 2.4D, 0.08D);
        }
        bossBar.setColor(crystals.isEmpty() ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
        if (!crystals.isEmpty() && tickCount % 20 == 0) {
            encounterHealth = Math.min(encounterMaxHealth,
                encounterHealth + encounterMaxHealth * LastEchoProfile.CRYSTAL_REGEN_PERCENT[phase]);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0D, getZ(), 18, 2.0D, 1.0D, 2.0D, 0.05D);
        }
        if (previousCrystalCount > 0 && crystals.isEmpty()) {
            playCrystalWeakeningSound(serverLevel);
            if (phase > 0 && phase < 3) scheduleCrystalDetonation();
        }
        previousCrystalCount = crystals.size();
        for (int i = 0; i < skillCooldowns.length; i++) if (skillCooldowns[i] > 0) skillCooldowns[i]--;

        ServerPlayer target = nearestTarget(serverLevel);
        enforceArenaMovement(serverLevel, phase, target);
        tickStuckResolution(serverLevel, target);
        if (emergencyTeleportCooldown > 0) emergencyTeleportCooldown--;
        if (combatTeleportCooldown > 0) combatTeleportCooldown--;

        tickCrystalDetonations(serverLevel, phase);
        tickQueuedVolley(serverLevel, phase);
        tickBreathDive(serverLevel, phase);
        tickSlam(serverLevel, phase);
        tickEnderRain(serverLevel, phase);
        tickSonicBeam(serverLevel, phase);
        tickWingCyclone(serverLevel, phase);
        tickEchoChains(serverLevel, phase);
        tickResonanceSweep(serverLevel, phase);
        tickVoidCollapse(serverLevel, phase);
        tickSoulHarvest(serverLevel, phase);
        tickCombo(serverLevel, phase);
        updateFlightOrientation(target);

        if (phase == 3 && !finalRefrainUsed && encounterHealth <= encounterMaxHealth * 0.10F && !isBusy()) {
            finalRefrainUsed = true;
            startCombo(4);
        }
        int interval = LastEchoProfile.ATTACK_INTERVAL[phase];
        if (!isBusy() && comboId == 0 && tickCount % interval == 0) attack(serverLevel, phase);
    }

    private void playCrystalWeakeningSound(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.WITHER_HURT,
            SoundSource.HOSTILE, 5.5F, 0.55F);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE,
            SoundSource.HOSTILE, 4.0F, 0.62F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5D, getZ(),
            70, 2.8D, 1.8D, 2.8D, 0.04D);
    }

    private void playPhaseStrengthSound(ServerLevel level, int phase) {
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
            SoundSource.HOSTILE, 7.0F, 0.62F + phase * 0.08F);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE,
            SoundSource.HOSTILE, 5.0F, 0.72F + phase * 0.08F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5D, getZ(),
            140, 4.0D, 2.5D, 4.0D, 0.18D);
    }

    private void scaleForPlayers(ServerLevel level) {
        int count = Math.max(1, level.getEntitiesOfClass(ServerPlayer.class,
            new AABB(arena).inflate(160.0D), this::isValidTarget).size());
        encounterMaxHealth = LastEchoProfile.scaledHealth(count);
        encounterHealth = encounterMaxHealth;
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHYSICAL_HEALTH_SEGMENT);
        }
        setHealth(PHYSICAL_HEALTH_SEGMENT);
        scaled = true;
    }

    private List<EndCrystal> activeCrystals(ServerLevel level) {
        return level.getEntitiesOfClass(EndCrystal.class, new AABB(arena).inflate(72.0D, 48.0D, 72.0D),
            c -> c.isAlive() && c.getTags().contains(CRYSTAL_TAG));
    }

    private void ensurePhaseCrystals(ServerLevel level, int phase) {
        int bit = 1 << phase;
        int wanted = LastEchoProfile.CRYSTALS_BY_PHASE[phase];
        if ((spawnedPhaseMask & bit) != 0 || wanted == 0) return;
        spawnedPhaseMask |= bit;
        phaseCrystalSites.clear();
        for (EndCrystal old : activeCrystals(level)) old.discard();
        List<Vec3> sites = crystalTowerSites(wanted);
        for (Vec3 site : sites) {
            EndCrystal crystal = EntityType.END_CRYSTAL.create(level);
            if (crystal == null) continue;
            crystal.addTag(CRYSTAL_TAG);
            crystal.setCustomName(Component.translatable("entity.velorise_emc.echo_crystal"));
            crystal.setCustomNameVisible(true);
            crystal.setShowBottom(false);
            crystal.moveTo(site.x, site.y, site.z, 0.0F, 0.0F);
            phaseCrystalSites.add(site);
            level.addFreshEntity(crystal);
        }
        level.playSound(null, arena, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0F, 0.65F + phase * 0.1F);
    }

    private List<Vec3> crystalTowerSites(int wanted) {
        List<Vec3> sites = new ArrayList<>(8);
        // Four monumental cardinal towers (R = 35). Bedrock cap is at arena Y + 30, crystal at Y + 31.
        int[][] major = {{35, 0}, {-35, 0}, {0, 35}, {0, -35}};
        for (int[] offset : major) {
            sites.add(new Vec3(arena.getX() + offset[0] + 0.5D, arena.getY() + 31.0D,
                arena.getZ() + offset[1] + 0.5D));
        }
        // Four medium diagonal side towers (R = 25,25). Bedrock cap is at arena Y + 17, crystal at Y + 18.
        int[][] diagonal = {{25, 25}, {25, -25}, {-25, 25}, {-25, -25}};
        for (int[] offset : diagonal) {
            sites.add(new Vec3(arena.getX() + offset[0] + 0.5D, arena.getY() + 18.0D,
                arena.getZ() + offset[1] + 0.5D));
        }
        return new ArrayList<>(sites.subList(0, Math.min(wanted, sites.size())));
    }

    private void attack(ServerLevel level, int phase) {
        ServerPlayer target = nearestTarget(level);
        if (target == null) return;
        if (combatTeleportCooldown <= 0 && phase >= 1 && random.nextFloat() < (phase == 3 ? 0.24F : 0.14F)) {
            teleportAroundTarget(level, target, phase);
            combatTeleportCooldown = phase == 3 ? 90 : 130;
            if (random.nextBoolean()) return;
        }
        if ((target.distanceToSqr(this) > 72.0D * 72.0D || !hasLineOfSight(target)) && random.nextFloat() < 0.55F) {
            devourEcho(level, target);
            return;
        }
        if (phase >= 1 && random.nextFloat() < 0.22F) {
            startCombo(phase == 1 ? 1 : phase == 2 ? 2 : 3);
            return;
        }
        List<Integer> pool = new ArrayList<>();
        addSkill(pool, SKILL_BEAM);
        addSkill(pool, SKILL_VOLLEY);
        addSkill(pool, SKILL_CYCLONE);
        if (phase >= 1) {
            addSkill(pool, SKILL_DIVE);
            addSkill(pool, SKILL_RAIN);
            addSkill(pool, SKILL_CHAINS);
            addSkill(pool, SKILL_SWEEP);
        }
        if (phase >= 2) addSkill(pool, SKILL_SLAM);
        if (phase == 3) {
            addSkill(pool, SKILL_HARVEST);
            addSkill(pool, SKILL_COLLAPSE);
        }
        if (pool.isEmpty()) return;
        int skill = pool.get(random.nextInt(pool.size()));
        executeSkill(level, phase, target, skill);
    }

    private void addSkill(List<Integer> pool, int skill) {
        if (skill != lastSkill && skillCooldowns[skill] <= 0) pool.add(skill);
    }

    private boolean isValidTarget(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private boolean isValidArenaTarget(ServerPlayer player) {
        return isValidTarget(player) && insideArena(player.position(), ARENA_RADIUS);
    }

    private ServerPlayer nearestTarget(ServerLevel level) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(arena).inflate(ARENA_RADIUS, 80.0D, ARENA_RADIUS),
                this::isValidArenaTarget).stream()
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    /**
     * Smooth 3D orbital dragon flight controller that replaces Vanilla PhantomMoveControl.
     */
    private void enforceArenaMovement(ServerLevel level, int phase, ServerPlayer target) {
        if (arena.equals(BlockPos.ZERO)) return;
        double cx = arena.getX() + 0.5D;
        double cz = arena.getZ() + 0.5D;
        double dxCenter = getX() - cx;
        double dzCenter = getZ() - cz;
        double distCenter = Math.sqrt(dxCenter * dxCenter + dzCenter * dzCenter);

        if (distCenter > ARENA_RADIUS + 24.0D || getY() < arena.getY() - 6.0D || getY() > arena.getY() + 64.0D) {
            teleportTo(cx, arena.getY() + 26.0D, cz);
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (target == null) {
            setTarget(null);
            cancelPlayerLockedActions();
        } else {
            setTarget(target);
        }

        // Skills that directly control velocity handle their own motion.
        if (breathDiveTicks > 0 || slamTicks > 0 || wingCycloneTicks > 0) return;

        double orbitRate = 0.024D + phase * 0.004D;
        double angle = tickCount * orbitRate;
        double desiredX;
        double desiredY;
        double desiredZ;
        double maxSpeed;

        if (target != null) {
            double orbitRadius = 18.0D + Math.sin(tickCount * 0.015D) * 4.0D;
            desiredX = target.getX() + Math.cos(angle) * orbitRadius;
            desiredZ = target.getZ() + Math.sin(angle) * orbitRadius;
            double offsetX = desiredX - cx;
            double offsetZ = desiredZ - cz;
            double offsetDist = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
            double maxArenaOrbit = ARENA_RADIUS - 8.0D;
            if (offsetDist > maxArenaOrbit && offsetDist > 1.0E-4D) {
                desiredX = cx + offsetX * (maxArenaOrbit / offsetDist);
                desiredZ = cz + offsetZ * (maxArenaOrbit / offsetDist);
            }
            desiredY = Mth.clamp(target.getY() + 13.0D + Math.sin(angle * 2.0D) * 3.5D,
                arena.getY() + 10.0D, arena.getY() + 36.0D);
            maxSpeed = (beamChargeTicks > 0 || resonanceSweepTicks > 0) ? 0.22D : (0.58D + phase * 0.07D);
        } else {
            double orbit = distCenter > ARENA_RADIUS + 4.0D ? 10.0D : 26.0D;
            desiredX = cx + Math.cos(angle) * orbit;
            desiredZ = cz + Math.sin(angle) * orbit;
            desiredY = arena.getY() + 24.0D + Math.sin(angle * 2.0D) * 4.0D;
            maxSpeed = distCenter > ARENA_RADIUS + 4.0D ? 0.85D : 0.48D;
        }

        Vec3 toDesired = new Vec3(desiredX - getX(), desiredY - getY(), desiredZ - getZ());
        double dist = toDesired.length();
        Vec3 desiredVel = dist > 1.0E-4D
            ? toDesired.scale(Math.min(maxSpeed, dist * 0.12D) / dist)
            : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.76D).add(desiredVel.scale(0.24D)));
        hasImpulse = true;
    }

    private void updateFlightOrientation(ServerPlayer target) {
        if (slamTicks > 0) return;
        Vec3 lookVec = null;
        if (beamChargeTicks > 0) {
            lookVec = beamAim.subtract(getEyePosition());
        } else if (resonanceSweepTicks > 0) {
            lookVec = new Vec3(Math.cos(resonanceSweepAngle), -0.15D, Math.sin(resonanceSweepAngle));
        } else {
            Vec3 vel = getDeltaMovement();
            if (vel.horizontalDistanceSqr() > 0.008D) {
                lookVec = vel;
            } else if (target != null) {
                lookVec = target.getEyePosition().subtract(getEyePosition());
            }
        }
        if (lookVec != null && lookVec.horizontalDistanceSqr() > 1.0E-4D) {
            float targetYaw = (float) (Mth.atan2(lookVec.z, lookVec.x) * (180.0D / Math.PI)) - 90.0F;
            float newYaw = Mth.rotLerp(0.22F, getYRot(), targetYaw);
            setYRot(newYaw);
            yBodyRot = newYaw;
            yHeadRot = newYaw;
            float horiz = (float) Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
            float targetPitch = Mth.clamp((float) (-Mth.atan2(lookVec.y, horiz) * (180.0D / Math.PI)), -45.0F, 55.0F);
            setXRot(Mth.rotLerp(0.20F, getXRot(), targetPitch));
        }
    }

    private void tickStuckResolution(ServerLevel level, ServerPlayer target) {
        if (tickCount % 20 != 0) return;
        Vec3 current = position();
        if (lastMovementCheck == Vec3.ZERO) {
            lastMovementCheck = current;
            return;
        }
        double moved = current.distanceToSqr(lastMovementCheck);
        lastMovementCheck = current;
        boolean pausedForCast = beamChargeTicks > 0 || resonanceSweepTicks > 0 || wingCycloneTicks > 0;
        if (!pausedForCast && (moved < 0.65D || horizontalCollision || verticalCollision)) stuckTicks += 20;
        else stuckTicks = Math.max(0, stuckTicks - 40);
        if (stuckTicks < 60 || emergencyTeleportCooldown > 0) return;

        Vec3 destination;
        if (target != null) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 14.0D + random.nextDouble() * 8.0D;
            destination = new Vec3(target.getX() + Math.cos(angle) * radius,
                Math.max(arena.getY() + 14.0D, target.getY() + 12.0D),
                target.getZ() + Math.sin(angle) * radius);
        } else {
            destination = Vec3.atCenterOf(arena).add(0.0D, 26.0D, 0.0D);
        }
        performTeleport(level, destination, true);
        stuckTicks = 0;
        emergencyTeleportCooldown = 100;
    }

    private void teleportAroundTarget(ServerLevel level, ServerPlayer target, int phase) {
        Vec3 from = position();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 10.0D + random.nextDouble() * (phase == 3 ? 10.0D : 7.0D);
        double height = 7.0D + random.nextDouble() * 7.0D;
        Vec3 destination = new Vec3(target.getX() + Math.cos(angle) * radius,
            Math.max(arena.getY() + 10.0D, target.getY() + height),
            target.getZ() + Math.sin(angle) * radius);
        performTeleport(level, destination, false);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, from.x, from.y + 1.0D, from.z,
            50, 1.8D, 1.2D, 1.8D, 0.06D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0D, getZ(),
            70, 1.6D, 1.4D, 1.6D, 0.16D);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 4.0F, 0.72F);
        getLookControl().setLookAt(target, 45.0F, 45.0F);
    }

    private void performTeleport(ServerLevel level, Vec3 destination, boolean emergency) {
        double dx = destination.x - (arena.getX() + 0.5D);
        double dz = destination.z - (arena.getZ() + 0.5D);
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > ARENA_RADIUS - 6.0D && distance > 1.0E-4D) {
            double scale = (ARENA_RADIUS - 6.0D) / distance;
            destination = new Vec3(arena.getX() + 0.5D + dx * scale, destination.y,
                arena.getZ() + 0.5D + dz * scale);
        }
        teleportTo(destination.x, Mth.clamp(destination.y, arena.getY() + 8.0D, arena.getY() + 42.0D), destination.z);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        if (emergency) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0D, getZ(),
                90, 2.0D, 1.8D, 2.0D, 0.2D);
            level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 4.0F, 0.55F);
        }
    }

    private void cancelPlayerLockedActions() {
        queuedShots = 0;
        queuedTarget = null;
        breathDiveTicks = 0;
        breathTarget = null;
        beamChargeTicks = 0;
        beamTarget = null;
        chainTicks = 0;
        chainedPlayers.clear();
        comboId = 0;
        comboStep = 0;
    }

    private boolean insideArena(Vec3 position, double radius) {
        if (arena.equals(BlockPos.ZERO)) return true;
        double dx = position.x - (arena.getX() + 0.5D);
        double dz = position.z - (arena.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    private void executeSkill(ServerLevel level, int phase, ServerPlayer target, int skill) {
        lastSkill = skill;
        switch (skill) {
            case SKILL_BEAM -> { startSonicBeam(level, target); skillCooldowns[skill] = 70; }
            case SKILL_VOLLEY -> { startVolley(target, random.nextBoolean() ? 1 : 2); skillCooldowns[skill] = 45; }
            case SKILL_DIVE -> { startBreathDive(level, target); skillCooldowns[skill] = 95; }
            case SKILL_SLAM -> { startSlam(level, target); skillCooldowns[skill] = 150; }
            case SKILL_RAIN -> { startEnderRain(level, phase); skillCooldowns[skill] = 120; }
            case SKILL_CYCLONE -> { startWingCyclone(level); skillCooldowns[skill] = 100; }
            case SKILL_CHAINS -> { startEchoChains(level); skillCooldowns[skill] = 140; }
            case SKILL_SWEEP -> { startResonanceSweep(level); skillCooldowns[skill] = 110; }
            case SKILL_HARVEST -> { startSoulHarvest(level); skillCooldowns[skill] = 260; }
            case SKILL_COLLAPSE -> { startVoidCollapse(level); skillCooldowns[skill] = 300; }
            default -> { }
        }
    }

    private boolean isBusy() {
        return queuedShots > 0 || breathDiveTicks > 0 || slamTicks > 0 || beamChargeTicks > 0
            || wingCycloneTicks > 0 || resonanceSweepTicks > 0 || voidCollapseTicks > 0
            || soulHarvestTicks > 0 || chainTicks > 0 || enderRainMarksRemaining > 0
            || !pendingEnderRain.isEmpty();
    }

    private void startVolley(ServerPlayer target, int projectileKind) {
        queuedProjectileKind = projectileKind;
        queuedShots = 3;
        nextQueuedShotTick = tickCount + 1;
        queuedTarget = target.getUUID();
    }

    private void tickQueuedVolley(ServerLevel level, int phase) {
        if (queuedShots <= 0 || tickCount < nextQueuedShotTick) return;
        Player target = queuedTarget == null ? null : level.getPlayerByUUID(queuedTarget);
        if (!(target instanceof ServerPlayer sp) || !isValidArenaTarget(sp)) {
            queuedShots = 0;
            queuedTarget = null;
            return;
        }
        Vec3 origin = getEyePosition();
        Vec3 leadPos = sp.getEyePosition().add(sp.getDeltaMovement().scale(4.0D));
        Vec3 direction = leadPos.subtract(origin).normalize();
        EntityType<? extends AbstractHurtingProjectile> type = queuedProjectileKind == 1
            ? EntityType.DRAGON_FIREBALL : EntityType.WITHER_SKULL;
        AbstractHurtingProjectile projectile = type.create(level);
        if (projectile != null) {
            projectile.setOwner(this);
            if (projectile instanceof WitherSkull skull) skull.setDangerous(phase >= 2);
            projectile.setPos(origin);
            projectile.setDeltaMovement(direction.scale(1.25D + phase * 0.16D));
            level.addFreshEntity(projectile);
        }
        queuedShots--;
        nextQueuedShotTick = tickCount + 8;
        if (queuedShots == 0) queuedTarget = null;
    }

    private void startBreathDive(ServerLevel level, ServerPlayer target) {
        breathDiveTicks = 42;
        breathTarget = target.getUUID();
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 0.72F);
    }

    private void tickBreathDive(ServerLevel level, int phase) {
        if (breathDiveTicks <= 0) return;
        Player target = breathTarget == null ? null : level.getPlayerByUUID(breathTarget);
        if (!(target instanceof ServerPlayer sp) || !isValidArenaTarget(sp)) {
            breathDiveTicks = 0;
            breathTarget = null;
            return;
        }

        Vec3 origin = getEyePosition();
        Vec3 targetPoint = sp.position().add(0.0D, 1.0D, 0.0D);
        Vec3 direction = targetPoint.subtract(origin).normalize();
        if (breathDiveTicks > 10) {
            Vec3 dive = direction.scale(1.15D + phase * 0.08D);
            setDeltaMovement(getDeltaMovement().scale(0.35D).add(dive.scale(0.65D)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.45D).add(0.0D, 0.55D, 0.0D));
        }
        hasImpulse = true;

        for (int step = 1; step <= 10; step++) {
            Vec3 point = origin.add(direction.scale(step * 1.35D));
            level.sendParticles(ParticleTypes.DRAGON_BREATH, point.x, point.y, point.z,
                2, 0.22D, 0.22D, 0.22D, 0.02D);
        }
        if (breathDiveTicks % 8 == 0 && sp.distanceToSqr(this) <= 18.0D * 18.0D && hasLineOfSight(sp)) {
            float breathDamage = 8.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase];
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                sp.getBoundingBox().inflate(3.5D), this::isValidArenaTarget)) {
                player.hurt(level.damageSources().mobAttack(this), breathDamage);
            }
        }
        breathDiveTicks--;
        if (breathDiveTicks == 0) breathTarget = null;
    }

    private void startSlam(ServerLevel level, ServerPlayer target) {
        slamTicks = 64;
        slamX = target.getX();
        slamY = target.getY();
        slamZ = target.getZ();
        level.broadcastEntityEvent(this, EVENT_SLAM_START);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
            SoundSource.HOSTILE, 6.0F, 0.55F);
    }

    /**
     * Occasionally replaces a normal Dragon Fireball volley after phase one.
     * Marks are distributed uniformly over the circular combat floor rather
     * than clustering at the centre or directly under one player.
     */
    private void startEnderRain(ServerLevel level, int phase) {
        ServerPlayer target = nearestTarget(level);
        if (target == null) return;
        enderRainMarksRemaining = 34 + phase * 8;
        nextEnderRainMarkTick = tickCount + 1;
        enderRainEndTick = tickCount + 100; // five seconds
        enderRainTarget = target.getUUID();
        enderRainFinalQueued = false;
        level.playSound(null, arena, SoundEvents.WARDEN_SONIC_CHARGE,
            SoundSource.HOSTILE, 5.5F, 0.72F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            arena.getX() + 0.5D, arena.getY() + 34.0D, arena.getZ() + 0.5D,
            180, 26.0D, 12.0D, 26.0D, 0.18D);
    }

    private void tickEnderRain(ServerLevel level, int phase) {
        if (enderRainMarksRemaining > 0 && tickCount < enderRainEndTick && tickCount >= nextEnderRainMarkTick) {
            Player p = enderRainTarget == null ? null : level.getPlayerByUUID(enderRainTarget);
            ServerPlayer tracked = p instanceof ServerPlayer sp && isValidArenaTarget(sp) ? sp : null;
            Vec3 focus = tracked != null
                ? tracked.position() : new Vec3(arena.getX() + 0.5D, arena.getY() + 1.0D, arena.getZ() + 0.5D);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 5.0D + Math.sqrt(random.nextDouble()) * 31.0D;
            double x = Mth.clamp(focus.x + Math.cos(angle) * radius,
                arena.getX() - ARENA_RADIUS + 5.0D, arena.getX() + ARENA_RADIUS - 5.0D);
            double z = Mth.clamp(focus.z + Math.sin(angle) * radius,
                arena.getZ() - ARENA_RADIUS + 5.0D, arena.getZ() + ARENA_RADIUS - 5.0D);
            double y = arena.getY() + 1.05D;
            pendingEnderRain.add(new EnderRainStrike(x, y, z, tickCount + 8, false));
            level.sendParticles(ParticleTypes.PORTAL, x, y + 0.15D, z,
                22, 0.75D, 0.08D, 0.75D, 0.05D);
            enderRainMarksRemaining--;
            nextEnderRainMarkTick = tickCount + 3;
        }

        if (!enderRainFinalQueued && enderRainEndTick > 0 && tickCount >= enderRainEndTick) {
            Player p = enderRainTarget == null ? null : level.getPlayerByUUID(enderRainTarget);
            ServerPlayer tracked = p instanceof ServerPlayer sp && isValidArenaTarget(sp) ? sp : nearestTarget(level);
            Vec3 impact = tracked != null ? tracked.position() : new Vec3(arena.getX() + 0.5D, arena.getY() + 1.0D, arena.getZ() + 0.5D);
            pendingEnderRain.add(new EnderRainStrike(impact.x, arena.getY() + 1.05D, impact.z, tickCount + 12, true));
            enderRainFinalQueued = true;
            enderRainMarksRemaining = 0;
            level.playSound(null, BlockPos.containing(impact.x, impact.y, impact.z), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 6.0F, 0.45F);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, impact.x, impact.y + 0.2D, impact.z,
                120, 2.4D, 0.15D, 2.4D, 0.03D);
        }

        Iterator<EnderRainStrike> iterator = pendingEnderRain.iterator();
        while (iterator.hasNext()) {
            EnderRainStrike strike = iterator.next();
            level.sendParticles(strike.huge() ? ParticleTypes.DRAGON_BREATH : ParticleTypes.REVERSE_PORTAL,
                strike.x(), strike.y() + 0.20D, strike.z(),
                strike.huge() ? 14 : 3, strike.huge() ? 1.6D : 0.55D, 0.05D,
                strike.huge() ? 1.6D : 0.55D, 0.02D);
            if (tickCount < strike.releaseTick()) continue;

            Vec3 origin = new Vec3(
                strike.x() + (random.nextDouble() - 0.5D) * (strike.huge() ? 2.0D : 5.0D),
                arena.getY() + (strike.huge() ? 52.0D : 34.0D + random.nextDouble() * 8.0D),
                strike.z() + (random.nextDouble() - 0.5D) * (strike.huge() ? 2.0D : 5.0D));
            Vec3 impact = new Vec3(strike.x(), strike.y(), strike.z());
            Vec3 direction = impact.subtract(origin).normalize();
            AbstractHurtingProjectile projectile = EntityType.DRAGON_FIREBALL.create(level);
            if (projectile != null) {
                projectile.setOwner(this);
                projectile.setPos(origin);
                projectile.setDeltaMovement(direction.scale(strike.huge() ? 1.05D : 1.35D + phase * 0.10D));
                level.addFreshEntity(projectile);
            }
            level.sendParticles(ParticleTypes.DRAGON_BREATH,
                origin.x, origin.y, origin.z, strike.huge() ? 180 : 10,
                strike.huge() ? 3.0D : 0.35D, strike.huge() ? 3.0D : 0.35D,
                strike.huge() ? 3.0D : 0.35D, strike.huge() ? 0.12D : 0.03D);
            if (strike.huge()) {
                pendingMeteorImpacts.add(new MeteorImpact(impact, tickCount + 34));
                level.playSound(null, BlockPos.containing(origin.x, origin.y, origin.z), SoundEvents.ENDER_DRAGON_SHOOT,
                    SoundSource.HOSTILE, 7.0F, 0.35F);
            }
            iterator.remove();
        }

        Iterator<MeteorImpact> impactIterator = pendingMeteorImpacts.iterator();
        while (impactIterator.hasNext()) {
            MeteorImpact impact = impactIterator.next();
            if (tickCount < impact.detonateTick()) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH, impact.position().x,
                    arena.getY() + 2.0D + (impact.detonateTick() - tickCount) * 1.2D,
                    impact.position().z, 10, 1.2D, 1.2D, 1.2D, 0.04D);
                continue;
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.position().x, impact.position().y + 0.5D,
                impact.position().z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, impact.position().x, impact.position().y + 0.5D,
                impact.position().z, 260, 5.0D, 1.0D, 5.0D, 0.12D);
            level.playSound(null, impact.position().x, impact.position().y, impact.position().z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 7.0F, 0.5F);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(impact.position(), impact.position()).inflate(9.0D, 5.0D, 9.0D), this::isValidArenaTarget)) {
                player.hurt(level.damageSources().mobAttack(this),
                    32.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase]);
                Vec3 push = player.position().subtract(impact.position());
                if (push.lengthSqr() > 0.01D) {
                    push = push.normalize().scale(1.8D);
                    player.push(push.x, 0.85D, push.z);
                }
            }
            impactIterator.remove();
            enderRainEndTick = 0;
            enderRainTarget = null;
        }
    }

    private void tickSlam(ServerLevel level, int phase) {
        if (slamTicks <= 0) return;
        if (slamTicks > 44) {
            Vec3 staging = new Vec3(slamX, slamY + 28.0D, slamZ);
            Vec3 rise = staging.subtract(position());
            if (rise.lengthSqr() > 1.0E-4D) rise = rise.normalize().scale(1.05D);
            setDeltaMovement(getDeltaMovement().scale(0.30D).add(rise.scale(0.70D)));
            setXRot(-18.0F);
        } else {
            Vec3 impact = new Vec3(slamX, slamY + 0.4D, slamZ);
            Vec3 dive = impact.subtract(position());
            if (dive.lengthSqr() > 1.0E-4D) dive = dive.normalize().scale(2.15D + phase * 0.12D);
            setDeltaMovement(getDeltaMovement().scale(0.12D).add(dive.scale(0.88D)));
            setXRot(78.0F);
            level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(),
                10, 0.8D, 0.8D, 0.8D, 0.08D);

            HitResult floor = level.clip(new ClipContext(position(), position().add(0.0D, -4.5D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (slamTicks < 38 && (onGround() || verticalCollision || floor.getType() == HitResult.Type.BLOCK)) {
                finishSlam(level, phase);
                return;
            }
        }
        hasImpulse = true;
        slamTicks--;
        if (slamTicks == 0) setXRot(0.0F);
    }

    private void finishSlam(ServerLevel level, int phase) {
        slamTicks = 0;
        setXRot(0.0F);
        level.broadcastEntityEvent(this, EVENT_SLAM_IMPACT);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE,
            SoundSource.HOSTILE, 7.0F, 0.55F);
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
            SoundSource.HOSTILE, 5.0F, 0.62F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(),
            3, 1.5D, 0.5D, 1.5D, 0.0D);

        double radius = 18.0D;
        float maximumDamage = 34.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase];
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
            getBoundingBox().inflate(radius), this::isValidArenaTarget)) {
            double distance = Math.sqrt(player.distanceToSqr(this));
            if (distance > radius) continue;
            double strength = Math.max(0.15D, 1.0D - distance / radius);
            player.hurt(level.damageSources().mobAttack(this), (float) (maximumDamage * strength));
            Vec3 push = player.position().subtract(position());
            if (push.lengthSqr() > 1.0E-4D) push = push.normalize().scale(2.8D * strength);
            player.push(push.x, 0.75D + strength * 0.65D, push.z);
        }
        for (int i = 0; i < 72; i++) {
            double angle = Math.PI * 2.0D * i / 72.0D;
            double ring = 3.0D + (i % 3) * 3.2D;
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                getX() + Math.cos(angle) * ring, getY() + 0.35D,
                getZ() + Math.sin(angle) * ring, 2, 0.15D, 0.12D, 0.15D, 0.04D);
        }
        setDeltaMovement(0.0D, 1.25D, 0.0D);
        hasImpulse = true;
    }

    private void startSonicBeam(ServerLevel level, ServerPlayer target) {
        beamChargeTicks = 20;
        beamTarget = target.getUUID();
        beamAim = target.getEyePosition();
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0F, 0.72F);
    }

    private void tickSonicBeam(ServerLevel level, int phase) {
        if (beamChargeTicks <= 0) return;
        Player target = beamTarget == null ? null : level.getPlayerByUUID(beamTarget);
        if (target instanceof ServerPlayer sp && isValidArenaTarget(sp) && beamChargeTicks > 8) {
            beamAim = sp.getEyePosition();
        }
        Vec3 origin = getEyePosition();
        Vec3 direction = beamAim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D) direction = new Vec3(0.0D, -1.0D, 0.0D);
        else direction = direction.normalize();
        for (int i = 1; i <= 28; i++) {
            Vec3 point = origin.add(direction.scale(i * 2.0D));
            level.sendParticles(ParticleTypes.SCULK_SOUL, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        beamChargeTicks--;
        if (beamChargeTicks > 0) return;
        float damage = 22.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase];
        Vec3 beamEnd = origin.add(direction.scale(96.0D));
        AABB beamBox = new AABB(origin, beamEnd).inflate(2.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, beamBox, this::isValidArenaTarget)) {
            Vec3 closest = closestPointOnSegment(origin, beamEnd, player.getEyePosition());
            if (closest.distanceToSqr(player.getEyePosition()) <= 2.5D * 2.5D) {
                player.hurt(level.damageSources().mobAttack(this), damage);
            }
        }
        for (int i = 1; i <= 48; i++) {
            Vec3 point = origin.add(direction.scale(i * 2.0D));
            level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 0.75F + phase * 0.08F);
        beamTarget = null;
    }

    private Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        if (len < 1.0E-6D) return a;
        double t = Mth.clamp(point.subtract(a).dot(ab) / len, 0.0D, 1.0D);
        return a.add(ab.scale(t));
    }

    private void startWingCyclone(ServerLevel level) {
        wingCycloneTicks = 36;
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 6.0F, 0.65F);
    }

    private void tickWingCyclone(ServerLevel level, int phase) {
        if (wingCycloneTicks <= 0) return;
        wingCycloneTicks--;
        double floorY = arena.equals(BlockPos.ZERO) ? getY() : arena.getY() + 1.0D;
        double desiredY = floorY + 9.0D;
        setDeltaMovement(getDeltaMovement().scale(0.35D).add(0.0D, Mth.clamp((desiredY - getY()) * 0.08D, -0.35D, 0.25D), 0.0D));
        hasImpulse = true;

        double radius = 5.0D + (36 - wingCycloneTicks) * 0.38D;
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2.0D * i / 32.0D + tickCount * 0.20D;
            level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * radius, floorY + 0.5D,
                getZ() + Math.sin(a) * radius, 1, 0, 0.08D, 0, 0.01D);
        }
        if (wingCycloneTicks % 6 == 0) {
            Vec3 groundCenter = new Vec3(getX(), floorY, getZ());
            AABB area = new AABB(groundCenter, groundCenter).inflate(18.0D, 14.0D, 18.0D);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, this::isValidArenaTarget)) {
                Vec3 push = new Vec3(player.getX() - getX(), 0.0D, player.getZ() - getZ());
                double horizDistSqr = push.lengthSqr();
                if (horizDistSqr < 0.01D || horizDistSqr > 18.0D * 18.0D) continue;
                push = push.normalize().scale(1.4D + phase * 0.18D);
                player.push(push.x, 0.25D, push.z);
                if (horizDistSqr < 8.5D * 8.5D) {
                    player.hurt(level.damageSources().mobAttack(this), 6.0F + phase * 2.0F);
                }
            }
        }
    }

    private void startEchoChains(ServerLevel level) {
        chainedPlayers.clear();
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
            new AABB(arena).inflate(80.0D), this::isValidArenaTarget);
        players.sort(Comparator.comparingDouble(this::distanceToSqr));
        for (int i = 0; i < Math.min(2, players.size()); i++) chainedPlayers.add(players.get(i).getUUID());
        if (chainedPlayers.size() == 1) {
            ServerPlayer solo = players.get(0);
            chainAnchor = new Vec3(solo.getX(), arena.equals(BlockPos.ZERO) ? solo.getY() : arena.getY() + 1.0D, solo.getZ());
        }
        chainTicks = 80;
        level.playSound(null, arena, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 4.0F, 0.65F);
    }

    private void tickEchoChains(ServerLevel level, int phase) {
        if (chainTicks <= 0) return;
        chainTicks--;
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID id : chainedPlayers) {
            Player found = level.getPlayerByUUID(id);
            if (found instanceof ServerPlayer p && isValidArenaTarget(p)) players.add(p);
        }
        if (players.isEmpty()) { chainTicks = 0; return; }
        Vec3 a = players.get(0).getEyePosition();
        Vec3 b = players.size() > 1 ? players.get(1).getEyePosition() : chainAnchor;
        Vec3 delta = b.subtract(a);
        for (int i = 0; i <= 20; i++) {
            Vec3 p = a.add(delta.scale(i / 20.0D));
            level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (chainTicks == 0 && a.distanceTo(b) > 10.0D) {
            for (ServerPlayer p : players) {
                p.hurt(level.damageSources().mobAttack(this), 14.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase]);
            }
            level.playSound(null, arena, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.25F);
        }
    }

    private void startResonanceSweep(ServerLevel level) {
        resonanceSweepTicks = 52;
        resonanceSweepAngle = (float) (getYRot() * Math.PI / 180.0D) - 1.6F;
        sweepHitThisPulse.clear();
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0F, 1.18F);
    }

    private void tickResonanceSweep(ServerLevel level, int phase) {
        if (resonanceSweepTicks <= 0) return;
        resonanceSweepTicks--;
        resonanceSweepAngle += 0.065F;
        if (resonanceSweepTicks % 8 == 0) sweepHitThisPulse.clear();

        double floorY = arena.equals(BlockPos.ZERO) ? getY() : arena.getY() + 1.2D;
        Vec3 origin = new Vec3(getX(), floorY, getZ());
        Vec3 dir = new Vec3(Math.cos(resonanceSweepAngle), 0.0D, Math.sin(resonanceSweepAngle));
        Vec3 sweepEnd = origin.add(dir.scale(36.0D));
        for (int i = 2; i <= 34; i += 2) {
            Vec3 p = origin.add(dir.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        AABB sweepBox = new AABB(origin, sweepEnd).inflate(2.5D, 3.5D, 2.5D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, sweepBox, this::isValidArenaTarget)) {
            Vec3 playerHorizontal = new Vec3(player.getX(), floorY, player.getZ());
            Vec3 closest = closestPointOnSegment(origin, sweepEnd, playerHorizontal);
            if (closest.distanceToSqr(playerHorizontal) <= 2.25D * 2.25D && Math.abs(player.getY() - floorY) <= 3.5D) {
                if (sweepHitThisPulse.add(player.getUUID())) {
                    player.hurt(level.damageSources().mobAttack(this), 10.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase]);
                    player.push(dir.x * 1.2D, 0.35D, dir.z * 1.2D);
                }
            }
        }
    }

    private void startVoidCollapse(ServerLevel level) {
        voidCollapseTicks = 120;
        level.playSound(null, arena, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 5.0F, 0.72F);
    }

    private void tickVoidCollapse(ServerLevel level, int phase) {
        if (voidCollapseTicks <= 0) return;
        voidCollapseTicks--;
        boolean outerUnsafe = voidCollapseTicks > 60;
        double safeRadius = outerUnsafe ? 20.0D : 31.0D;
        for (int i = 0; i < 72; i++) {
            double a = Math.PI * 2.0D * i / 72.0D;
            level.sendParticles(outerUnsafe ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.DRAGON_BREATH,
                arena.getX() + 0.5D + Math.cos(a) * safeRadius, arena.getY() + 1.0D,
                arena.getZ() + 0.5D + Math.sin(a) * safeRadius, 1, 0, 0.05D, 0, 0.01D);
        }
        if (voidCollapseTicks % 10 != 0) return;
        Vec3 center = Vec3.atCenterOf(arena);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
            new AABB(arena).inflate(52.0D), this::isValidArenaTarget)) {
            double horizontal = new Vec3(player.getX() - center.x, 0.0D, player.getZ() - center.z).length();
            boolean unsafe = outerUnsafe ? horizontal > safeRadius : horizontal < safeRadius;
            if (unsafe) player.hurt(level.damageSources().mobAttack(this), 7.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase]);
        }
    }

    private void startSoulHarvest(ServerLevel level) {
        soulHarvestTicks = 120;
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0D * i / 6.0D;
            ArmorStand soul = EntityType.ARMOR_STAND.create(level);
            if (soul == null) continue;
            soul.addTag(SOUL_TAG);
            soul.setInvisible(true);
            soul.setNoGravity(true);
            soul.setCustomName(Component.literal("Echo Soul").withStyle(ChatFormatting.AQUA));
            soul.setCustomNameVisible(true);
            soul.moveTo(arena.getX() + 0.5D + Math.cos(a) * 38.0D, arena.getY() + 2.0D,
                arena.getZ() + 0.5D + Math.sin(a) * 38.0D, 0.0F, 0.0F);
            level.addFreshEntity(soul);
        }
        level.playSound(null, arena, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 5.0F, 0.65F);
    }

    private void tickSoulHarvest(ServerLevel level, int phase) {
        if (soulHarvestTicks <= 0) return;
        soulHarvestTicks--;
        List<ArmorStand> souls = level.getEntitiesOfClass(ArmorStand.class,
            new AABB(arena).inflate(80.0D), e -> e.isAlive() && e.getTags().contains(SOUL_TAG));
        List<ServerPlayer> arenaPlayers = level.getEntitiesOfClass(ServerPlayer.class,
            new AABB(arena).inflate(ARENA_RADIUS), this::isValidArenaTarget);

        for (ArmorStand soul : souls) {
            // Allow players to shatter Echo Souls by intercepting them before they reach the dragon.
            boolean shattered = false;
            for (ServerPlayer player : arenaPlayers) {
                if (player.distanceToSqr(soul) <= 2.6D * 2.6D) {
                    level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, soul.getX(), soul.getY() + 0.8D, soul.getZ(),
                        24, 0.4D, 0.4D, 0.4D, 0.05D);
                    level.playSound(null, soul.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                        SoundSource.PLAYERS, 2.0F, 1.25F);
                    soul.discard();
                    shattered = true;
                    break;
                }
            }
            if (shattered) continue;

            Vec3 target = position().add(0.0D, 1.0D, 0.0D);
            Vec3 move = target.subtract(soul.position());
            if (move.lengthSqr() < 4.0D) {
                encounterHealth = Math.min(encounterMaxHealth, encounterHealth + encounterMaxHealth * 0.005F);
                level.sendParticles(ParticleTypes.SCULK_SOUL, soul.getX(), soul.getY(), soul.getZ(), 28, 0.5D, 0.5D, 0.5D, 0.08D);
                soul.discard();
            } else {
                Vec3 step = move.normalize().scale(0.24D + phase * 0.03D);
                soul.setPos(soul.getX() + step.x, soul.getY() + step.y, soul.getZ() + step.z);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, soul.getX(), soul.getY() + 0.6D, soul.getZ(), 3, 0.18D, 0.25D, 0.18D, 0.01D);
            }
        }
        if (soulHarvestTicks == 0) for (ArmorStand soul : souls) soul.discard();
    }

    private void devourEcho(ServerLevel level, ServerPlayer target) {
        Vec3 behind = target.getLookAngle().scale(-6.0D);
        Vec3 destination = new Vec3(target.getX() + behind.x, target.getY() + 5.0D, target.getZ() + behind.z);
        performTeleport(level, destination, false);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 80, 1.8D, 1.8D, 1.8D, 0.18D);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 5.0F, 0.55F);
        startBreathDive(level, target);
    }

    private void scheduleCrystalDetonation() {
        for (Vec3 site : phaseCrystalSites) pendingCrystalBlasts.add(new DelayedBlast(site, tickCount + 40));
        phaseCrystalSites.clear();
    }

    private void tickCrystalDetonations(ServerLevel level, int phase) {
        Iterator<DelayedBlast> it = pendingCrystalBlasts.iterator();
        while (it.hasNext()) {
            DelayedBlast blast = it.next();
            level.sendParticles(ParticleTypes.SCULK_SOUL, blast.position.x, blast.position.y, blast.position.z, 3, 0.8D, 0.8D, 0.8D, 0.03D);
            if (tickCount < blast.detonateTick) continue;
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, blast.position.x, blast.position.y, blast.position.z, 1, 0, 0, 0, 0);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(blast.position, blast.position).inflate(6.0D), this::isValidArenaTarget)) {
                player.hurt(level.damageSources().mobAttack(this), 12.0F * LastEchoProfile.PHASE_DAMAGE_MULTIPLIER[phase]);
            }
            it.remove();
        }
    }

    private void startCombo(int id) {
        comboId = id;
        comboStep = 0;
        comboNextTick = tickCount + 1;
    }

    private void tickCombo(ServerLevel level, int phase) {
        if (comboId == 0 || tickCount < comboNextTick || isBusy()) return;
        ServerPlayer target = nearestTarget(level);
        if (target == null) { comboId = 0; return; }
        if (comboId == 1) { // Mark and Dive
            if (comboStep == 0) { executeSkill(level, phase, target, SKILL_RAIN); comboNextTick = tickCount + 80; }
            else { executeSkill(level, phase, target, SKILL_DIVE); comboId = 0; }
        } else if (comboId == 2) { // Skybreaker
            if (comboStep == 0) { startVolley(target, 2); comboNextTick = tickCount + 50; }
            else { executeSkill(level, phase, target, SKILL_SLAM); comboId = 0; }
        } else if (comboId == 3) { // No Safe Ground
            if (comboStep == 0) { executeSkill(level, phase, target, SKILL_RAIN); comboNextTick = tickCount + 85; }
            else if (comboStep == 1) { executeSkill(level, phase, target, SKILL_SLAM); comboNextTick = tickCount + 90; }
            else { executeSkill(level, phase, target, SKILL_CYCLONE); comboId = 0; }
        } else if (comboId == 4) { // Final Refrain
            if (comboStep == 0) { executeSkill(level, phase, target, SKILL_HARVEST); comboNextTick = tickCount + 140; }
            else if (comboStep == 1) { echoCloneTelegraph(level); executeSkill(level, phase, target, SKILL_SWEEP); comboNextTick = tickCount + 75; }
            else { executeSkill(level, phase, target, SKILL_SLAM); comboId = 0; skillCooldowns[SKILL_SLAM] = 180; }
        }
        comboStep++;
    }

    private void echoCloneTelegraph(ServerLevel level) {
        for (int clone = 0; clone < 3; clone++) {
            double a = Math.PI * 2.0D * clone / 3.0D;
            double x = getX() + Math.cos(a) * 10.0D;
            double z = getZ() + Math.sin(a) * 10.0D;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, getY() + 1.5D, z, 55, 1.6D, 1.4D, 1.6D, 0.10D);
        }
        level.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 4.0F, 0.8F);
    }

    public boolean isClientSlamming() { return clientSlamTicks > 0; }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SLAM_START) {
            clientSlamTicks = 64;
        } else if (id == EVENT_SLAM_IMPACT) {
            clientSlamTicks = 0;
            EchoboundClientEffects.startShake(position(), 18, 1.0F);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel serverLevel)) return super.hurt(source, amount);
        boolean bypasses = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypasses) {
            if (source.getEntity() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) return false;
            List<EndCrystal> crystals = activeCrystals(serverLevel);
            // The boss uses its invulnerable texture whenever an Echo Crystal is active,
            // so combat must match that visual state in every crystal phase.
            if (!crystals.isEmpty()) return false;
        }
        if (!scaled) scaleForPlayers(serverLevel);

        if (bypasses && amount >= encounterHealth) {
            encounterHealth = 0.0F;
            setHealth(0.0F);
            die(source);
            return true;
        }

        // Let vanilla calculate armor, enchantment, invulnerability frames and damage-source
        // rules against a safe 1000-HP segment, then subtract the real accepted damage from
        // the encounter pool.
        setHealth(PHYSICAL_HEALTH_SEGMENT);
        boolean accepted = super.hurt(source, Math.min(Math.max(0.0F, amount), PHYSICAL_HEALTH_SEGMENT - 1.0F));
        if (!accepted) return false;
        float dealt = isDeadOrDying() ? amount : Math.max(0.0F, PHYSICAL_HEALTH_SEGMENT - getHealth());
        encounterHealth = Math.max(0.0F, encounterHealth - dealt);
        if (encounterHealth <= 0.0F) {
            if (!isDeadOrDying()) {
                die(source);
            }
        } else {
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        }
        return true;
    }

    public float deathAnimationProgress(float partialTick) {
        return Mth.clamp((deathTime + partialTick) / 240.0F, 0.0F, 1.0F);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        setYRot(getYRot());
        setDeltaMovement(0.0D, 0.08D, 0.0D);

        if (!(level() instanceof ServerLevel serverLevel)) {
            if (deathTime >= 240) remove(Entity.RemovalReason.KILLED);
            return;
        }

        if (deathTime == 1) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH,
                SoundSource.HOSTILE, 10.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.0D, getZ(),
                200, 4.0D, 3.0D, 4.0D, 0.15D);
        }

        if (deathTime % 4 == 0) {
            double ox = (random.nextDouble() - 0.5D) * 8.0D;
            double oy = random.nextDouble() * 5.0D;
            double oz = (random.nextDouble() - 0.5D) * 8.0D;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX() + ox, getY() + oy, getZ() + oz,
                3, 0.5D, 0.5D, 0.5D, 0.0D);
            serverLevel.playSound(null, getX() + ox, getY() + oy, getZ() + oz,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.8F + random.nextFloat() * 0.4F);
        }

        if (deathTime >= 100 && deathTime % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.0D, getZ(),
                60, 5.0D, 4.0D, 5.0D, 0.25D);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 2.0D, getZ(),
                80, 4.0D, 3.0D, 4.0D, 0.10D);
            serverLevel.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 2.5D, getZ(),
                40, 3.0D, 3.0D, 3.0D, 0.20D);
        }

        if (deathTime >= 200 && deathTime <= 240) {
            int xpAmount = (deathTime == 240) ? 2000 : 500;
            net.minecraft.world.entity.ExperienceOrb.award(serverLevel, position(), xpAmount);
        }

        if (deathTime >= 240) {
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 10.0F, 0.5F);
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 10.0F, 0.6F);

            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2.0D, getZ(),
                15, 3.0D, 3.0D, 3.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.0D, getZ(),
                300, 6.0D, 4.0D, 6.0D, 0.15D);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY() + 2.0D, getZ(),
                150, 4.0D, 5.0D, 4.0D, 0.10D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.0D, getZ(),
                500, 8.0D, 6.0D, 8.0D, 0.35D);

            buildExitPortal(serverLevel, arena);
            spawnBossLoot(serverLevel);
            remove(Entity.RemovalReason.KILLED);
        }
    }

    private void spawnBossLoot(ServerLevel level) {
        spawnAtLocation(VeloriseEMC.HEART_OF_THE_LAST_ECHO.get());
        int scales = LastEchoProfile.MIN_SCALE_DROP + random.nextInt(LastEchoProfile.MAX_SCALE_DROP - LastEchoProfile.MIN_SCALE_DROP + 1);
        for (int i = 0; i < scales; i++) spawnAtLocation(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get());
        LastEchoCitadelData.get(getServer()).markDefeated();
    }

    private void buildExitPortal(ServerLevel level, BlockPos center) {
        if (center.equals(BlockPos.ZERO)) return;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int distSq = x * x + z * z;
                BlockPos pos = center.offset(x, 0, z);
                if (distSq <= 2) {
                    level.setBlock(pos, Blocks.END_PORTAL.defaultBlockState(), 3);
                } else if (distSq <= 4) {
                    level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                }
            }
        }
        // Central Bedrock Pillar retaining the Last Echo Altar at Y+3 and crowning with the Dragon Egg at Y+4
        level.setBlock(center.above(1), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(center.above(2), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(center.above(3), VeloriseEMC.LAST_ECHO_ALTAR.get().defaultBlockState(), 3);
        level.setBlock(center.above(4), Blocks.DRAGON_EGG.defaultBlockState(), 3);
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isDeadOrDying();
        if (first) {
            encounterHealth = 0.0F;
            setHealth(0.0F);
            setNoAi(true);
            setTarget(null);
            setDeltaMovement(0.0D, -0.12D, 0.0D);
            queuedShots = 0;
            queuedTarget = null;
            breathDiveTicks = 0;
            breathTarget = null;
            slamTicks = 0;
            enderRainMarksRemaining = 0;
            enderRainEndTick = 0;
            enderRainTarget = null;
            pendingEnderRain.clear();
            pendingMeteorImpacts.clear();
            pendingCrystalBlasts.clear();
            if (level() instanceof ServerLevel serverLevel) {
                for (ArmorStand soul : serverLevel.getEntitiesOfClass(ArmorStand.class,
                    getBoundingBox().inflate(96.0D), e -> e.getTags().contains(SOUL_TAG))) {
                    soul.discard();
                }
                for (EndCrystal crystal : activeCrystals(serverLevel)) {
                    crystal.discard();
                }
                for (AbstractHurtingProjectile proj : serverLevel.getEntitiesOfClass(AbstractHurtingProjectile.class,
                    getBoundingBox().inflate(96.0D), p -> p.getOwner() == this)) {
                    proj.discard();
                }
            }
            bossBar.setProgress(0.0F);
            bossBar.setVisible(false);

            ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp ? sp
                : (getKillCredit() instanceof ServerPlayer credited ? credited : null);
            if (killer != null) {
                awardDefeatAdvancements(killer);
            }
            if (level() instanceof ServerLevel serverLevel) {
                for (ServerPlayer participant : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(arena).inflate(ARENA_RADIUS + 16.0D), this::isValidTarget)) {
                    SimpleAdvancements.award(participant, "last_echo/defeat_echobound_dragon");
                    SimpleAdvancements.award(participant, "last_echo/post_credits_shopping");
                }
            }
        }
        super.die(source);
    }

    private void awardDefeatAdvancements(ServerPlayer player) {
        SimpleAdvancements.award(player, "last_echo/defeat_echobound_dragon");
        SimpleAdvancements.award(player, "last_echo/post_credits_shopping");
        ItemStack held = player.getMainHandItem();
        if (held.is(VeloriseEMC.ECHOBOUND_SWORD.get()) || held.is(VeloriseEMC.ECHOBOUND_PICKAXE.get())) {
            SimpleAdvancements.award(player, "last_echo/performance_review");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("Arena", arena.asLong());
        tag.putInt("SpawnedPhaseMask", spawnedPhaseMask);
        tag.putBoolean("Scaled", scaled);
        tag.putFloat("EncounterHealth", encounterHealth);
        tag.putFloat("EncounterMaxHealth", encounterMaxHealth);
        tag.putInt("LastObservedPhase", lastObservedPhase);
        tag.putBoolean("FinalRefrainUsed", finalRefrainUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arena = BlockPos.of(tag.getLong("Arena"));
        spawnedPhaseMask = tag.getInt("SpawnedPhaseMask");
        scaled = tag.getBoolean("Scaled");
        encounterHealth = tag.getFloat("EncounterHealth");
        encounterMaxHealth = tag.getFloat("EncounterMaxHealth");
        if (tag.contains("LastObservedPhase")) lastObservedPhase = tag.getInt("LastObservedPhase");
        finalRefrainUsed = tag.getBoolean("FinalRefrainUsed");
        if (scaled && encounterMaxHealth > 0.0F && encounterHealth > 0.0F) {
            if (getAttribute(Attributes.MAX_HEALTH) != null) {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHYSICAL_HEALTH_SEGMENT);
            }
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        } else {
            scaled = false;
        }
    }

    private record EnderRainStrike(double x, double y, double z, int releaseTick, boolean huge) {}
    private record MeteorImpact(Vec3 position, int detonateTick) {}
    private record DelayedBlast(Vec3 position, int detonateTick) {}
}
