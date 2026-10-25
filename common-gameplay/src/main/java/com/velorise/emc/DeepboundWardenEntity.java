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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
    private final ServerBossEvent bossBar = new ServerBossEvent(BOSS_NAME,
        BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    private static final double ARENA_RADIUS = 34.0D;
    private static final double FINAL_SCREAM_RADIUS = 21.0D;
    private static final int INACTIVITY_BEFORE_DESPAWN = 20 * 30;
    private static final int DIG_ANIMATION_DURATION = 100;
    private static final double MELEE_RANGE_SQR = 4.25D * 4.25D;
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
    private int inactiveTicks;
    private int despawnAnimationTicks;
    private boolean allowedToDespawn;
    private BlockPos arena = BlockPos.ZERO;
    private UUID engagedTargetId;
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
    }

    public void bindArena(BlockPos center) {
        arena = center.immutable();
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

    @Override
    protected void customServerAiStep() {
        // Suppress Vanilla Warden Brain (which otherwise fires duplicate untelegraphed sonic booms,
        // overrides navigation paths during skill charges, and triggers calm-state digging).
        // Navigation and look control are ticked directly by Mob.serverAiStep().
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
        updateInactivityDespawn();
        if (isRemoved() || allowedToDespawn || despawnAnimationTicks > 0) return;

        if (getPose() == Pose.DIGGING || getPose() == Pose.EMERGING) {
            setPose(Pose.STANDING);
        }

        enforceArena(serverLevel);
        updatePhase();
        updateBossBarPresentation();

        ServerPlayer target = findCombatTarget(serverLevel);
        if (target == null) {
            clearCombatState();
            return;
        }

        setTarget(target);
        setAggressive(true);
        increaseAngerAt(target, 150, false);
        getLookControl().setLookAt(target, 180.0F, 180.0F);

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
        if (phase <= lastPhase) return;

        lastPhase = phase;
        cancelSkill();
        aggroRoarTicks = Math.max(aggroRoarTicks, 46);
        skillCooldown = 45;
        if (level() instanceof ServerLevel serverLevel) {
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

    private ServerPlayer findCombatTarget(ServerLevel serverLevel) {
        if (engagedTargetId != null) {
            Player engaged = serverLevel.getPlayerByUUID(engagedTargetId);
            if (engaged instanceof ServerPlayer engagedServerPlayer && isValidArenaTarget(engagedServerPlayer)) {
                return engagedServerPlayer;
            }
            engagedTargetId = null;
        }

        Player nearest = serverLevel.getNearestPlayer(getX(), getY(), getZ(), 64.0D,
            entity -> entity instanceof ServerPlayer player && isValidArenaTarget(player));
        return nearest instanceof ServerPlayer nearestServerPlayer ? nearestServerPlayer : null;
    }

    private boolean isValidArenaTarget(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
            && insideArena(player.position(), ARENA_RADIUS);
    }

    private void beginAggroRoar(ServerLevel serverLevel, ServerPlayer target) {
        cancelSkill();
        aggroRoarTicks = 54;
        skillCooldown = 65;
        getNavigation().stop();
        setSprinting(false);
        setPose(Pose.ROARING);
        lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR,
            SoundSource.HOSTILE, 4.0F, 0.52F);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.HOSTILE, 3.0F, 0.62F);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
            getX(), getY() + 1.2D, getZ(), 80, 1.2D, 1.4D, 1.2D, 0.06D);
        target.displayClientMessage(
            Component.translatable("message.velorise_emc.deepbound_warden.detected"), true);
    }

    private void tickAggroRoar(ServerLevel serverLevel, ServerPlayer target) {
        getNavigation().stop();
        setSprinting(false);
        setPose(Pose.ROARING);
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        if (aggroRoarTicks % 9 == 0) {
            ringParticles(serverLevel, position().add(0.0D, 0.15D, 0.0D),
                1.5D + (54 - aggroRoarTicks) * 0.055D, 24, ParticleTypes.SCULK_SOUL);
        }
        if (aggroRoarTicks == 28) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 3.5F, 0.48F);
        }

        aggroRoarTicks--;
        if (aggroRoarTicks <= 0) {
            setPose(Pose.STANDING);
        }
    }

    private void tickCombatMovement(ServerLevel serverLevel, ServerPlayer target) {
        double distanceSqr = distanceToSqr(target);
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        if (distanceSqr <= MELEE_RANGE_SQR) {
            getNavigation().stop();
            setSprinting(false);
            if (meleeCooldown <= 0) {
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
                }
            }
            return;
        }

        setPose(Pose.STANDING);
        setSprinting(true);
        double speed = switch (phase) {
            case 2 -> 1.15D;
            case 3 -> 1.28D;
            case 4 -> 1.42D;
            default -> 1.05D;
        };
        getNavigation().moveTo(target, speed);

        // A low heartbeat cadence makes the chase readable without flooding audio.
        if (tickCount % (phase >= 3 ? 28 : 40) == 0) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 1.8F, phase >= 3 ? 0.65F : 0.80F);
        }
    }

    private void beginRandomSkill(ServerLevel serverLevel, ServerPlayer target) {
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

    private void beginSkill(ServerLevel serverLevel, ServerPlayer target, Skill skill) {
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
        getNavigation().stop();
        setSprinting(false);
        setPose(skill == Skill.SONIC_CONE || skill == Skill.FINAL_SCREAM
            ? Pose.ROARING : Pose.SNIFFING);
        lookAt(EntityAnchorArgument.Anchor.EYES, skillAim.add(0.0D, 1.0D, 0.0D));

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

        target.displayClientMessage(
            Component.translatable("warning.velorise_emc.deepbound_warden." + skill.name().toLowerCase(java.util.Locale.ROOT)), true);
        updateBossBarPresentation();
    }

    private void tickSkillCharge(ServerLevel serverLevel, ServerPlayer currentTarget) {
        getNavigation().stop();
        setSprinting(false);
        setPose(pendingSkill == Skill.SONIC_CONE || pendingSkill == Skill.FINAL_SCREAM
            ? Pose.ROARING : Pose.SNIFFING);

        Player lockedPlayer = skillTargetId == null ? null : serverLevel.getPlayerByUUID(skillTargetId);
        ServerPlayer lockedTarget = lockedPlayer instanceof ServerPlayer lockedServerPlayer ? lockedServerPlayer : null;
        if (lockedTarget != null && lockedTarget.isAlive()
            && (pendingSkill == Skill.ECHO_GRAB || pendingSkill == Skill.SILENT_STEP)) {
            skillAim = lockedTarget.position();
        }
        lookAt(EntityAnchorArgument.Anchor.EYES, skillAim.add(0.0D, 1.0D, 0.0D));
        renderSkillTelegraph(serverLevel);

        skillChargeTicks--;
        if (skillChargeTicks > 0) return;

        Skill executing = pendingSkill;
        pendingSkill = Skill.NONE;
        setPose(Pose.STANDING);
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
                    serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE, 3.5F, 0.38F + elapsed * 0.004F);
                }
            }
            default -> { }
        }
    }

    private void executeSkill(ServerLevel serverLevel, ServerPlayer currentTarget,
                              ServerPlayer lockedTarget, Skill skill) {
        switch (skill) {
            case SONIC_CONE -> sonicCone(serverLevel, skillAim);
            case RESONANCE_LINE -> resonanceLine(serverLevel, skillAim);
            case SCULK_ERUPTION -> sculkEruption(serverLevel, skillAim);
            case ECHO_GRAB -> {
                ServerPlayer target = lockedTarget != null ? lockedTarget : currentTarget;
                if (isValidArenaTarget(target)) echoGrab(serverLevel, target);
            }
            case SILENT_STEP -> {
                ServerPlayer target = lockedTarget != null ? lockedTarget : currentTarget;
                if (isValidArenaTarget(target)) silentStep(serverLevel, target);
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
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
            getBoundingBox().inflate(35.0D), this::isValidArenaTarget)) {
            Vec3 offset = player.position().subtract(position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() < 0.0001D || horizontal.lengthSqr() > 35.0D * 35.0D) continue;
            if (horizontal.normalize().dot(forward) < cosHalfAngle) continue;
            player.hurt(serverLevel.damageSources().sonicBoom(this), 28.0F);
            player.push(horizontal.x * 0.045D, 0.28D, horizontal.z * 0.045D);
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
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, bounds,
            this::isValidArenaTarget)) {
            if (distanceToSegmentXZ(player.position(), start, end) > 1.8D) continue;
            player.hurt(serverLevel.damageSources().mobAttack(this), 20.0F);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
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
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area,
            this::isValidArenaTarget)) {
            if (player.position().distanceToSqr(aim) <= 25.0D) {
                player.hurt(serverLevel.damageSources().mobAttack(this), 18.0F);
                player.push(0.0D, 0.55D, 0.0D);
            }
        }
    }

    private void echoGrab(ServerLevel serverLevel, ServerPlayer target) {
        Vec3 pull = position().subtract(target.position());
        double length = Math.max(1.0D, Math.sqrt(pull.x * pull.x + pull.z * pull.z));
        target.push(pull.x / length * 1.25D, 0.20D, pull.z / length * 1.25D);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.HOSTILE, 3.0F, 0.42F);
        particleLine(serverLevel, getEyePosition(), target.getEyePosition(),
            ParticleTypes.SCULK_SOUL, 28);
    }

    private void silentStep(ServerLevel serverLevel, ServerPlayer target) {
        Vec3 before = position();
        Vec3 safeDest = findSafeSilentStepDestination(serverLevel, target);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DIG,
            SoundSource.HOSTILE, 2.5F, 0.72F);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
            before.x, before.y + 1.0D, before.z, 80, 1.0D, 1.2D, 1.0D, 0.08D);
        teleportTo(safeDest.x, safeDest.y, safeDest.z);
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
            getX(), getY() + 1.0D, getZ(), 100, 1.2D, 1.2D, 1.2D, 0.12D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE,
            SoundSource.HOSTILE, 3.0F, 0.62F);
        meleeCooldown = Math.min(meleeCooldown, 7);
    }

    private Vec3 findSafeSilentStepDestination(ServerLevel level, ServerPlayer target) {
        Vec3 look = target.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 0.0001D) horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        horizontalLook = horizontalLook.normalize();
        Vec3 behind = horizontalLook.scale(-1.0D);

        double[] angleOffsetsDeg = {0.0D, 45.0D, -45.0D, 90.0D, -90.0D, 135.0D, -135.0D, 180.0D};
        double[] distances = {4.0D, 3.0D};
        for (double dist : distances) {
            for (double deg : angleOffsetsDeg) {
                Vec3 offset = rotateY(behind, Math.toRadians(deg)).scale(dist);
                double cx = target.getX() + offset.x;
                double cz = target.getZ() + offset.z;
                if (!insideArena(new Vec3(cx, target.getY(), cz), ARENA_RADIUS - 2.5D)) continue;

                int baseY = Mth.floor(target.getY());
                for (int dy = 1; dy >= -2; dy--) {
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
        return insideArena(target.position(), ARENA_RADIUS - 2.0D) ? target.position() : position();
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
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
            getBoundingBox().inflate(FINAL_SCREAM_RADIUS + 2.0D, 6.0D, FINAL_SCREAM_RADIUS + 2.0D), this::isValidArenaTarget)) {
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            if (dx * dx + dz * dz <= maxDistSqr) {
                player.hurt(serverLevel.damageSources().sonicBoom(this), 32.0F);
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
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

    private void clearCombatState() {
        engagedTargetId = null;
        setTarget(null);
        setAggressive(false);
        setSprinting(false);
        if (pendingSkill != Skill.NONE) cancelSkill();
        if (aggroRoarTicks <= 0 && getPose() != Pose.DIGGING) setPose(Pose.STANDING);
    }

    private void sendArenaWarning(ServerLevel serverLevel, Component message) {
        for (ServerPlayer player : serverLevel.players()) {
            if (isValidArenaTarget(player)) player.displayClientMessage(message, true);
        }
    }

    private void updateInactivityDespawn() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Player sensedPlayer = serverLevel.getNearestPlayer(getX(), getY(), getZ(), 48.0D,
            entity -> entity instanceof ServerPlayer player && isValidArenaTarget(player));
        ServerPlayer sensed = sensedPlayer instanceof ServerPlayer sensedServerPlayer ? sensedServerPlayer : null;
        boolean active = sensed != null || hurtTime > 0 || pendingSkill != Skill.NONE || aggroRoarTicks > 0;
        if (active) {
            inactiveTicks = 0;
            despawnAnimationTicks = 0;
            allowedToDespawn = false;
            return;
        }

        if (++inactiveTicks < INACTIVITY_BEFORE_DESPAWN) return;

        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        if (despawnAnimationTicks == 0) {
            cancelSkill();
            setPose(Pose.DIGGING);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DIG,
                SoundSource.HOSTILE, 3.0F, 0.7F);
        }

        despawnAnimationTicks++;
        setPose(Pose.DIGGING);
        if (despawnAnimationTicks >= DIG_ANIMATION_DURATION) {
            allowedToDespawn = true;
            discard();
        }
    }

    private void enforceArena(Level level) {
        if (arena.equals(BlockPos.ZERO)) return;
        if (!insideArena(position(), ARENA_RADIUS + 4.0D) || getY() < arena.getY() - 4.0D) {
            teleportTo(arena.getX() + 0.5D, arena.getY() + 1.0D, arena.getZ() + 0.5D);
            getNavigation().stop();
            setTarget(null);
            engagedTargetId = null;
            cancelSkill();
            return;
        }
        if (getTarget() instanceof Player player && !insideArena(player.position(), ARENA_RADIUS)) {
            setTarget(null);
            engagedTargetId = null;
            getNavigation().stop();
        }
        boolean playerInside = level.getNearestPlayer(arena.getX() + 0.5D, arena.getY() + 1.0D,
            arena.getZ() + 0.5D, ARENA_RADIUS,
            entity -> entity instanceof Player player
                && player.isAlive() && !player.isCreative() && !player.isSpectator()
                && insideArena(player.position(), ARENA_RADIUS)) != null;
        if (!playerInside && tickCount % 80 == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 5.0D + random.nextDouble() * 18.0D;
            getNavigation().moveTo(arena.getX() + 0.5D + Math.cos(angle) * radius,
                arena.getY() + 1.0D,
                arena.getZ() + 0.5D + Math.sin(angle) * radius,
                0.8D);
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
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel serverLevel)) return super.hurt(source, amount);
        boolean bypasses = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypasses && source.getEntity() instanceof Player player
            && !insideArena(player.position(), ARENA_RADIUS)) return false;

        ensureHealthInitialized(serverLevel);
        inactiveTicks = 0;
        despawnAnimationTicks = 0;
        allowedToDespawn = false;

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
        // Reject Vanilla's immediate calm-state discard. Only the controlled 30-second
        // inactivity sequence may remove a living Deepbound Warden.
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
        tag.putFloat("EncounterHealth", encounterHealth);
        tag.putFloat("EncounterMaxHealth", encounterMaxHealth);
        tag.putInt("LastPhase", lastPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
        inactiveTicks = tag.getInt("InactiveTicks");
        despawnAnimationTicks = tag.getInt("DespawnAnimationTicks");
        initializedHealth = tag.getBoolean("InitializedHealth");
        if (tag.contains("EncounterHealth")) encounterHealth = tag.getFloat("EncounterHealth");
        if (tag.contains("EncounterMaxHealth")) encounterMaxHealth = tag.getFloat("EncounterMaxHealth");
        if (tag.contains("LastPhase")) lastPhase = tag.getInt("LastPhase");
        if (initializedHealth && encounterMaxHealth > 0.0F && encounterHealth > 0.0F) {
            setHealth(PHYSICAL_HEALTH_SEGMENT);
        } else {
            initializedHealth = false;
        }
        allowedToDespawn = false;
        cancelSkill();
        engagedTargetId = null;
        aggroRoarTicks = 0;
    }

    @Override
    public void die(DamageSource source) {
        cancelSkill();
        setSprinting(false);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_DEATH,
                SoundSource.HOSTILE, 4.0F, 0.55F);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                getX(), getY() + 1.0D, getZ(), 240, 4.0D, 3.0D, 4.0D, 0.18D);
        }
        super.die(source);
    }
}
