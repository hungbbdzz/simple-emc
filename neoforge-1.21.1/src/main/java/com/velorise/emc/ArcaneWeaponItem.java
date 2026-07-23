package com.velorise.emc;

import com.velorise.emc.common.ArcaneWeaponProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Reusable Core-powered weapons created by combining a Vanilla catalyst with a Wireless Core. */
public final class ArcaneWeaponItem extends Item {
    private static final String TAG_INFERNO_SHOTS = "VeloriseEMCInfernoBurstShots";
    private static final String TAG_INFERNO_LAST = "VeloriseEMCInfernoBurstLast";
    private static final String TAG_WEB_SHOTS = "VeloriseEMCWebBurstShots";
    private static final String TAG_WEB_LAST = "VeloriseEMCWebBurstLast";
    private static final String TAG_WITHER_SHOTS = "VeloriseEMCWitherBurstShots";
    private static final String TAG_WITHER_LAST = "VeloriseEMCWitherBurstLast";
    private static final String TAG_LIGHTNING_SHOTS = "VeloriseEMCLightningBurstShots";
    private static final String TAG_LIGHTNING_LAST = "VeloriseEMCLightningBurstLast";
    private static final String TAG_FROST_SHOTS = "VeloriseEMCFrostBurstShots";
    private static final String TAG_FROST_LAST = "VeloriseEMCFrostBurstLast";

    public enum Kind {
        DRAGONFIRE, ECHO, INFERNO, GHASTFIRE,
        WITHER, SHULKER, GUARDIAN, EVOKER, LIGHTNING, FROST, WEB
    }

    private final Kind kind;

