package com.velorise.emc;

import com.velorise.emc.common.GameplayConfig;
import com.velorise.emc.common.ThreatScalingProfile;
import com.velorise.emc.common.WorldProgressionState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.UUID;

/** Additive, event-based boss and hostile-mob scaling with no AI replacement. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThreatScalingEvents {
    private static final UUID BOSS_HEALTH_ID = UUID.fromString("0c95d46c-a152-4f14-9d59-6f4a29f71391");
    private static final UUID MOB_HEALTH_ID = UUID.fromString("bf7e1b78-2b18-4a0c-b97c-0f68b9ddcddb");
    private static final UUID MOB_ARMOR_ID = UUID.fromString("1e8cd3e9-d77f-4a51-b9e0-f954aa3a3822");
    private static final UUID MOB_KNOCKBACK_ID = UUID.fromString("8700e1cb-bb97-451a-a687-5bc36eb8db73");
    private static final String BOSS_SCALED = "VeloriseEMCBossScaled";
    private static final String BOSS_PLAYERS = "VeloriseEMCBossPlayers";
    private static final String BOSS_DAMAGE = "VeloriseEMCBossDamage";
    private static final String MOB_TIER = "VeloriseEMCThreatTier";

    private ThreatScalingEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity living)) return;
        if (isScaledBoss(living)) applyBossScaling(living, level.getServer());
        else applyMobScaling(living, WorldProgressionData.get(level.getServer()).snapshot());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        if (isScaledBoss(livingAttacker)) {
            MinecraftServer server = livingAttacker.getServer();
            if (server != null && !livingAttacker.getPersistentData().getBoolean(BOSS_SCALED)) {
                applyBossScaling(livingAttacker, server);
            }
            if (livingAttacker instanceof EnderDragon && event.getSource().getDirectEntity() instanceof AreaEffectCloud) return;
            float bonus = livingAttacker.getPersistentData().getFloat(BOSS_DAMAGE);
            if (bonus > 0.0F) event.setAmount(event.getAmount() + bonus);
            return;
        }

        if (!(livingAttacker instanceof Monster) || isExcludedHostile(livingAttacker)) return;
        MinecraftServer server = livingAttacker.getServer();
        if (server == null || !GameplayConfig.current().world.mobThreatScaling.enabled) return;
        float multiplier = ThreatScalingProfile.mobDamageMultiplier(WorldProgressionData.get(server).snapshot());
        if (multiplier > 1.0F) event.setAmount(event.getAmount() * multiplier);
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        if (dead instanceof ServerPlayer victim && attacker instanceof Warden warden) {
            int counted = warden.getPersistentData().getInt(BOSS_PLAYERS);
            if (reachedEasterEggScale(counted) && event.getSource().getMsgId().contains("sonic_boom")) {
                SimpleAdvancements.award(victim, "world/easter/human_resources_problem");
            }
            return;
        }

        if (isScaledBoss(dead)) {
            int counted = dead.getPersistentData().getInt(BOSS_PLAYERS);
            if (attacker instanceof ServerPlayer killer) {
                SimpleAdvancements.award(killer, "world/stronger_foe");
                if (counted >= 5) SimpleAdvancements.award(killer, "world/war_party");
                if (reachedEasterEggScale(counted)) SimpleAdvancements.award(killer, "world/united_front");
            }
            if (reachedEasterEggScale(counted) && dead.level() instanceof ServerLevel level) {
                java.util.List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
                    dead.getBoundingBox().inflate(96.0D), ThreatScalingEvents::isEligiblePlayer);
                if (nearby.size() == 1) {
                    SimpleAdvancements.award(nearby.get(0), "world/easter/meeting_could_have_been_an_email");
                }
            }
            return;
        }

        if (dead instanceof Monster && attacker instanceof ServerPlayer killer) {
            int threatTier = dead.getPersistentData().getInt(MOB_TIER);
            if (threatTier > 0) SimpleAdvancements.award(killer, "world/world_hardens");
            if (threatTier >= 3) SimpleAdvancements.award(killer, "world/apex_survivor");
        }
    }

    public static void refreshLoadedMobs(MinecraftServer server) {
        if (server == null) return;
        WorldProgressionState state = WorldProgressionData.get(server).snapshot();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !isScaledBoss(living)) applyMobScaling(living, state);
            }
        }
    }

    private static void applyBossScaling(LivingEntity boss, MinecraftServer server) {
        GameplayConfig.BossPlayerScaling config = GameplayConfig.current().world.bossPlayerScaling;
        if (!config.enabled || !bossEnabled(boss, config) || boss.getPersistentData().getBoolean(BOSS_SCALED)) return;

        int online = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isCreative() && !player.isSpectator()) online++;
        }
        int counted = ThreatScalingProfile.clampPlayerCount(online);
        if (counted <= 0) return;
        double healthBonus = ThreatScalingProfile.bossBonusHealth(counted);
        float damageBonus = ThreatScalingProfile.bossBonusDamage(counted);

        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && healthBonus > 0.0D) {
            health.removeModifier(BOSS_HEALTH_ID);
            health.addPermanentModifier(new AttributeModifier(BOSS_HEALTH_ID, "velorise_emc.server_player_boss_health",
                healthBonus, AttributeModifier.Operation.ADDITION));
            boss.setHealth(boss.getMaxHealth());
        }
        boss.getPersistentData().putBoolean(BOSS_SCALED, true);
        boss.getPersistentData().putInt(BOSS_PLAYERS, counted);
        boss.getPersistentData().putFloat(BOSS_DAMAGE, damageBonus);

        if (reachedEasterEggScale(counted) && boss.level() instanceof ServerLevel level) {
            java.util.List<ServerPlayer> inDimension = level.players().stream()
                .filter(ThreatScalingEvents::isEligiblePlayer)
                .toList();
            if (inDimension.size() == 1) {
                SimpleAdvancements.award(inDimension.get(0), "world/easter/understaffed");
            }
        }
    }

    private static void applyMobScaling(LivingEntity living, WorldProgressionState state) {
        if (!(living instanceof Monster) || isScaledBoss(living) || isExcludedHostile(living)) return;
        GameplayConfig.MobThreatScaling config = GameplayConfig.current().world.mobThreatScaling;
        if (!config.enabled) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (!config.scaleModdedHostiles && id != null && !"minecraft".equals(id.getNamespace())) return;

        int tier = ThreatScalingProfile.threatSteps(state);
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        int oldTier = living.getPersistentData().getInt(MOB_TIER);
        if (oldTier == tier && health.getModifier(MOB_HEALTH_ID) != null) return;

        double oldMax = living.getMaxHealth();
        health.removeModifier(MOB_HEALTH_ID);
        double bonus = ThreatScalingProfile.mobBonusHealth(health.getBaseValue(), state);
        if (bonus > 0.0D) {
            health.addPermanentModifier(new AttributeModifier(MOB_HEALTH_ID, "velorise_emc.world_threat_health",
                bonus, AttributeModifier.Operation.ADDITION));
        }
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(MOB_ARMOR_ID);
            double bonusArmor = ThreatScalingProfile.mobBonusArmor(state);
            if (bonusArmor > 0.0D) armor.addPermanentModifier(new AttributeModifier(MOB_ARMOR_ID, "velorise_emc.world_threat_armor", bonusArmor, AttributeModifier.Operation.ADDITION));
        }
        AttributeInstance knockback = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(MOB_KNOCKBACK_ID);
            double bonusKnockback = ThreatScalingProfile.mobBonusKnockbackResistance(state);
            if (bonusKnockback > 0.0D) knockback.addPermanentModifier(new AttributeModifier(MOB_KNOCKBACK_ID, "velorise_emc.world_threat_knockback", bonusKnockback, AttributeModifier.Operation.ADDITION));
        }
        double gained = Math.max(0.0D, living.getMaxHealth() - oldMax);
        if (gained > 0.0D) living.setHealth((float)Math.min(living.getMaxHealth(), living.getHealth() + gained));
        living.getPersistentData().putInt(MOB_TIER, tier);
        ArcaneMobEvents.tryAssignArcaneAbility(living, tier);
    }

    private static boolean bossEnabled(LivingEntity entity, GameplayConfig.BossPlayerScaling config) {
        if (entity instanceof EnderDragon) return config.enderDragon;
        if (entity instanceof WitherBoss) return config.wither;
        return entity instanceof Warden && config.warden;
    }

    private static boolean isScaledBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof Warden;
    }

    private static boolean isExcludedHostile(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) return false;
        String key = id.toString();
        return Arrays.asList(GameplayConfig.current().world.mobThreatScaling.excludedEntities).contains(key);
    }
    private static boolean isEligiblePlayer(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static boolean reachedEasterEggScale(int countedPlayers) {
        int configuredCap = GameplayConfig.current().world.bossPlayerScaling.maxCountedPlayers;
        int threshold = configuredCap > 0 ? Math.max(2, configuredCap) : 10;
        return countedPlayers >= threshold;
    }

}
