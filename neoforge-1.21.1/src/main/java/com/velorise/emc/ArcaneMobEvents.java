package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArcaneMobEvents {
    private static final String ARCANE_TYPE = "VeloriseEMCArcaneMobType";
    private static final String ARCANE_COOLDOWN = "VeloriseEMCArcaneMobCooldown";

    private ArcaneMobEvents() {}

    /** Called after World Tier modifiers are applied. Existing modded equipment is never replaced. */
    public static void tryAssignArcaneAbility(LivingEntity living, int tier) {
        if (!(living instanceof Mob mob) || tier < 2 || living.getMaxHealth() >= 200.0F || !mob.getMainHandItem().isEmpty()) return;
        if (mob.getPersistentData().contains(ARCANE_TYPE) || mob instanceof EnderMan || mob instanceof FlyingMob) return;
        double chance = com.velorise.emc.common.GameplayConfig.current().world.mobThreatScaling.arcaneMobChanceByTier[Math.min(4, tier)];
        if (mob.getRandom().nextDouble() >= chance) return;

        String type;
        int roll = mob.getRandom().nextInt(tier >= 4 ? 5 : 3);
        if (roll == 0) type = "web";
        else if (roll == 1) type = "frost";
        else if (roll == 2) type = "shulker";
        else if (roll == 3) type = "storm";
        else type = "teleport";

        mob.getPersistentData().putString(ARCANE_TYPE, type);
        mob.getPersistentData().putInt(ARCANE_COOLDOWN, 80 + mob.getRandom().nextInt(80));
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(itemFor(type)));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || !mob.isAlive()) return;
        String type = mob.getPersistentData().getString(ARCANE_TYPE);
        if (type.isEmpty()) return;

        int cooldown = mob.getPersistentData().getInt(ARCANE_COOLDOWN);
        if (cooldown > 0) {
            mob.getPersistentData().putInt(ARCANE_COOLDOWN, cooldown - 1);
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target.isSpectator() || (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative())) return;
        double distance = mob.distanceTo(target);
        if (distance > 28.0D) return;
        ServerLevel level = (ServerLevel) mob.level();

        boolean cast = switch (type) {
            case "web" -> castWeb(level, mob, target, distance);
            case "frost" -> castFrost(level, mob, target, distance);
            case "shulker" -> castShulker(level, mob, target, distance);
            case "storm" -> castStorm(level, mob, target, distance);
            case "teleport" -> castTeleport(level, mob, target, distance);
            default -> false;
        };
        if (cast) mob.getPersistentData().putInt(ARCANE_COOLDOWN, cooldownFor(type, mob));
    }

    private static boolean castWeb(ServerLevel level, Mob mob, LivingEntity target, double distance) {
        if (distance > 22.0D || !mob.hasLineOfSight(target)) return false;
        target.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.15D, 0.05D, 0.15D));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.45D, 0.65D, 0.45D, 0.03D);
        level.playSound(null, mob.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.65F);
        return true;
    }

    private static boolean castFrost(ServerLevel level, Mob mob, LivingEntity target, double distance) {
        if (distance > 26.0D || !mob.hasLineOfSight(target)) return false;
        target.hurt(level.damageSources().mobAttack(mob), 5.0F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), 120));
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0D, target.getZ(), 24, 0.4D, 0.8D, 0.4D, 0.03D);
        level.playSound(null, mob.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 1.6F);
        return true;
    }

    private static boolean castShulker(ServerLevel level, Mob mob, LivingEntity target, double distance) {
        if (distance > 22.0D || !mob.hasLineOfSight(target)) return false;
        target.hurt(level.damageSources().mobAttack(mob), 4.0F);
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 0));
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 0.8D, target.getZ(), 28, 0.45D, 0.65D, 0.45D, 0.2D);
        level.playSound(null, mob.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F);
        return true;
    }

    private static boolean castStorm(ServerLevel level, Mob mob, LivingEntity target, double distance) {
        BlockPos strikePos = target.blockPosition();
        if (distance > 24.0D || !mob.hasLineOfSight(target) || !level.isRainingAt(strikePos) || !level.canSeeSky(strikePos)) return false;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.1D, target.getZ(), 35, 0.6D, 0.1D, 0.6D, 0.05D);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(strikePos));
            level.addFreshEntity(bolt);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 0.8F);
        return true;
    }

    private static boolean castTeleport(ServerLevel level, Mob mob, LivingEntity target, double distance) {
        if (distance < 7.0D && mob.hasLineOfSight(target)) return false;
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 3.0D + mob.getRandom().nextDouble() * 4.0D;
            int x = Mth.floor(target.getX() + Math.cos(angle) * radius);
            int z = Mth.floor(target.getZ() + Math.sin(angle) * radius);
            int y = Mth.floor(target.getY());
            BlockPos feet = new BlockPos(x, y, z);
            while (y > level.getMinBuildHeight() + 1 && level.getBlockState(feet.below()).isAir()) feet = feet.below();
            if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
            level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 0.8D, mob.getZ(), 32, 0.4D, 0.8D, 0.4D, 0.2D);
            mob.teleportTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
            level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 0.8D, mob.getZ(), 32, 0.4D, 0.8D, 0.4D, 0.2D);
            level.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    private static Item itemFor(String type) {
        return switch (type) {
            case "web" -> VeloriseEMC.WEBCASTER.get();
            case "frost" -> VeloriseEMC.FROSTBOLT_WAND.get();
            case "shulker" -> VeloriseEMC.SHULKER_SEEKER.get();
            case "storm" -> VeloriseEMC.STORMCALLER.get();
            default -> VeloriseEMC.VOID_TRANSLOCATOR.get();
        };
    }

    private static int cooldownFor(String type, Mob mob) {
        int base = switch (type) {
            case "web" -> 240;
            case "frost" -> 200;
            case "shulker" -> 260;
            case "storm" -> 320;
            default -> 240;
        };
        return base + mob.getRandom().nextInt(81);
    }
}