    public ArcaneWeaponItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        if (isCharged()) {
            player.startUsingItem(hand);
            if (!level.isClientSide()) {
                level.playSound(null, player.blockPosition(),
                    kind == Kind.ECHO ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.GUARDIAN_AMBIENT,
                    SoundSource.PLAYERS, 0.8F, kind == Kind.ECHO ? 0.85F : 1.25F);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        int tier = CoreTierData.get(stack).level();
        if (kind == Kind.INFERNO && !prepareInfernoBurst(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.WEB && !prepareWebBurst(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.WITHER && !prepareWitherBurst(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.LIGHTNING && !prepareLightningBurst(serverPlayer, tier)) {
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.FROST && !prepareFrostBurst(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        long cost = ArcaneWeaponProfile.emcCost(profileKind(), tier);
        if (!EmcTransactionService.spend(serverPlayer, cost)) {
            EmcFailureMessages.show(serverPlayer, cost);
            return InteractionResultHolder.fail(stack);
        }

        boolean activated = switch (kind) {
            case DRAGONFIRE -> shootDragonfire(serverPlayer, tier);
            case INFERNO -> shootInferno(serverPlayer, tier);
            case GHASTFIRE -> shootGhastfire(serverPlayer, tier);
            case WITHER -> shootWither(serverPlayer, tier);
            case SHULKER -> shootShulker(serverPlayer, tier);
            case EVOKER -> castFangs(serverPlayer, tier);
            case LIGHTNING -> castLightning(serverPlayer, tier);
            case FROST -> shootSnowball(serverPlayer, tier, "frost", 1.45F);
            case WEB -> shootSnowball(serverPlayer, tier, "web", 1.35F);
            case ECHO, GUARDIAN -> false;
        };
        if (!activated) {
            EmcAccountUtil.add(serverPlayer, cost);
            return InteractionResultHolder.fail(stack);
        }

        if (kind == Kind.INFERNO) advanceInfernoBurst(serverPlayer);
        else if (kind == Kind.WEB) advanceWebBurst(serverPlayer);
        else if (kind == Kind.WITHER) advanceWitherBurst(serverPlayer, tier);
        else if (kind == Kind.LIGHTNING) advanceLightningBurst(serverPlayer, tier);
        else if (kind == Kind.FROST) advanceFrostBurst(serverPlayer, tier);
        else player.getCooldowns().addCooldown(this, ArcaneWeaponProfile.cooldownTicks(profileKind()));
        player.awardStat(Stats.ITEM_USED.get(this));
        player.swing(hand, true);
        return InteractionResultHolder.consume(stack);
    }

    private boolean isCharged() {
        return kind == Kind.ECHO || kind == Kind.GUARDIAN;
    }

    private ArcaneWeaponProfile.Kind profileKind() {
        return ArcaneWeaponProfile.Kind.valueOf(kind.name());
    }

    private boolean prepareInfernoBurst(ServerPlayer player) {
        int shots = player.getPersistentData().getInt(TAG_INFERNO_SHOTS);
        if (shots <= 0) return true;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(TAG_INFERNO_LAST);
        if (elapsed <= ArcaneWeaponProfile.infernoBurstResetTicks()) return true;

        // Abandoning a partial burst cannot be used to avoid the recovery window.
        clearInfernoBurst(player);
        player.getCooldowns().addCooldown(this,
            ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.INFERNO));
        return false;
    }

    private void advanceInfernoBurst(ServerPlayer player) {
        int shots = player.getPersistentData().getInt(TAG_INFERNO_SHOTS) + 1;
        player.getPersistentData().putLong(TAG_INFERNO_LAST, player.level().getGameTime());
        if (shots >= ArcaneWeaponProfile.infernoBurstShots()) {
            clearInfernoBurst(player);
            player.getCooldowns().addCooldown(this,
                ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.INFERNO));
        } else {
            player.getPersistentData().putInt(TAG_INFERNO_SHOTS, shots);
            player.getCooldowns().addCooldown(this, 4);
        }
    }

    private static void clearInfernoBurst(ServerPlayer player) {
        player.getPersistentData().remove(TAG_INFERNO_SHOTS);
        player.getPersistentData().remove(TAG_INFERNO_LAST);
    }

    private boolean prepareWebBurst(ServerPlayer player) {
        int shots = player.getPersistentData().getInt(TAG_WEB_SHOTS);
        if (shots <= 0) return true;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(TAG_WEB_LAST);
        if (elapsed <= ArcaneWeaponProfile.webBurstResetTicks()) return true;

        clearWebBurst(player);
        player.getCooldowns().addCooldown(this,
            ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.WEB));
        return false;
    }

    private void advanceWebBurst(ServerPlayer player) {
        int shots = player.getPersistentData().getInt(TAG_WEB_SHOTS) + 1;
        player.getPersistentData().putLong(TAG_WEB_LAST, player.level().getGameTime());
        if (shots >= ArcaneWeaponProfile.webBurstShots()) {
            clearWebBurst(player);
            player.getCooldowns().addCooldown(this,
                ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.WEB));
        } else {
            player.getPersistentData().putInt(TAG_WEB_SHOTS, shots);
            player.getCooldowns().addCooldown(this, 4);
        }
    }

    private static void clearWebBurst(ServerPlayer player) {
        player.getPersistentData().remove(TAG_WEB_SHOTS);
        player.getPersistentData().remove(TAG_WEB_LAST);
    }

    private boolean prepareWitherBurst(ServerPlayer player) {
        int shots = player.getPersistentData().getInt(TAG_WITHER_SHOTS);
        if (shots <= 0) return true;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(TAG_WITHER_LAST);
        if (elapsed <= ArcaneWeaponProfile.witherBurstResetTicks()) return true;

        clearWitherBurst(player);
        player.getCooldowns().addCooldown(this,
            ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.WITHER));
        return false;
    }

    private void advanceWitherBurst(ServerPlayer player, int tier) {
        int shots = player.getPersistentData().getInt(TAG_WITHER_SHOTS) + 1;
        player.getPersistentData().putLong(TAG_WITHER_LAST, player.level().getGameTime());
        if (shots >= ArcaneWeaponProfile.witherBurstShots()) {
            ArcaneAchievementTracker.markWitherBurstComplete(player, tier,
                ArcaneWeaponProfile.witherSkullCount(tier), shots);
            clearWitherBurst(player);
            player.getCooldowns().addCooldown(this,
                ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.WITHER));
        } else {
            player.getPersistentData().putInt(TAG_WITHER_SHOTS, shots);
            player.getCooldowns().addCooldown(this, 4);
        }
    }

    private static void clearWitherBurst(ServerPlayer player) {
        player.getPersistentData().remove(TAG_WITHER_SHOTS);
        player.getPersistentData().remove(TAG_WITHER_LAST);
    }

