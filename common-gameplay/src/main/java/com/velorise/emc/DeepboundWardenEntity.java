package com.velorise.emc;

import com.velorise.emc.common.GameplayConfig;
import com.velorise.emc.common.ThreatScalingProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Tier IV promotion boss: a stronger Warden built around sound, darkness and arena resonance. */
public final class DeepboundWardenEntity extends Warden {
    private static final Component BOSS_NAME = Component.translatable("entity.velorise_emc.deepbound_warden");
    private static final byte EVENT_MELEE_ATTACK = 4;
    private static final byte EVENT_TENDRIL_CLICKS = 61;
    private static final byte EVENT_SONIC_CHARGE = 62;

    private final ServerBossEvent bossBar = new ServerBossEvent(BOSS_NAME,
        BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    private static final double ARENA_RADIUS = 34.0D;
    private static final double FINAL_SCREAM_RADIUS = 21.0D;
    private static final int INACTIVITY_BEFORE_DESPAWN = 20 * 45;
    private static final int DIG_ANIMATION_DURATION = 100;
    private static final int EMERGE_ANIMATION_DURATION = 42;
    private static final double MELEE_RANGE_SQR = 4.35D * 4.35D;
    private static final float BASE_ENCOUNTER_HEALTH = 3_600.0F;
    /** Vanilla clamps MAX_HEALTH to 1024, so damage is resolved against a 1000-HP physical segment. */
    private static final float PHYSICAL_HEALTH_SEGMENT = 1_000.0F;

    private boolean initializedHealth;
    private float encounterHealth = BASE_ENCOUNTER_HEALTH;
    private float encounterMaxHealth = BASE_ENCOUNTER_HEALTH;
    private int phase = 1;
    private int lastPhase = 1;
    private int skillCooldown = 70;
    private int skillChargeTicks;
    private int skillChargeTotal;
    private int meleeCooldown;
    private int aggroRoarTicks;
    private int emergeTicks;
    private int sniffPoseTicks;
    private int inactiveTicks;
    private int despawnAnimationTicks;
    private boolean allowedToDespawn;
    private boolean hadCombatEngagement;
    private BlockPos arena = BlockPos.ZERO;
    private UUID engagedTargetId;
    private UUID retaliationTargetId;
    private int wanderTimer;
    private int lookAroundTimer;
    private float desiredLookYawOffset;
    private float desiredLookPitch;
    private int pathRecalcTimer;
    private Vec3 lastPathTargetPos = Vec3.ZERO;
    private int stuckCounter;
    private Vec3 lastProgressPos = Vec3.ZERO;
    private UUID skillTargetId;
    private Vec3 skillAim = Vec3.ZERO;
    private Skill pendingSkill = Skill.NONE;
    private Skill lastExecutedSkill = Skill.NONE;

    private enum Skill {
        NONE(""),
        SONIC_CONE("boss.velorise_emc.deepbound_warden.sonic_cone"),
        RESONANCE_LINE("boss.velorise_emc.deepbound_warden.resonance_line"),
        SCULK_ERUPTION("boss.velorise_emc.deepbound_warden.sculk_eruption"),
        ECHO_GRAB("boss.velorise_emc.deepbound_warden.echo_grab"),
        SILENT_STEP("boss.velorise_emc.deepbound_warden.silent_step"),
        FINAL_SCREAM("boss.velorise_emc.deepbound_warden.final_scream");

        private final String translationKey;

        Skill(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    public DeepboundWardenEntity(EntityType<? extends Warden> type, Level level) {
        super(type, level);
        setCustomName(BOSS_NAME);
        setCustomNameVisible(true);
        setPersistenceRequired();
        xpReward = 300;
        applyStepHeight(this, 1.25F);
    }

    private static void applyStepHeight(Entity entity, float step) {
        try {
            java.lang.reflect.Method m = Entity.class.getMethod("setMaxUpStep", float.class);
            m.invoke(entity, step);
            return;
        } catch (Throwable ignored) {}

        if (entity instanceof LivingEntity living) {
            try {
                java.lang.reflect.Field f = Attributes.class.getField("STEP_HEIGHT");
                Object attrObj = f.get(null);
                for (java.lang.reflect.Method m : LivingEntity.class.getMethods()) {
                    if (m.getName().equals("getAttribute") && m.getParameterCount() == 1) {
                        Object inst = m.invoke(living, attrObj);
                        if (inst != null) {
                            java.lang.reflect.Method setBase = inst.getClass().getMethod("setBaseValue", double.class);
                            setBase.invoke(inst, (double) step);
                        }
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public void bindArena(BlockPos center) {
        arena = center.immutable();
    }

    public BlockPos arenaCenter() {
        return arena;
    }

    /** Starts the authentic emerging-from-sculk animation when summoned. */
    public void beginSummonEmerge() {
        emergeTicks = EMERGE_ANIMATION_DURATION;
        setPose(Pose.EMERGING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Warden.createAttributes()
            .add(Attributes.MAX_HEALTH, 3600.0D)
            .add(Attributes.ATTACK_DAMAGE, 34.0D)
            .add(Attributes.ARMOR, 22.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.34D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 72.0D);
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) { return false; }

    @Override
    protected void customServerAiStep() {
        // Suppress Vanilla Warden Brain (which otherwise fires duplicate untelegraphed sonic booms,
        // overrides navigation paths during skill charges, and triggers calm-state digging).
        // Navigation, MoveControl, JumpControl and LookControl are ticked directly by Mob.serverAiStep().
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        ensureHealthInitialized(serverLevel);
        updateInactivityDespawn(serverLevel);
        if (isRemoved() || allowedToDespawn || despawnAnimationTicks > 0) return;

        enforceArena(serverLevel);
        updatePhase();
        updateBossBarPresentation();

        if (emergeTicks > 0) {
            tickSummonEmerge(serverLevel);
            return;
        }

        if (sniffPoseTicks > 0) {
            sniffPoseTicks--;
            if (sniffPoseTicks == 0 && getPose() == Pose.SNIFFING && pendingSkill == Skill.NONE) {
                setPose(Pose.STANDING);
            }
        }

        if (getPose() == Pose.DIGGING || getPose() == Pose.EMERGING) {
            setPose(Pose.STANDING);
        }

        LivingEntity target = findCombatTarget(serverLevel);
        if (target == null) {
            clearCombatState(serverLevel);
            wanderArena(serverLevel);
            return;
        }

        hadCombatEngagement = true;
        inactiveTicks = 0;
        setTarget(target);
        setAggressive(true);
        increaseAngerAt(target, 150, false);

        if (engagedTargetId == null || !engagedTargetId.equals(target.getUUID())) {
            engagedTargetId = target.getUUID();
            beginAggroRoar(serverLevel, target);
        }

        if (meleeCooldown > 0) meleeCooldown--;

        if (aggroRoarTicks > 0) {
            tickAggroRoar(serverLevel, target);
            return;
        }

        if (pendingSkill != Skill.NONE) {
            tickSkillCharge(serverLevel, target);
            return;
        }

        tickCombatMovement(serverLevel, target);

        if (skillCooldown > 0) {
            skillCooldown--;
        } else if (distanceToSqr(target) > MELEE_RANGE_SQR || phase >= 2) {
            beginRandomSkill(serverLevel, target);
        }
    }

    private void tickSummonEmerge(ServerLevel serverLevel) {
        getNavigation().stop();
        setSprinting(false);
        setPose(Pose.EMERGING);
        if (emergeTicks % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                getX(), getY() + 0.5D, getZ(), 10, 0.7D, 0.4D, 0.7D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                getX(), getY() + 0.2D, getZ(), 8, 0.6D, 0.2D, 0.6D, 0.03D);
        }
        if (emergeTicks == 20) {
            serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_TENDRIL_CLICKS,
                SoundSource.HOSTILE, 2.5F, 0.75F);
        }
        emergeTicks--;
        if (emergeTicks <= 0) {
            setPose(Pose.STANDING);
        }
    }

    private void ensureHealthInitialized(ServerLevel serverLevel) {
        if (!initializedHealth) {
            float bonus = 0.0F;
            GameplayConfig.BossPlayerScaling cfg = GameplayConfig.current().world.bossPlayerScaling;
            if (cfg.enabled && cfg.warden && serverLevel.getServer() != null) {
                int online = 0;
                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    if (!player.isCreative() && !player.isSpectator()) online++;
                }
                int counted = ThreatScalingProfile.clampPlayerCount(online);
                if (counted > 0) {
                    bonus = (float) ThreatScalingProfile.bossBonusHealth(counted);
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

    private float healthRatio() {
        return encounterMaxHealth <= 0.0F ? 0.0F : Mth.clamp(encounterHealth / encounterMaxHealth, 0.0F, 1.0F);
    }

    private void updatePhase() {
        float ratio = healthRatio();
        phase = ratio > 0.75F ? 1
            : ratio > 0.50F ? 2
            : ratio > 0.25F ? 3 : 4;
        if (phase < lastPhase) {
            lastPhase = phase;
            return;
        }
        if (phase == lastPhase) return;

        lastPhase = phase;
        cancelSkill();
        setPose(Pose.STANDING);
        setPose(Pose.ROARING);
        aggroRoarTicks = Math.max(aggroRoarTicks, 46);
        skillCooldown = 45;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 3.8F, 0.45F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 2.2F, 0.65F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 3.0F, 0.50F);
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                getX(), getY() + 1.5D, getZ(), 3, 0.5D, 0.5D, 0.5D, 0.0D);
            sendArenaWarning(serverLevel,
                Component.translatable("message.velorise_emc.deepbound_warden.phase", phase));
        }
    }

    private void updateBossBarPresentation() {
        bossBar.setProgress(healthRatio());
        bossBar.setColor(switch (phase) {
            case 2 -> BossEvent.BossBarColor.PURPLE;
            case 3 -> BossEvent.BossBarColor.RED;
            case 4 -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.BLUE;
        });
        if (pendingSkill == Skill.NONE) {
            bossBar.setName(BOSS_NAME);
        } else {
            bossBar.setName(BOSS_NAME.copy()
                .append(Component.literal(" — "))
                .append(Component.translatable(pendingSkill.translationKey)));
        }
    }

    private LivingEntity findCombatTarget(ServerLevel serverLevel) {
        if (engagedTargetId != null) {
            Entity engaged = serverLevel.getEntity(engagedTargetId);
            if (engaged instanceof LivingEntity living && isValidCombatTarget(living)) {
                return living;
            }
            engagedTargetId = null;
        }

        if (retaliationTargetId != null) {
            Entity ent = serverLevel.getEntity(retaliationTargetId);
            if (ent instanceof LivingEntity le && isValidCombatTarget(le)) {
                return le;
            }
            retaliationTargetId = null;
        }

        Player nearest = serverLevel.getNearestPlayer(getX(), getY(), getZ(), 64.0D,
            entity -> entity instanceof ServerPlayer player && isValidArenaTarget(player));
        if (nearest instanceof ServerPlayer nearestServerPlayer) {
            return nearestServerPlayer;
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
        return insideArena(entity.position(), ARENA_RADIUS + 6.0D);
    }

    private void wanderArena(ServerLevel level) {
        setAggressive(false);
        setSprinting(false);
        if (sniffPoseTicks <= 0 && getPose() != Pose.ROARING && getPose() != Pose.EMERGING && getPose() != Pose.DIGGING) {
            setPose(Pose.STANDING);
        }

        if (--wanderTimer <= 0 || getNavigation().isDone()) {
            wanderTimer = 80 + random.nextInt(70);
            Vec3 origin = arena.equals(BlockPos.ZERO) ? position() : Vec3.atBottomCenterOf(arena);
            double maxRadius = arena.equals(BlockPos.ZERO) ? 14.0D : 18.0D;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 4.0D + random.nextDouble() * maxRadius;
            double targetX = origin.x + Math.cos(angle) * radius;
            double targetZ = origin.z + Math.sin(angle) * radius;
            Vec3 snapped = snapToWalkableFloor(level, new Vec3(targetX, getY(), targetZ));

            getNavigation().moveTo(snapped.x, snapped.y, snapped.z, 0.72D);

            if (random.nextFloat() < 0.38F) {
                setPose(Pose.STANDING);
                setPose(Pose.SNIFFING);
                sniffPoseTicks = 48;
                level.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
                level.playSound(null, blockPosition(), SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 2.0F, 0.85F);
                level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 2.0D, getZ(), 10, 0.5D, 0.5D, 0.5D, 0.02D);
            }
        }

        // Natural head looking and ear/tendril awareness while patrolling
        if (--lookAroundTimer <= 0) {
            lookAroundTimer = 30 + random.nextInt(40);
            desiredLookYawOffset = (random.nextFloat() - 0.5F) * 110.0F;
            desiredLookPitch = (random.nextFloat() - 0.5F) * 24.0F;
            if (random.nextFloat() < 0.28F) {
                level.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
                level.playSound(null, blockPosition(), SoundEvents.WARDEN_LISTENING, SoundSource.HOSTILE, 1.4F, 0.85F + random.nextFloat() * 0.2F);
            }
        }
        Vec3 forward = Vec3.directionFromRotation(desiredLookPitch, yBodyRot + desiredLookYawOffset);
        getLookControl().setLookAt(getX() + forward.x * 6.0D, getEyeY() + forward.y * 3.0D, getZ() + forward.z * 6.0D, 22.0F, 20.0F);
    }

    private void beginAggroRoar(ServerLevel serverLevel, LivingEntity target) {
        cancelSkill();
        sniffPoseTicks = 0;
        aggroRoarTicks = 54;
        skillCooldown = 55;
        getNavigation().stop();
        setSprinting(false);
        setPose(Pose.STANDING);
        setPose(Pose.ROARING);
        smoothFaceTarget(target, 45.0F, 40.0F);
        serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR,
            SoundSource.HOSTILE, 4.0F, 0.52F);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.HOSTILE, 3.0F, 0.62F);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
            getX(), getY() + 1.2D, getZ(), 80, 1.2D, 1.4D, 1.2D, 0.06D);
        if (target instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                Component.translatable("message.velorise_emc.deepbound_warden.detected"), true);
        }
    }

    private void tickAggroRoar(ServerLevel serverLevel, LivingEntity target) {
        getNavigation().stop();
        setSprinting(false);
        if (getPose() != Pose.ROARING) {
            setPose(Pose.ROARING);
        }
        smoothFaceTarget(target, 35.0F, 30.0F);

        if (aggroRoarTicks % 9 == 0) {
            ringParticles(serverLevel, position().add(0.0D, 0.15D, 0.0D),
                1.5D + (54 - aggroRoarTicks) * 0.055D, 24, ParticleTypes.SCULK_SOUL);
        }
        if (aggroRoarTicks == 28) {
            serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 3.5F, 0.48F);
        }

        aggroRoarTicks--;
        if (aggroRoarTicks <= 0) {
            setPose(Pose.STANDING);
        }
    }

    private void smoothFaceTarget(LivingEntity target, float maxYawTurn, float maxPitchTurn) {
        Vec3 eye = target.getEyePosition();
        getLookControl().setLookAt(eye.x, eye.y, eye.z, maxYawTurn, maxPitchTurn);
        double dx = eye.x - getX();
        double dz = eye.z - getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.32F, getYRot(), desiredYaw));
            yBodyRot = Mth.rotLerp(0.26F, yBodyRot, getYRot());
            yHeadRot = Mth.rotLerp(0.38F, yHeadRot, desiredYaw);
        }
    }

    private void smoothFacePosition(Vec3 pos, float maxYawTurn, float maxPitchTurn) {
        getLookControl().setLookAt(pos.x, pos.y, pos.z, maxYawTurn, maxPitchTurn);
        double dx = pos.x - getX();
        double dz = pos.z - getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.32F, getYRot(), desiredYaw));
            yBodyRot = Mth.rotLerp(0.26F, yBodyRot, getYRot());
            yHeadRot = Mth.rotLerp(0.38F, yHeadRot, desiredYaw);
        }
    }

    private void tickCombatMovement(ServerLevel serverLevel, LivingEntity target) {
        double distanceSqr = distanceToSqr(target);
        smoothFaceTarget(target, 38.0F, 35.0F);

        if (distanceSqr <= MELEE_RANGE_SQR) {
            getNavigation().stop();
            setSprinting(false);
            pathRecalcTimer = 0;
            if (meleeCooldown <= 0) {
                // Trigger Vanilla Warden two-handed slam animation on all clients!
                serverLevel.broadcastEntityEvent(this, EVENT_MELEE_ATTACK);
                swing(InteractionHand.MAIN_HAND);
                serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT,
                    SoundSource.HOSTILE, 2.8F, 0.85F + random.nextFloat() * 0.15F);
                boolean hit = super.doHurtTarget(target);
                meleeCooldown = switch (phase) {
                    case 2 -> 22;
                    case 3 -> 19;
                    case 4 -> 16;
                    default -> 26;
                };
                if (hit) {
                    serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        target.getX(), target.getY() + 1.0D, target.getZ(),
                        18, 0.45D, 0.75D, 0.45D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                        target.getX(), target.getY() + 1.0D, target.getZ(),
                        12, 0.35D, 0.55D, 0.35D, 0.04D);
                }
            }
            return;
        }

        if (getPose() != Pose.STANDING) {
            setPose(Pose.STANDING);
        }
        setSprinting(true);
        double speed = switch (phase) {
            case 2 -> 1.15D;
            case 3 -> 1.28D;
            case 4 -> 1.42D;
            default -> 1.05D;
        };

        // Throttle path recalculation so the Warden strides smoothly without resetting its path every tick
        if (--pathRecalcTimer <= 0
            || getNavigation().isDone()
            || target.position().distanceToSqr(lastPathTargetPos) > 2.25D) {
            lastPathTargetPos = target.position();
            pathRecalcTimer = 6 + random.nextInt(5);
            if (!getNavigation().moveTo(target, speed)) {
                getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
            }
        }

        // Intelligent anti-stuck & step assist on uneven arena stairs/pillars
        if (tickCount % 10 == 0) {
            if (lastProgressPos != Vec3.ZERO && position().distanceToSqr(lastProgressPos) < 0.08D && distanceSqr > MELEE_RANGE_SQR * 1.5D) {
                stuckCounter++;
                if (horizontalCollision && onGround()) {
                    getJumpControl().jump();
                }
                if (stuckCounter >= 3 && skillCooldown > 10) {
                    stuckCounter = 0;
                    beginSkill(serverLevel, target, Skill.SILENT_STEP);
                    return;
                }
            } else {
                stuckCounter = 0;
            }
            lastProgressPos = position();
        }

        // Low heartbeat + tendril twitch cadence makes the chase feel alive without flooding audio.
        if (tickCount % (phase >= 3 ? 28 : 40) == 0) {
            serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 1.8F, phase >= 3 ? 0.65F : 0.80F);
        }
    }

    private void beginRandomSkill(ServerLevel serverLevel, LivingEntity target) {
        double distSqr = distanceToSqr(target);
        double dy = target.getY() - getY();

        // Tactical intelligence: choose context-appropriate skills if player is high up or far away
        if (dy > 3.5D && distSqr > 9.0D && random.nextFloat() < 0.65F) {
            Skill antiAir = (phase >= 3 && random.nextBoolean()) ? Skill.ECHO_GRAB : Skill.SONIC_CONE;
            if (antiAir != lastExecutedSkill) {
                beginSkill(serverLevel, target, antiAir);
                return;
            }
        }
        if (distSqr > 16.0D * 16.0D && phase >= 2 && random.nextFloat() < 0.50F) {
            Skill gapCloser = (phase >= 3 && random.nextBoolean()) ? Skill.SILENT_STEP : Skill.ECHO_GRAB;
            if (gapCloser != lastExecutedSkill) {
                beginSkill(serverLevel, target, gapCloser);
                return;
            }
        }

        int poolSize = phase + 2;
        int choice = random.nextInt(poolSize);
        Skill skill = skillFromChoice(choice);
        if (skill == lastExecutedSkill && poolSize > 1) {
            skill = skillFromChoice((choice + 1 + random.nextInt(poolSize - 1)) % poolSize);
        }
        if (skill == Skill.FINAL_SCREAM && phase < 4) skill = Skill.SONIC_CONE;
        beginSkill(serverLevel, target, skill);
    }

    private Skill skillFromChoice(int choice) {
        return switch (choice) {
            case 0 -> Skill.SONIC_CONE;
            case 1 -> Skill.RESONANCE_LINE;
            case 2 -> Skill.SCULK_ERUPTION;
            case 3 -> Skill.ECHO_GRAB;
            case 4 -> Skill.SILENT_STEP;
            default -> Skill.FINAL_SCREAM;
        };
    }

    private void beginSkill(ServerLevel serverLevel, LivingEntity target, Skill skill) {
        pendingSkill = skill;
        lastExecutedSkill = skill;
        skillTargetId = target.getUUID();
        skillAim = target.position();
        skillChargeTotal = switch (skill) {
            case SONIC_CONE -> 36;
            case RESONANCE_LINE -> 32;
            case SCULK_ERUPTION -> 42;
            case ECHO_GRAB -> 28;
            case SILENT_STEP -> 24;
            case FINAL_SCREAM -> 54;
            default -> 0;
        };
        skillChargeTicks = skillChargeTotal;
        sniffPoseTicks = 0;
        getNavigation().stop();
        setSprinting(false);

        // Reset pose to STANDING first so client OnSyncedDataUpdated cleanly restarts the pose animation
        setPose(Pose.STANDING);
        if (skill == Skill.SONIC_CONE || skill == Skill.FINAL_SCREAM) {
            setPose(Pose.ROARING);
            serverLevel.broadcastEntityEvent(this, EVENT_SONIC_CHARGE);
        } else {
            setPose(Pose.SNIFFING);
            serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
        }
        smoothFacePosition(skillAim.add(0.0D, 1.0D, 0.0D), 45.0F, 40.0F);

        switch (skill) {
            case SONIC_CONE -> serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 3.2F, 0.58F);
            case RESONANCE_LINE -> serverLevel.playSound(null, blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.HOSTILE, 2.6F, 0.72F);
            case SCULK_ERUPTION -> serverLevel.playSound(null, BlockPos.containing(skillAim.x, skillAim.y, skillAim.z), SoundEvents.SCULK_CATALYST_BLOOM,
                SoundSource.HOSTILE, 2.4F, 0.55F);
            case ECHO_GRAB -> serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 3.0F, 0.42F);
            case SILENT_STEP -> serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DIG,
                SoundSource.HOSTILE, 2.6F, 0.72F);
            case FINAL_SCREAM -> serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 4.0F, 0.36F);
            default -> { }
        }

        if (target instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                Component.translatable("warning.velorise_emc.deepbound_warden." + skill.name().toLowerCase(java.util.Locale.ROOT)), true);
        }
        updateBossBarPresentation();
    }

    private void tickSkillCharge(ServerLevel serverLevel, LivingEntity currentTarget) {
        getNavigation().stop();
        setSprinting(false);
        Pose desiredPose = (pendingSkill == Skill.SONIC_CONE || pendingSkill == Skill.FINAL_SCREAM)
            ? Pose.ROARING : Pose.SNIFFING;
        if (getPose() != desiredPose) {
            setPose(desiredPose);
        }

        Entity lockedEntity = skillTargetId == null ? null : serverLevel.getEntity(skillTargetId);
        LivingEntity lockedTarget = lockedEntity instanceof LivingEntity le && isValidCombatTarget(le) ? le : null;
        if (lockedTarget != null && lockedTarget.isAlive()) {
            if (pendingSkill == Skill.ECHO_GRAB || pendingSkill == Skill.SILENT_STEP) {
                skillAim = lockedTarget.position();
            } else if ((pendingSkill == Skill.SONIC_CONE || pendingSkill == Skill.RESONANCE_LINE) && skillChargeTicks > 10) {
                // Smoothly track moving target during the first part of the charge, locking aim for the final 10 ticks
                skillAim = skillAim.lerp(lockedTarget.position(), 0.28D);
            }
        }
        smoothFacePosition(skillAim.add(0.0D, 1.0D, 0.0D), 35.0F, 30.0F);
        renderSkillTelegraph(serverLevel);

        skillChargeTicks--;
        if (skillChargeTicks > 0) return;

        Skill executing = pendingSkill;
        pendingSkill = Skill.NONE;
        setPose(Pose.STANDING);
        serverLevel.broadcastEntityEvent(this, EVENT_MELEE_ATTACK);
        swing(InteractionHand.MAIN_HAND);
        executeSkill(serverLevel, currentTarget, lockedTarget, executing);
        skillTargetId = null;
        skillCooldown = switch (phase) {
            case 2 -> 78;
            case 3 -> 60;
            case 4 -> 44;
            default -> 100;
        };
        updateBossBarPresentation();
    }

    private void renderSkillTelegraph(ServerLevel serverLevel) {
        int elapsed = skillChargeTotal - skillChargeTicks;
        Vec3 origin = getEyePosition();
        switch (pendingSkill) {
            case SONIC_CONE -> {
                if (elapsed % 3 == 0) {
                    Vec3 direction = horizontalDirection(origin, skillAim);
                    particleRay(serverLevel, origin, direction, 28.0D, ParticleTypes.SCULK_SOUL, 18);
                    particleRay(serverLevel, origin, rotateY(direction, Math.toRadians(36.0D)),
                        26.0D, ParticleTypes.SCULK_SOUL, 14);
                    particleRay(serverLevel, origin, rotateY(direction, Math.toRadians(-36.0D)),
                        26.0D, ParticleTypes.SCULK_SOUL, 14);
                }
            }
            case RESONANCE_LINE -> {
                if (elapsed % 2 == 0) {
                    particleLine(serverLevel, position().add(0.0D, 0.18D, 0.0D),
                        skillAim.add(0.0D, 0.18D, 0.0D), ParticleTypes.SCULK_SOUL, 24);
                }
            }
            case SCULK_ERUPTION -> {
                double radius = 1.5D + 3.5D * Math.min(1.0D, elapsed / (double) skillChargeTotal);
                ringParticles(serverLevel, skillAim.add(0.0D, 0.12D, 0.0D), radius, 28,
                    elapsed % 4 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SCULK_SOUL);
            }
            case ECHO_GRAB -> {
                if (elapsed % 2 == 0) {
                    particleLine(serverLevel, getEyePosition(), skillAim.add(0.0D, 1.0D, 0.0D),
                        ParticleTypes.SCULK_SOUL, 20);
                }
                if (elapsed % 10 == 0) {
                    serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
                    serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE, 2.4F, 0.48F + elapsed * 0.005F);
                }
            }
            case SILENT_STEP -> {
                if (elapsed % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        getX(), getY() + 1.0D, getZ(), 10, 0.8D, 1.0D, 0.8D, 0.02D);
                    serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        skillAim.x, skillAim.y + 0.8D, skillAim.z, 8, 1.0D, 0.8D, 1.0D, 0.02D);
                }
            }
            case FINAL_SCREAM -> {
                double radius = 2.0D + (FINAL_SCREAM_RADIUS - 2.0D) * (elapsed / (double) Math.max(1, skillChargeTotal));
                if (elapsed % 3 == 0) {
                    ringParticles(serverLevel, position().add(0.0D, 0.15D, 0.0D), radius,
                        32, ParticleTypes.SCULK_SOUL);
                    ringParticles(serverLevel, position().add(0.0D, 0.15D, 0.0D), FINAL_SCREAM_RADIUS,
                        36, ParticleTypes.SOUL_FIRE_FLAME);
                }
                if (elapsed % 14 == 0) {
                    serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
                    serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE, 3.5F, 0.38F + elapsed * 0.004F);
                }
            }
            default -> { }
        }
    }

    private void executeSkill(ServerLevel serverLevel, LivingEntity currentTarget,
                              LivingEntity lockedTarget, Skill skill) {
        switch (skill) {
            case SONIC_CONE -> sonicCone(serverLevel, skillAim);
            case RESONANCE_LINE -> resonanceLine(serverLevel, skillAim);
            case SCULK_ERUPTION -> sculkEruption(serverLevel, skillAim);
            case ECHO_GRAB -> {
                LivingEntity target = lockedTarget != null ? lockedTarget : currentTarget;
                if (isValidCombatTarget(target)) echoGrab(serverLevel, target);
            }
            case SILENT_STEP -> {
                LivingEntity target = lockedTarget != null ? lockedTarget : currentTarget;
                if (isValidCombatTarget(target)) silentStep(serverLevel, target);
            }
            case FINAL_SCREAM -> finalScream(serverLevel);
            default -> { }
        }
    }

    private void sonicCone(ServerLevel serverLevel, Vec3 aim) {
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
            SoundSource.HOSTILE, 4.0F, 0.68F);
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
            getX(), getY() + 2.0D, getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);

        Vec3 origin = getEyePosition();
        Vec3 forward = horizontalDirection(origin, aim);
        double cosHalfAngle = Math.cos(Math.toRadians(36.0D));
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(35.0D), this::isValidCombatTarget)) {
            Vec3 offset = entity.position().subtract(position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() < 0.0001D || horizontal.lengthSqr() > 35.0D * 35.0D) continue;
            if (horizontal.normalize().dot(forward) < cosHalfAngle) continue;
            entity.hurt(serverLevel.damageSources().sonicBoom(this), 28.0F);
            entity.push(horizontal.x * 0.045D, 0.28D, horizontal.z * 0.045D);
        }

        particleRay(serverLevel, origin, forward, 35.0D, ParticleTypes.SONIC_BOOM, 9);
    }

    private void resonanceLine(ServerLevel serverLevel, Vec3 aim) {
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT,
            SoundSource.HOSTILE, 3.0F, 0.62F);
        Vec3 start = position();
        Vec3 end = aim;
        particleLine(serverLevel, start.add(0.0D, 0.2D, 0.0D),
            end.add(0.0D, 0.2D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 42);

        AABB bounds = new AABB(
            Math.min(start.x, end.x), Math.min(start.y, end.y) - 2.0D, Math.min(start.z, end.z),
            Math.max(start.x, end.x), Math.max(start.y, end.y) + 4.0D, Math.max(start.z, end.z)
        ).inflate(2.0D);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, bounds,
            this::isValidCombatTarget)) {
            if (distanceToSegmentXZ(entity.position(), start, end) > 1.8D) continue;
            entity.hurt(serverLevel.damageSources().mobAttack(this), 20.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
    }

    private void sculkEruption(ServerLevel serverLevel, Vec3 aim) {
        BlockPos impact = BlockPos.containing(aim.x, aim.y, aim.z);
        serverLevel.playSound(null, impact, SoundEvents.SCULK_CATALYST_BLOOM,
            SoundSource.HOSTILE, 3.0F, 0.42F);
        // The coordinate overload is compatible with both 1.20.1 SoundEvent fields
        // and 1.21.1 holder-backed sound constants.
        serverLevel.playSound(null, aim.x, aim.y, aim.z, SoundEvents.GENERIC_EXPLODE,
            SoundSource.HOSTILE, 2.2F, 0.65F);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            aim.x, aim.y + 0.2D, aim.z, 120, 4.0D, 0.8D, 4.0D, 0.05D);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
            aim.x, aim.y + 0.8D, aim.z, 80, 4.5D, 1.2D, 4.5D, 0.08D);
        AABB area = new AABB(aim.x - 5.0D, aim.y - 2.0D, aim.z - 5.0D,
            aim.x + 5.0D, aim.y + 4.0D, aim.z + 5.0D);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
            this::isValidCombatTarget)) {
            if (entity.position().distanceToSqr(aim) <= 25.0D) {
                entity.hurt(serverLevel.damageSources().mobAttack(this), 18.0F);
                entity.push(0.0D, 0.55D, 0.0D);
            }
        }
    }

    private void echoGrab(ServerLevel serverLevel, LivingEntity target) {
        Vec3 pull = position().subtract(target.position());
        double length = Math.max(1.0D, Math.sqrt(pull.x * pull.x + pull.z * pull.z));
        target.push(pull.x / length * 1.25D, 0.20D, pull.z / length * 1.25D);
        target.hasImpulse = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.HOSTILE, 3.0F, 0.42F);
        particleLine(serverLevel, getEyePosition(), target.getEyePosition(),
            ParticleTypes.SCULK_SOUL, 28);
    }

    private void silentStep(ServerLevel serverLevel, LivingEntity target) {
        Vec3 before = position();
        Vec3 safeDest = findSafeSilentStepDestination(serverLevel, target);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DIG,
            SoundSource.HOSTILE, 2.5F, 0.72F);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
            before.x, before.y + 1.0D, before.z, 80, 1.0D, 1.2D, 1.0D, 0.08D);
        teleportTo(safeDest.x, safeDest.y, safeDest.z);
        smoothFaceTarget(target, 180.0F, 90.0F);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
            getX(), getY() + 1.0D, getZ(), 100, 1.2D, 1.2D, 1.2D, 0.12D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE,
            SoundSource.HOSTILE, 3.0F, 0.62F);
        meleeCooldown = Math.min(meleeCooldown, 7);
        pathRecalcTimer = 0;
    }

    private Vec3 findSafeSilentStepDestination(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 0.0001D) horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        horizontalLook = horizontalLook.normalize();
        Vec3 behind = horizontalLook.scale(-1.0D);

        double[] angleOffsetsDeg = {0.0D, 45.0D, -45.0D, 90.0D, -90.0D, 135.0D, -135.0D, 180.0D};
        double[] distances = {4.0D, 3.0D, 5.5D};
        for (double dist : distances) {
            for (double deg : angleOffsetsDeg) {
                Vec3 offset = rotateY(behind, Math.toRadians(deg)).scale(dist);
                double cx = target.getX() + offset.x;
                double cz = target.getZ() + offset.z;
                if (!insideArena(new Vec3(cx, target.getY(), cz), ARENA_RADIUS - 2.5D)) continue;

                int baseY = Mth.floor(target.getY());
                for (int dy = 2; dy >= -4; dy--) {
                    BlockPos feet = BlockPos.containing(cx, baseY + dy, cz);
                    if (level.getBlockState(feet.below()).isAir()) continue;
                    Vec3 candidate = new Vec3(cx, feet.getY(), cz);
                    AABB movedBox = getBoundingBox().move(candidate.subtract(position()));
                    if (level.noCollision(this, movedBox)) {
                        return candidate;
                    }
                }
            }
        }
        Vec3 snappedTarget = snapToWalkableFloor(level, target.position());
        return insideArena(snappedTarget, ARENA_RADIUS - 2.0D) ? snappedTarget : position();
    }

    public Vec3 snapToWalkableFloor(ServerLevel level, Vec3 candidate) {
        int bx = Mth.floor(candidate.x);
        int bz = Mth.floor(candidate.z);
        int startY = Mth.floor(candidate.y);
        int minY = arena.equals(BlockPos.ZERO) ? level.getMinBuildHeight() + 1 : Math.max(level.getMinBuildHeight() + 1, arena.getY() - 6);
        int maxY = startY + 8;
        BlockPos pos = new BlockPos(bx, startY, bz);
        while (pos.getY() > minY && level.getBlockState(pos.below()).isAir()) {
            pos = pos.below();
        }
        while (pos.getY() < maxY && !level.getBlockState(pos).isAir()) {
            pos = pos.above();
        }
        return new Vec3(bx + 0.5D, pos.getY(), bz + 0.5D);
    }

    private void finalScream(ServerLevel serverLevel) {
        if (phase < 4) return;
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR,
            SoundSource.HOSTILE, 5.0F, 0.40F);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
            SoundSource.HOSTILE, 4.0F, 0.52F);
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
            getX(), getY() + 2.0D, getZ(), 10, 7.0D, 2.0D, 7.0D, 0.0D);
        for (int radius = 4; radius <= (int) FINAL_SCREAM_RADIUS; radius += 4) {
            ringParticles(serverLevel, position().add(0.0D, 0.2D, 0.0D),
                radius, 40, ParticleTypes.SCULK_SOUL);
        }
        double maxDistSqr = FINAL_SCREAM_RADIUS * FINAL_SCREAM_RADIUS;
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(FINAL_SCREAM_RADIUS + 2.0D, 6.0D, FINAL_SCREAM_RADIUS + 2.0D), this::isValidCombatTarget)) {
            double dx = entity.getX() - getX();
            double dz = entity.getZ() - getZ();
            if (dx * dx + dz * dz <= maxDistSqr) {
                entity.hurt(serverLevel.damageSources().sonicBoom(this), 32.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
            }
        }
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        if (direction.lengthSqr() < 0.0001D) return new Vec3(0.0D, 0.0D, 1.0D);
        return direction.normalize();
    }

    private static Vec3 rotateY(Vec3 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos - vector.z * sin, 0.0D,
            vector.x * sin + vector.z * cos);
    }

    private static void particleRay(ServerLevel serverLevel, Vec3 origin, Vec3 direction,
                                    double length, ParticleOptions particle, int steps) {
        for (int step = 1; step <= steps; step++) {
            double distance = length * step / steps;
            Vec3 point = origin.add(direction.scale(distance));
            serverLevel.sendParticles(particle, point.x, point.y, point.z,
                1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    private static void particleLine(ServerLevel serverLevel, Vec3 from, Vec3 to,
                                     ParticleOptions particle, int steps) {
        Vec3 delta = to.subtract(from);
        for (int step = 0; step <= steps; step++) {
            Vec3 point = from.add(delta.scale(step / (double) steps));
            serverLevel.sendParticles(particle, point.x, point.y, point.z,
                1, 0.04D, 0.04D, 0.04D, 0.0D);
        }
    }

    private static void ringParticles(ServerLevel serverLevel, Vec3 center, double radius,
                                      int points, ParticleOptions particle) {
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points;
            serverLevel.sendParticles(particle,
                center.x + Math.cos(angle) * radius,
                center.y,
                center.z + Math.sin(angle) * radius,
                1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static double distanceToSegmentXZ(Vec3 point, Vec3 start, Vec3 end) {
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double lengthSqr = dx * dx + dz * dz;
        if (lengthSqr < 0.0001D) {
            double px = point.x - start.x;
            double pz = point.z - start.z;
            return Math.sqrt(px * px + pz * pz);
        }
        double t = ((point.x - start.x) * dx + (point.z - start.z) * dz) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double nearestX = start.x + dx * t;
        double nearestZ = start.z + dz * t;
        double px = point.x - nearestX;
        double pz = point.z - nearestZ;
        return Math.sqrt(px * px + pz * pz);
    }

    private void cancelSkill() {
        pendingSkill = Skill.NONE;
        skillChargeTicks = 0;
        skillChargeTotal = 0;
        skillTargetId = null;
        skillAim = Vec3.ZERO;
        bossBar.setName(BOSS_NAME);
    }

    private void clearCombatState(ServerLevel serverLevel) {
        engagedTargetId = null;
        retaliationTargetId = null;
        setTarget(null);
        setAggressive(false);
        setSprinting(false);
        if (pendingSkill != Skill.NONE) cancelSkill();
        if (aggroRoarTicks > 0) aggroRoarTicks = 0;
        if (emergeTicks <= 0 && sniffPoseTicks <= 0 && getPose() != Pose.DIGGING) {
            setPose(Pose.STANDING);
        }
        // Gracefully regenerate and reset phase if all challengers died or left the arena for 10 seconds
        if (hadCombatEngagement && inactiveTicks >= 200 && encounterHealth < encounterMaxHealth) {
            encounterHealth = Math.min(encounterMaxHealth, encounterHealth + encounterMaxHealth * 0.025F);
            if (encounterHealth >= encounterMaxHealth) {
                hadCombatEngagement = false;
                phase = 1;
                lastPhase = 1;
            }
            updateBossBarPresentation();
        }
    }

    private void sendArenaWarning(ServerLevel serverLevel, Component message) {
        for (ServerPlayer player : serverLevel.players()) {
            if (isValidArenaTarget(player)) player.displayClientMessage(message, true);
        }
    }

    private void updateInactivityDespawn(ServerLevel serverLevel) {
        LivingEntity sensedTarget = findCombatTarget(serverLevel);
        boolean active = sensedTarget != null || hurtTime > 0 || pendingSkill != Skill.NONE || aggroRoarTicks > 0 || emergeTicks > 0;
        if (active) {
            inactiveTicks = 0;
            despawnAnimationTicks = 0;
            allowedToDespawn = false;
            return;
        }

        inactiveTicks++;
        // Arena-bound bosses remain in their arena (healing up after a wipe) so returning players can resume or restart cleanly.
        if (!arena.equals(BlockPos.ZERO)) {
            despawnAnimationTicks = 0;
            allowedToDespawn = false;
            return;
        }

        if (inactiveTicks < INACTIVITY_BEFORE_DESPAWN) return;

        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        if (despawnAnimationTicks == 0) {
            cancelSkill();
            setPose(Pose.STANDING);
            setPose(Pose.DIGGING);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DIG,
                SoundSource.HOSTILE, 3.0F, 0.7F);
        }

        despawnAnimationTicks++;
        if (getPose() != Pose.DIGGING) {
            setPose(Pose.DIGGING);
        }
        if (despawnAnimationTicks % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                getX(), getY() + 0.3D, getZ(), 6, 0.5D, 0.2D, 0.5D, 0.02D);
        }
        if (despawnAnimationTicks >= DIG_ANIMATION_DURATION) {
            allowedToDespawn = true;
            discard();
        }
    }

    private void enforceArena(ServerLevel level) {
        if (arena.equals(BlockPos.ZERO)) {
            // If summoned without a bound arena, check if a registered Deepbound arena is nearby and bind to it
            BlockPos nearestArena = BossArenaRegistryData.get(level.getServer())
                .nearest(BossArenaRegistryData.Kind.DEEPBOUND, blockPosition());
            if (nearestArena != null && nearestArena.distSqr(blockPosition()) <= (ARENA_RADIUS + 16.0D) * (ARENA_RADIUS + 16.0D)) {
                arena = nearestArena.immutable();
            } else {
                return;
            }
        }
        if (!insideArena(position(), ARENA_RADIUS + 4.0D) || getY() < arena.getY() - 5.0D || getY() > arena.getY() + 26.0D) {
            Vec3 safeHome = findSafeArenaHome(level);
            teleportTo(safeHome.x, safeHome.y, safeHome.z);
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            setTarget(null);
            engagedTargetId = null;
            aggroRoarTicks = 0;
            cancelSkill();
            setPose(Pose.STANDING);
            return;
        }
        if (getTarget() != null && !isValidCombatTarget(getTarget())) {
            setTarget(null);
            engagedTargetId = null;
            getNavigation().stop();
        }
    }

    /** Finds a clear, walkable floor spot near the central altar without clipping into the Sculk Shrieker. */
    public Vec3 findSafeArenaHome(ServerLevel level) {
        if (arena.equals(BlockPos.ZERO)) {
            return snapToWalkableFloor(level, position());
        }
        int[][] offsets = {{0, 3}, {3, 0}, {-3, 0}, {0, -3}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}, {0, 0}};
        for (int[] off : offsets) {
            Vec3 candidate = snapToWalkableFloor(level, new Vec3(arena.getX() + 0.5D + off[0], arena.getY() + 2.0D, arena.getZ() + 0.5D + off[1]));
            AABB box = getBoundingBox().move(candidate.subtract(position()));
            if (level.noCollision(this, box)) {
                return candidate;
            }
        }
        return new Vec3(arena.getX() + 0.5D, arena.getY() + 2.0D, arena.getZ() + 3.5D);
    }

    private boolean insideArena(Vec3 position, double radius) {
        if (arena.equals(BlockPos.ZERO)) return true;
        double dx = position.x - (arena.getX() + 0.5D);
        double dz = position.z - (arena.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel serverLevel)) return super.hurt(source, amount);
        boolean bypasses = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypasses && source.getEntity() instanceof LivingEntity attacker
            && !insideArena(attacker.position(), ARENA_RADIUS + 4.0D)) return false;

        ensureHealthInitialized(serverLevel);
        inactiveTicks = 0;
        despawnAnimationTicks = 0;
        allowedToDespawn = false;
        hadCombatEngagement = true;

        Entity directAttacker = source.getEntity();
        if (directAttacker instanceof LivingEntity attacker && attacker != this && isValidCombatTarget(attacker)) {
            retaliationTargetId = attacker.getUUID();
            if (engagedTargetId == null) {
                engagedTargetId = attacker.getUUID();
            }
            setTarget(attacker);
            setLastHurtByMob(attacker);
            increaseAngerAt(attacker, 150, false);
        }

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
            serverLevel.broadcastEntityEvent(this, EVENT_TENDRIL_CLICKS);
            level().playSound(null, blockPosition(), SoundEvents.WARDEN_HURT,
                SoundSource.HOSTILE, 2.5F, 0.75F);
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
    public void remove(Entity.RemovalReason reason) {
        // Reject Vanilla's immediate calm-state discard. Only the controlled inactivity
        // sequence may remove a living Deepbound Warden.
        if (reason == Entity.RemovalReason.DISCARDED && isAlive() && !allowedToDespawn) {
            setPose(Pose.STANDING);
            return;
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("Arena", arena.asLong());
        tag.putInt("InactiveTicks", inactiveTicks);
        tag.putInt("DespawnAnimationTicks", despawnAnimationTicks);
        tag.putBoolean("InitializedHealth", initializedHealth);
        tag.putBoolean("HadCombatEngagement", hadCombatEngagement);
        tag.putFloat("EncounterHealth", encounterHealth);
        tag.putFloat("EncounterMaxHealth", encounterMaxHealth);
        tag.putInt("LastPhase", lastPhase);
        if (retaliationTargetId != null) tag.putUUID("RetaliationTargetId", retaliationTargetId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
        inactiveTicks = tag.getInt("InactiveTicks");
        despawnAnimationTicks = tag.getInt("DespawnAnimationTicks");
        initializedHealth = tag.getBoolean("InitializedHealth");
        hadCombatEngagement = tag.getBoolean("HadCombatEngagement");
        if (tag.contains("EncounterHealth")) encounterHealth = tag.getFloat("EncounterHealth");
        if (tag.contains("EncounterMaxHealth")) encounterMaxHealth = tag.getFloat("EncounterMaxHealth");
        if (tag.contains("LastPhase")) lastPhase = tag.getInt("LastPhase");
        if (tag.hasUUID("RetaliationTargetId")) retaliationTargetId = tag.getUUID("RetaliationTargetId");
        if (initializedHealth && encounterMaxHealth > 0.0F && encounterHealth > 0.0F) {
            if (getAttribute(Attributes.MAX_HEALTH) != null) {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHYSICAL_HEALTH_SEGMENT);
            }
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        } else {
            initializedHealth = false;
        }
        allowedToDespawn = false;
        cancelSkill();
        engagedTargetId = null;
        aggroRoarTicks = 0;
        emergeTicks = 0;
        sniffPoseTicks = 0;
        setPose(Pose.STANDING);
    }

    @Override
    public void die(DamageSource source) {
        cancelSkill();
        setSprinting(false);
        setPose(Pose.DYING);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DEATH,
                SoundSource.HOSTILE, 4.0F, 0.55F);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                getX(), getY() + 1.0D, getZ(), 240, 4.0D, 3.0D, 4.0D, 0.18D);
        }
        super.die(source);
    }
}
