package com.velorise.emc;

import com.velorise.emc.common.ArcaneWeaponProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/** Server-side impact resolution for reusable Core weapons. */
@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArcaneWeaponEvents {
    public static final String TAG_KIND = "VeloriseEMCArcaneKind";
    public static final String TAG_TIER = "VeloriseEMCArcaneTier";
    public static final String TAG_DAMAGE_MULTIPLIER = "VeloriseEMCArcaneDamageMultiplier";
    private static final String TAG_ORIGIN_X = "VeloriseEMCArcaneOriginX";
    private static final String TAG_ORIGIN_Y = "VeloriseEMCArcaneOriginY";
    private static final String TAG_ORIGIN_Z = "VeloriseEMCArcaneOriginZ";
    private static final String TAG_MAX_RANGE = "VeloriseEMCArcaneMaxRange";

    private ArcaneWeaponEvents() {}

    public static void markRange(Projectile projectile, Vec3 origin, double maxRange) {
        projectile.getPersistentData().putDouble(TAG_ORIGIN_X, origin.x);
        projectile.getPersistentData().putDouble(TAG_ORIGIN_Y, origin.y);
        projectile.getPersistentData().putDouble(TAG_ORIGIN_Z, origin.z);
        projectile.getPersistentData().putDouble(TAG_MAX_RANGE, Math.max(1.0D, maxRange));
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) return;
        String kind = projectile.getPersistentData().getString(TAG_KIND);
        if (kind.isEmpty()) return;

        Vec3 hit = event.getRayTraceResult().getLocation();
        if (isBeyondRange(projectile, hit)) {
            event.setCanceled(true);
            level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 8, 0.12D, 0.12D, 0.12D, 0.01D);
            projectile.discard();
            return;
        }

        int tier = Math.max(1, projectile.getPersistentData().getInt(TAG_TIER));
        float multiplier = projectile.getPersistentData().contains(TAG_DAMAGE_MULTIPLIER)
            ? Math.max(1.0F, projectile.getPersistentData().getFloat(TAG_DAMAGE_MULTIPLIER)) : 1.0F;
        Entity owner = projectile.getOwner();

        // Ghastfire deliberately remains uncancelled. The custom handler applies the
        // configured Core-tier AoE, while vanilla LargeFireball collision supplies a real
        // explosion for both entity hits and block hits instead of silently disappearing.
        if ("ghastfire".equals(kind)) {
            resolveGhastfire(level, hit, owner, tier, multiplier);
            return;
        }

        event.setCanceled(true);
        switch (kind) {
            case "dragonfire" -> resolveDragonfire(level, hit, owner, tier, multiplier);
            case "inferno" -> resolveInferno(level, hit, owner, tier, multiplier, event);
            case "wither" -> resolveWither(level, hit, owner, tier, multiplier);
            case "shulker" -> resolveShulker(level, hit, owner, tier, multiplier, event);
            case "frost" -> resolveFrost(level, hit, owner, tier, multiplier, event);
            case "web" -> resolveWeb(level, hit, owner, tier, event);
            default -> { }
        }
        projectile.discard();
    }

    private static void resolveDragonfire(ServerLevel level, Vec3 hit, Entity owner, int tier, float multiplier) {
        if (owner instanceof ServerPlayer player) {
            DragonfireCloudManager.spawn(level, hit, player, tier, multiplier);
        }
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE,
            SoundSource.PLAYERS, 0.9F, 1.15F);
    }

    private static void resolveInferno(ServerLevel level, Vec3 hit, Entity owner, int tier, float multiplier,
                                       ProjectileImpactEvent event) {
        LivingEntity target = hitEntity(event);
        if (target != null && target != owner) {
            // Ignite first so Vanilla entity loot tables select cooked meat even on a lethal direct hit.
            if (owner instanceof ServerPlayer player) ArcaneAchievementTracker.markHit(player, target, "inferno", tier);
            target.igniteForSeconds(ArcaneWeaponProfile.infernoFireSeconds());
            target.hurt(level.damageSources().magic(),
                ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.INFERNO, tier) * multiplier);
            if (target instanceof net.minecraft.world.entity.monster.Blaze && owner instanceof ServerPlayer player) {
                ArcaneMilestoneTracker.award(player, "engineering/inferno_rod", ArcaneMilestoneTracker.INFERNO);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, 30, 0.35D, 0.35D, 0.35D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIRECHARGE_USE,
            SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void resolveGhastfire(ServerLevel level, Vec3 hit, Entity owner, int tier, float multiplier) {
        double radius = ArcaneWeaponProfile.ghastfireRadius(tier);
        float damage = ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.GHASTFIRE, tier) * multiplier;
        java.util.List<LivingEntity> affected = targets(level, hit, radius, owner);
        for (LivingEntity target : affected) {
            if (owner instanceof ServerPlayer player) ArcaneAchievementTracker.markHit(player, target, "ghastfire", tier);
            target.igniteForSeconds(ArcaneWeaponProfile.ghastfireFireSeconds());
            target.hurt(level.damageSources().magic(), damage);
        }
        if (owner instanceof ServerPlayer player) ArcaneAchievementTracker.markGhastfireArea(player, affected.size());
        // Vanilla LargeFireball now performs the actual impact explosion. Keep only the
        // Core-themed flame halo here to avoid duplicate explosion sounds and emitters.
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, 45,
            radius * 0.35D, radius * 0.25D, radius * 0.35D, 0.05D);
    }

    private static void resolveWither(ServerLevel level, Vec3 hit, Entity owner, int tier, float multiplier) {
        double radius = ArcaneWeaponProfile.witherRadius(tier);
        float damage = ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.WITHER, tier) * multiplier;
        int duration = ArcaneWeaponProfile.witherSeconds(tier) * 20;
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            if (owner instanceof ServerPlayer player) ArcaneAchievementTracker.markHit(player, target, "wither", tier);
            target.hurt(level.damageSources().magic(), damage);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, tier >= 4 ? 1 : 0));
            if (target instanceof net.minecraft.world.entity.monster.WitherSkeleton && owner instanceof ServerPlayer player) {
                ArcaneMilestoneTracker.award(player, "engineering/wither_scepter", ArcaneMilestoneTracker.WITHER);
            }
        }
        level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 36,
            radius * 0.4D, radius * 0.35D, radius * 0.4D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.WITHER_BREAK_BLOCK,
            SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void resolveShulker(ServerLevel level, Vec3 hit, Entity owner, int tier, float multiplier,
                                       ProjectileImpactEvent event) {
        LivingEntity target = hitEntity(event);
        if (target != null && target != owner) {
            if (owner instanceof ServerPlayer player) ArcaneAchievementTracker.markHit(player, target, "shulker", tier);
            target.hurt(level.damageSources().magic(),
                ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.SHULKER, tier) * multiplier);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                ArcaneWeaponProfile.shulkerLevitationSeconds(tier) * 20, 0));
            if (target instanceof net.minecraft.world.entity.monster.Shulker && owner instanceof ServerPlayer player) {
                ArcaneMilestoneTracker.award(player, "engineering/shulker_launcher", ArcaneMilestoneTracker.SHULKER);
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 18, 0.25D, 0.25D, 0.25D, 0.03D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SHULKER_BULLET_HIT,
            SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void resolveFrost(ServerLevel level, Vec3 hit, Entity owner, int tier, float multiplier,
                                     ProjectileImpactEvent event) {
        LivingEntity target = hitEntity(event);
        if (target != null && target != owner) {
            if (owner instanceof ServerPlayer player) ArcaneAchievementTracker.markHit(player, target, "frost", tier);
            target.hurt(level.damageSources().freeze(),
                ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.FROST, tier) * multiplier);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                ArcaneWeaponProfile.frostSlowSeconds(tier) * 20, 1));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 120 + tier * 20));
            if (target instanceof net.minecraft.world.entity.monster.Blaze && owner instanceof ServerPlayer player) {
                ArcaneMilestoneTracker.award(player, "engineering/frostbolt_wand", ArcaneMilestoneTracker.FROST);
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, hit.x, hit.y, hit.z, 24, 0.3D, 0.3D, 0.3D, 0.025D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GLASS_BREAK,
            SoundSource.PLAYERS, 0.65F, 1.6F);
    }

    private static void resolveWeb(ServerLevel level, Vec3 hit, Entity owner, int tier, ProjectileImpactEvent event) {
        BlockPos pos;
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit) {
            pos = entityHit.getEntity().blockPosition();
            if (entityHit.getEntity() instanceof LivingEntity target && owner instanceof ServerPlayer player) {
                ArcaneAchievementTracker.markHit(player, target, "web", tier);
                if (target instanceof net.minecraft.world.entity.monster.Spider) {
                    ArcaneMilestoneTracker.award(player, "engineering/webcaster", ArcaneMilestoneTracker.WEB);
                }
            }
        } else if (event.getRayTraceResult() instanceof BlockHitResult blockHit) {
            pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            pos = BlockPos.containing(hit);
        }
        int webTicks = ArcaneWeaponProfile.webDurationTicks(tier);
        int venomTicks = ArcaneWeaponProfile.webVenomDurationTicks(tier);
        int poisonAmplifier = ArcaneWeaponProfile.webPoisonAmplifier(tier);
        java.util.UUID webOwner = owner instanceof ServerPlayer player ? player.getUUID() : null;
        if (!TemporaryWebManager.place(level, pos, webTicks, venomTicks, poisonAmplifier, webOwner)) {
            TemporaryWebManager.place(level, pos.above(), webTicks, venomTicks, poisonAmplifier, webOwner);
        }
        level.sendParticles(ParticleTypes.POOF, hit.x, hit.y, hit.z, 16, 0.25D, 0.25D, 0.25D, 0.02D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SLIME_SQUISH,
            SoundSource.PLAYERS, 0.7F, 1.5F);
    }

    public static void resolveLightning(ServerLevel level, Vec3 hit, ServerPlayer owner, int tier, net.minecraft.world.entity.LightningBolt bolt) {
        double radius = ArcaneWeaponProfile.lightningRadius(tier);
        float damage = ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.LIGHTNING, tier)
            * ResonantSetBonus.arcaneMultiplier(owner, ArcaneWeaponProfile.Kind.LIGHTNING);
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            ArcaneAchievementTracker.markHit(owner, target, "lightning", tier);
            if (target instanceof net.minecraft.world.entity.monster.Creeper creeper && bolt != null && !creeper.isPowered()) {
                creeper.thunderHit(level, bolt);
                ArcaneAchievementTracker.markChargedCreeper(owner);
            }
            target.igniteForSeconds(4);
            target.hurt(level.damageSources().lightningBolt(), damage);
            if (target instanceof net.minecraft.world.entity.monster.Drowned) {
                ArcaneMilestoneTracker.award(owner, "engineering/stormcaller", ArcaneMilestoneTracker.STORMCALLER);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y + 0.5D, hit.z, 35,
            radius * 0.35D, 0.8D, radius * 0.35D, 0.08D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static LivingEntity hitEntity(ProjectileImpactEvent event) {
        return event.getRayTraceResult() instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static java.util.List<LivingEntity> targets(ServerLevel level, Vec3 hit, double radius, Entity owner) {
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(hit, hit).inflate(radius),
            target -> target.isAlive() && target != owner && target.position().distanceToSqr(hit) <= radiusSqr);
    }

    private static boolean isBeyondRange(Projectile projectile, Vec3 hit) {
        double maxRange = projectile.getPersistentData().getDouble(TAG_MAX_RANGE);
        if (maxRange <= 0.0D) return false;
        Vec3 origin = new Vec3(
            projectile.getPersistentData().getDouble(TAG_ORIGIN_X),
            projectile.getPersistentData().getDouble(TAG_ORIGIN_Y),
            projectile.getPersistentData().getDouble(TAG_ORIGIN_Z));
        return origin.distanceToSqr(hit) > maxRange * maxRange;
    }
}