    private boolean prepareLightningBurst(ServerPlayer player, int tier) {
        int shots = player.getPersistentData().getInt(TAG_LIGHTNING_SHOTS);
        if (shots <= 0) return true;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(TAG_LIGHTNING_LAST);
        if (elapsed <= ArcaneWeaponProfile.lightningBurstResetTicks()) return true;

        clearLightningBurst(player);
        player.getCooldowns().addCooldown(this,
            ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.LIGHTNING));
        return false;
    }

    private void advanceLightningBurst(ServerPlayer player, int tier) {
        int shots = player.getPersistentData().getInt(TAG_LIGHTNING_SHOTS) + 1;
        player.getPersistentData().putLong(TAG_LIGHTNING_LAST, player.level().getGameTime());
        if (shots >= ArcaneWeaponProfile.lightningBurstUses(tier)) {
            ArcaneAchievementTracker.markLightningBurstComplete(player, tier, shots);
            clearLightningBurst(player);
            player.getCooldowns().addCooldown(this,
                ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.LIGHTNING));
        } else {
            player.getPersistentData().putInt(TAG_LIGHTNING_SHOTS, shots);
            player.getCooldowns().addCooldown(this, 4);
        }
    }

    private static void clearLightningBurst(ServerPlayer player) {
        player.getPersistentData().remove(TAG_LIGHTNING_SHOTS);
        player.getPersistentData().remove(TAG_LIGHTNING_LAST);
    }

    private boolean prepareFrostBurst(ServerPlayer player) {
        int shots = player.getPersistentData().getInt(TAG_FROST_SHOTS);
        if (shots <= 0) return true;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(TAG_FROST_LAST);
        if (elapsed <= ArcaneWeaponProfile.frostBurstResetTicks()) return true;

        clearFrostBurst(player);
        player.getCooldowns().addCooldown(this,
            ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.FROST));
        return false;
    }

    private void advanceFrostBurst(ServerPlayer player, int tier) {
        int shots = player.getPersistentData().getInt(TAG_FROST_SHOTS) + 1;
        player.getPersistentData().putLong(TAG_FROST_LAST, player.level().getGameTime());
        if (shots >= ArcaneWeaponProfile.frostBurstUses(tier)) {
            clearFrostBurst(player);
            player.getCooldowns().addCooldown(this,
                ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.FROST));
        } else {
            player.getPersistentData().putInt(TAG_FROST_SHOTS, shots);
            player.getCooldowns().addCooldown(this, 4);
        }
    }

    private static void clearFrostBurst(ServerPlayer player) {
        player.getPersistentData().remove(TAG_FROST_SHOTS);
        player.getPersistentData().remove(TAG_FROST_LAST);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (kind == Kind.GUARDIAN) return ArcaneWeaponProfile.guardianBeamDurationTicks(CoreTierData.get(stack).level());
        return isCharged() ? ArcaneWeaponProfile.chargeTicks(profileKind()) : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isCharged() ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!isCharged() || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (kind == Kind.GUARDIAN) {
            int tier = CoreTierData.get(stack).level();
            int elapsed = ArcaneWeaponProfile.guardianBeamDurationTicks(tier) - count;
            if (elapsed <= 0 || elapsed % 5 != 0 || !(entity instanceof ServerPlayer player)) return;
            long cost = ArcaneWeaponProfile.guardianEmcPerPulse(tier);
            if (!EmcTransactionService.spend(player, cost)) {
                EmcFailureMessages.show(player, cost);
                player.stopUsingItem();
                return;
            }
            LivingEntity target = findTarget(player,
                ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.GUARDIAN, tier), 1.1D);
            fireGuardian(player, target, tier);
            return;
        }
        if (count % 5 != 0) return;
        Vec3 eye = entity.getEyePosition();
        serverLevel.sendParticles(kind == Kind.ECHO ? ParticleTypes.SCULK_CHARGE_POP : ParticleTypes.BUBBLE_POP,
            eye.x, eye.y, eye.z, 2, 0.18D, 0.18D, 0.18D, 0.02D);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!isCharged() || level.isClientSide() || !(entity instanceof ServerPlayer player)) return stack;
        if (kind == Kind.GUARDIAN) {
            finishGuardianBeam(player);
            return stack;
        }
        int tier = CoreTierData.get(stack).level();
        long cost = ArcaneWeaponProfile.emcCost(profileKind(), tier);
        if (!EmcTransactionService.spend(player, cost)) {
            EmcFailureMessages.show(player, cost);
            return stack;
        }
        fireEcho(player, tier);
        player.getCooldowns().addCooldown(this, ArcaneWeaponProfile.cooldownTicks(profileKind()));
        player.awardStat(Stats.ITEM_USED.get(this));
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (kind == Kind.GUARDIAN && !level.isClientSide() && entity instanceof ServerPlayer player
            && timeLeft < ArcaneWeaponProfile.guardianBeamDurationTicks(CoreTierData.get(stack).level())) {
            finishGuardianBeam(player);
        }
    }

    private void finishGuardianBeam(ServerPlayer player) {
        player.getCooldowns().addCooldown(this,
            ArcaneWeaponProfile.cooldownTicks(ArcaneWeaponProfile.Kind.GUARDIAN));
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static boolean shootDragonfire(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        DragonFireball projectile = EntityType.DRAGON_FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, tier, "dragonfire",
            ArcaneWeaponProfile.dragonfireRange(tier), 1.35F,
            ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.DRAGONFIRE));
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private static boolean shootGhastfire(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        LargeFireball projectile = EntityType.FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, tier, "ghastfire",
            ArcaneWeaponProfile.ghastfireRange(tier), 1.15F,
            ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.GHASTFIRE));
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private static boolean shootInferno(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        SmallFireball projectile = EntityType.SMALL_FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, tier, "inferno",
            ArcaneWeaponProfile.infernoRange(tier), 1.5F,
            ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.INFERNO));
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private static boolean shootWither(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        int skulls = ArcaneWeaponProfile.witherSkullCount(tier);
        int spawned = 0;
        for (int i = 0; i < skulls; i++) {
            WitherSkull projectile = EntityType.WITHER_SKULL.create(level);
            if (projectile == null) continue;
            Vec3 direction = fanDirection(player.getLookAngle(), i, skulls, 12.0F);
            prepareProjectile(projectile, player, tier, "wither",
                ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.WITHER, tier), 1.25F,
                ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.WITHER), direction);
            level.addFreshEntity(projectile);
            spawned++;
        }
        if (spawned <= 0) return false;
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.85F, 1.15F);
        return true;
    }

    private static boolean shootShulker(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        double range = ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.SHULKER, tier);
        LivingEntity target = findTarget(player, range, 1.5D);
        int shots = ArcaneWeaponProfile.shulkerShotCount(tier);
        int spawned = 0;
        for (int i = 0; i < shots; i++) {
            Vec3 direction = fanDirection(player.getLookAngle(), i, shots, 9.0F);
            ShulkerBullet projectile;
            if (target != null) {
                projectile = new ShulkerBullet(level, player, target, Direction.Axis.Y);
            } else {
                projectile = new ShulkerBullet(EntityType.SHULKER_BULLET, level);
                projectile.setOwner(player);
                Vec3 spawnPos = player.getEyePosition().add(direction.scale(0.8D));
                projectile.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                projectile.setDeltaMovement(direction.scale(1.25D));
            }
            prepareProjectile(projectile, player, tier, "shulker",
                range, 1.25F,
                ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.SHULKER), direction);
            level.addFreshEntity(projectile);
            spawned++;
        }
        if (spawned <= 0) return false;
        level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.15F);
        return true;
    }

    private static boolean shootSnowball(ServerPlayer player, int tier, String kind, float velocity) {
        ServerLevel level = player.serverLevel();
        Snowball projectile = EntityType.SNOWBALL.create(level);
        if (projectile == null) return false;
        ArcaneWeaponProfile.Kind profile = "web".equals(kind) ? ArcaneWeaponProfile.Kind.WEB : ArcaneWeaponProfile.Kind.FROST;
        prepareProjectile(projectile, player, tier, kind, ArcaneWeaponProfile.range(profile, tier), velocity,
            ResonantSetBonus.arcaneMultiplier(player, profile));
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(),
            "web".equals(kind) ? SoundEvents.SPIDER_AMBIENT : SoundEvents.SNOW_GOLEM_SHOOT,
            SoundSource.PLAYERS, 0.7F, "web".equals(kind) ? 1.45F : 1.1F);
        return true;
    }

    private static void prepareProjectile(net.minecraft.world.entity.projectile.Projectile projectile,
                                          ServerPlayer player, int tier, String kind,
                                          double range, float velocity, float multiplier) {
        prepareProjectile(projectile, player, tier, kind, range, velocity, multiplier, player.getLookAngle());
    }

    private static void prepareProjectile(net.minecraft.world.entity.projectile.Projectile projectile,
                                          ServerPlayer player, int tier, String kind,
                                          double range, float velocity, float multiplier, Vec3 direction) {
        Vec3 shot = direction.normalize();
        projectile.setOwner(player);
        projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        projectile.shoot(shot.x, shot.y, shot.z, velocity, 0.0F);
        projectile.getPersistentData().putString(ArcaneWeaponEvents.TAG_KIND, kind);
        projectile.getPersistentData().putInt(ArcaneWeaponEvents.TAG_TIER, tier);
        projectile.getPersistentData().putFloat(ArcaneWeaponEvents.TAG_DAMAGE_MULTIPLIER, multiplier);
        ArcaneWeaponEvents.markRange(projectile, player.position(), range);
    }

    private static Vec3 fanDirection(Vec3 look, int index, int count, float totalSpreadDegrees) {
        if (count <= 1) return look.normalize();
        float step = totalSpreadDegrees / (count - 1);
        float angle = -totalSpreadDegrees * 0.5F + step * index;
        return look.yRot((float)Math.toRadians(angle)).normalize();
    }

    private static boolean castFangs(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 0.001D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        int count = ArcaneWeaponProfile.evokerFangCount(tier);
        int waves = ArcaneWeaponProfile.evokerWaveCount(tier);
        double maxRange = ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.EVOKER, tier);
        float yaw = (float)Math.atan2(flat.z, flat.x);

        // Resolve the fang line once. Higher Core tiers replay this exact same line
        // 1/2/3/4 times instead of extending or shifting subsequent casts.
        double[] fangX = new double[count];
        double[] fangY = new double[count];
        double[] fangZ = new double[count];
        for (int i = 0; i < count; i++) {
            double distance = Math.min(maxRange, 1.5D + i * 1.5D);
            fangX[i] = player.getX() + flat.x * distance;
            fangZ[i] = player.getZ() + flat.z * distance;
            fangY[i] = findGroundY(level, fangX[i], player.getY(), fangZ[i]);
        }

        boolean evokerInLine = false;
        for (int i = 0; i < count && !evokerInLine; i++) {
            AABB fangArea = new AABB(fangX[i] - 0.9D, fangY[i] - 0.5D, fangZ[i] - 0.9D,
                fangX[i] + 0.9D, fangY[i] + 2.0D, fangZ[i] + 0.9D);
            evokerInLine = !level.getEntitiesOfClass(net.minecraft.world.entity.monster.Evoker.class,
                fangArea, LivingEntity::isAlive).isEmpty();
        }
        for (int wave = 0; wave < waves; wave++) {
            int waveDelay = wave * 10;
            for (int i = 0; i < count; i++) {
                level.addFreshEntity(new EvokerFangs(
                    level, fangX[i], fangY[i], fangZ[i], yaw, waveDelay + i * 2, player));
            }
        }
        if (evokerInLine) {
            for (int i = 0; i < count; i++) {
                AABB fangArea = new AABB(fangX[i] - 0.9D, fangY[i] - 0.5D, fangZ[i] - 0.9D,
                    fangX[i] + 0.9D, fangY[i] + 2.0D, fangZ[i] + 0.9D);
                for (net.minecraft.world.entity.monster.Evoker evoker : level.getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Evoker.class, fangArea, LivingEntity::isAlive)) {
                    ArcaneAchievementTracker.markEvokerCast(player, evoker, tier, waves);
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static double findGroundY(ServerLevel level, double x, double originY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos((int)Math.floor(x), (int)Math.floor(originY) + 2, (int)Math.floor(z));
        for (int i = 0; i < 7; i++) {
            BlockPos below = cursor.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                return below.getY() + 1.0D;
            }
            cursor.move(Direction.DOWN);
        }
        return originY;
    }

    private static boolean castLightning(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        double range = ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.LIGHTNING, tier);
        LivingEntity target = findTarget(player, range, 1.3D);
        Vec3 impact = target != null ? target.getBoundingBox().getCenter() : rayEnd(player, range);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.setPos(impact.x, impact.y, impact.z);
            bolt.setCause(player);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        ArcaneWeaponEvents.resolveLightning(level, impact, player, tier, bolt);
        return true;
    }

    private static Vec3 rayEnd(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static void fireGuardian(ServerPlayer player, LivingEntity target, int tier) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 end = target == null
            ? rayEnd(player, ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.GUARDIAN, tier))
            : target.getBoundingBox().getCenter();
        if (target != null) {
            target.hurt(level.damageSources().magic(),
                ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.GUARDIAN, tier) / 4.0F
                    * ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.GUARDIAN));
            if (target instanceof net.minecraft.world.entity.monster.Guardian guardian) {
                ArcaneAchievementTracker.markHit(player, guardian, "guardian", tier);
                ArcaneAchievementTracker.markGuardianPulse(player, guardian);
            }
        }
        drawBeam(level, start, end, ParticleTypes.ELECTRIC_SPARK, 0.45D);
        level.playSound(null, player.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void fireEcho(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double range = ArcaneWeaponProfile.echoRange();
        double beamRadius = ArcaneWeaponProfile.echoBeamRadius();
        AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(beamRadius + 1.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target.isAlive() && target != player)
            .stream()
            .filter(target -> distanceFromRay(start, direction, target.getBoundingBox().getCenter(), range)
                <= beamRadius + target.getBbWidth() * 0.5D)
            .sorted(Comparator.comparingDouble(target -> projection(start, direction, target.getBoundingBox().getCenter())))
            .toList();

        float damage = ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.ECHO, tier)
            * ResonantSetBonus.arcaneMultiplier(player, ArcaneWeaponProfile.Kind.ECHO);
        int killed = 0;
        for (LivingEntity target : targets) {
            ArcaneAchievementTracker.markHit(player, target, "echo", tier);
            boolean wasAlive = target.isAlive();
            target.hurt(level.damageSources().magic(), damage);
            if (wasAlive && !target.isAlive()) killed++;
            if (target instanceof net.minecraft.world.entity.monster.warden.Warden) {
                ArcaneMilestoneTracker.award(player, "engineering/resonant_echo", ArcaneMilestoneTracker.ECHO);
            }
        }
        boolean deepDark = level.getBiome(player.blockPosition()).is(net.minecraft.world.level.biome.Biomes.DEEP_DARK);
        ArcaneAchievementTracker.markEcho(player, targets.size(), killed, deepDark);
        Vec3 end = start.add(direction.scale(range));
        drawBeam(level, start, end, ParticleTypes.SONIC_BOOM, 1.5D);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void drawBeam(ServerLevel level, Vec3 start, Vec3 end,
                                 net.minecraft.core.particles.SimpleParticleType particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.001D) return;
        Vec3 direction = delta.scale(1.0D / length);
        for (double d = 0.5D; d <= length; d += spacing) {
            Vec3 point = start.add(direction.scale(d));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, double range, double beamRadius) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(beamRadius + 2.0D);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                target -> target.isAlive() && target != player && player.hasLineOfSight(target))
            .stream()
            .filter(target -> distanceFromRay(start, direction, target.getBoundingBox().getCenter(), range)
                <= beamRadius + target.getBbWidth() * 0.5D)
            .min(Comparator.comparingDouble(target -> projection(start, direction, target.getBoundingBox().getCenter())))
            .orElse(null);
    }

    private static double projection(Vec3 start, Vec3 direction, Vec3 point) {
        return point.subtract(start).dot(direction);
    }

    private static double distanceFromRay(Vec3 start, Vec3 direction, Vec3 point, double range) {
        double projection = projection(start, direction, point);
        if (projection < 0.0D || projection > range) return Double.MAX_VALUE;
        return point.distanceTo(start.add(direction.scale(projection)));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}
