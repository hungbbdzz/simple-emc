package com.velorise.emc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.WitherSkeleton;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Stateful checks for the optional humorous Arcane Weapon challenges. */
public final class ArcaneAchievementTracker {
    private static final String OWNER = "VeloriseEMCArcaneAchievementOwner";
    private static final String KIND = "VeloriseEMCArcaneAchievementKind";
    private static final String HIT_TICK = "VeloriseEMCArcaneAchievementHitTick";
    private static final String ELEMENT_OWNER = "VeloriseEMCElementOwner";
    private static final String ELEMENT_MASK = "VeloriseEMCElementMask";
    private static final String ELEMENT_TICK = "VeloriseEMCElementTick";
    private static final String FROST_OWNER = "VeloriseEMCFrostOwner";
    private static final String FROST_COUNT = "VeloriseEMCFrostCount";
    private static final String FROST_TICK = "VeloriseEMCFrostTick";
    private static final String SHULKER_OWNER = "VeloriseEMCShulkerOwner";
    private static final String SHULKER_START_Y = "VeloriseEMCShulkerStartY";
    private static final String SHULKER_MAX_Y = "VeloriseEMCShulkerMaxY";
    private static final String SHULKER_EXPIRES = "VeloriseEMCShulkerExpires";
    private static final String WEB_OWNER = "VeloriseEMCWebOwner";
    private static final String WEB_SECONDS = "VeloriseEMCWebSeconds";
    private static final String EVOKER_OWNER = "VeloriseEMCEvokerOwner";
    private static final String EVOKER_EXPIRES = "VeloriseEMCEvokerExpires";
    private static final String PERSONAL_KILLS = "VeloriseEMCPersonalMobKills";

    private static final int ELEMENT_FIRE = 1;
    private static final int ELEMENT_FROST = 2;
    private static final int ELEMENT_LIGHTNING = 4;
    private static final int PERSONAL_BLAZE = 1;
    private static final int PERSONAL_GHAST = 2;
    private static final int PERSONAL_WITHER_SKELETON = 4;
    private static final int PERSONAL_SHULKER = 8;
    private static final int PERSONAL_EVOKER = 16;
    private static final int PERSONAL_SPIDER = 32;
    private static final int ALL_PERSONAL = 63;
    private static final Map<ServerLevel, Set<UUID>> SHULKER_TRACKED = new IdentityHashMap<>();

    private ArcaneAchievementTracker() {}

    public static void markHit(ServerPlayer owner, LivingEntity target, String kind, int tier) {
        if (owner == null || target == null || kind == null) return;
        CompoundTag data = target.getPersistentData();
        long now = target.level().getGameTime();
        data.putString(OWNER, owner.getUUID().toString());
        data.putString(KIND, kind);
        data.putLong(HIT_TICK, now);

        int element = switch (kind) {
            case "inferno", "ghastfire", "dragonfire" -> ELEMENT_FIRE;
            case "frost" -> ELEMENT_FROST;
            case "lightning" -> ELEMENT_LIGHTNING;
            default -> 0;
        };
        if (element != 0) recordElement(owner, target, element, now);

        if ("frost".equals(kind)) {
            boolean same = owner.getUUID().toString().equals(data.getString(FROST_OWNER))
                && now - data.getLong(FROST_TICK) <= 100L;
            int count = same ? data.getInt(FROST_COUNT) + 1 : 1;
            data.putString(FROST_OWNER, owner.getUUID().toString());
            data.putInt(FROST_COUNT, count);
            data.putLong(FROST_TICK, now);
            if (tier >= 4 && count >= 4) SimpleAdvancements.award(owner, "engineering/easter/brain_freeze");
        }

        if ("shulker".equals(kind)) {
            data.putString(SHULKER_OWNER, owner.getUUID().toString());
            data.putDouble(SHULKER_START_Y, target.getY());
            data.putDouble(SHULKER_MAX_Y, target.getY());
            data.putLong(SHULKER_EXPIRES, now + 600L);
            if (target.level() instanceof ServerLevel serverLevel) {
                SHULKER_TRACKED.computeIfAbsent(serverLevel, ignored -> new HashSet<>()).add(target.getUUID());
            }
        }

        if ("web".equals(kind)) {
            data.putString(WEB_OWNER, owner.getUUID().toString());
            data.putInt(WEB_SECONDS, 0);
            if (target instanceof Spider) markPersonalKill(owner, PERSONAL_SPIDER);
            if (target instanceof CaveSpider) {
                SimpleAdvancements.award(owner, "engineering/easter/incognito_mode");
            }
        }

        checkCrowdControl(owner, target);
    }

    private static void recordElement(ServerPlayer owner, LivingEntity target, int element, long now) {
        CompoundTag data = target.getPersistentData();
        boolean same = owner.getUUID().toString().equals(data.getString(ELEMENT_OWNER))
            && now - data.getLong(ELEMENT_TICK) <= 200L;
        int mask = (same ? data.getInt(ELEMENT_MASK) : 0) | element;
        data.putString(ELEMENT_OWNER, owner.getUUID().toString());
        data.putInt(ELEMENT_MASK, mask);
        data.putLong(ELEMENT_TICK, now);
        if ((mask & 7) == 7) SimpleAdvancements.award(owner, "engineering/easter/elemental_disagreement");
    }

    public static void markWebVenomPulse(ServerPlayer owner, LivingEntity target) {
        if (owner == null || target == null) return;
        CompoundTag data = target.getPersistentData();
        String id = owner.getUUID().toString();
        if (!id.equals(data.getString(WEB_OWNER))) {
            data.putString(WEB_OWNER, id);
            data.putInt(WEB_SECONDS, 0);
        }
        int seconds = data.getInt(WEB_SECONDS) + 1;
        data.putInt(WEB_SECONDS, seconds);
        if (seconds >= 10) SimpleAdvancements.award(owner, "engineering/easter/terms_and_conditions");
        checkCrowdControl(owner, target);
    }

    public static void markEvokerCast(ServerPlayer owner, Evoker target, int tier, int waves) {
        if (owner == null || target == null) return;
        target.getPersistentData().putString(EVOKER_OWNER, owner.getUUID().toString());
        target.getPersistentData().putLong(EVOKER_EXPIRES, target.level().getGameTime() + Math.max(80L, waves * 20L));
        markHit(owner, target, "evoker", tier);
        if (tier >= 4 && waves >= 4) SimpleAdvancements.award(owner, "engineering/easter/dental_appointment");
    }

    public static void markGuardianPulse(ServerPlayer owner, Guardian target) {
        if (owner == null || target == null) return;
        CompoundTag playerData = owner.getPersistentData();
        String targetKey = "VeloriseEMCGuardianTarget";
        String pulseKey = "VeloriseEMCGuardianPulses";
        String tickKey = "VeloriseEMCGuardianLastPulse";
        long now = owner.level().getGameTime();
        boolean same = target.getUUID().toString().equals(playerData.getString(targetKey))
            && now - playerData.getLong(tickKey) <= 10L;
        int pulses = same ? playerData.getInt(pulseKey) + 1 : 1;
        playerData.putString(targetKey, target.getUUID().toString());
        playerData.putInt(pulseKey, pulses);
        playerData.putLong(tickKey, now);
        if (pulses >= 23) {
            ArcaneMilestoneTracker.award(owner, "engineering/guardian_lens", ArcaneMilestoneTracker.GUARDIAN);
        }
    }

    public static void markGhastfireArea(ServerPlayer owner, int targets) {
        if (owner != null && targets >= 5) SimpleAdvancements.award(owner, "engineering/easter/collateral_architecture");
    }

    public static void markEcho(ServerPlayer owner, int targets, int killed, boolean deepDark) {
        if (owner == null) return;
        if (targets >= 10) SimpleAdvancements.award(owner, "engineering/easter/speak_up");
        if (deepDark && killed >= 5) SimpleAdvancements.award(owner, "engineering/easter/urban_renewal");
    }

    public static void markWitherBurstComplete(ServerPlayer owner, int tier, int skullsPerVolley, int volleys) {
        if (owner != null && tier >= 4 && skullsPerVolley >= 4 && volleys >= 2) {
            SimpleAdvancements.award(owner, "engineering/easter/eight_business_skulls");
        }
    }

    public static void markLightningBurstComplete(ServerPlayer owner, int tier, int uses) {
        if (owner != null && tier >= 4 && uses >= 4) {
            SimpleAdvancements.award(owner, "engineering/easter/fourcast");
        }
    }

    public static void markChargedCreeper(ServerPlayer owner) {
        if (owner != null) SimpleAdvancements.award(owner, "engineering/easter/highly_conductive");
    }

    public static void handleDeath(LivingEntity target, DamageSource source) {
        if (target == null || !(target.level() instanceof ServerLevel level)) return;
        CompoundTag data = target.getPersistentData();
        ServerPlayer owner = resolvePlayer(level.getServer(), data.getString(OWNER));
        String kind = data.getString(KIND);
        long age = level.getGameTime() - data.getLong(HIT_TICK);
        if (owner != null && age >= 0L && age <= 400L) {
            if ("inferno".equals(kind) && target instanceof Blaze) {
                SimpleAdvancements.award(owner, "engineering/easter/youre_fired");
                markPersonalKill(owner, PERSONAL_BLAZE);
            }
            if ("ghastfire".equals(kind) && target instanceof Ghast) {
                ArcaneMilestoneTracker.award(owner, "engineering/ghastfire_staff", ArcaneMilestoneTracker.GHASTFIRE);
                markPersonalKill(owner, PERSONAL_GHAST);
            }
            if ("wither".equals(kind) && target instanceof WitherSkeleton) {
                SimpleAdvancements.award(owner, "engineering/easter/skeleton_management");
                markPersonalKill(owner, PERSONAL_WITHER_SKELETON);
            }
            if ("shulker".equals(kind) && target instanceof Shulker) markPersonalKill(owner, PERSONAL_SHULKER);
            if ("frost".equals(kind) && target instanceof Blaze) {
                SimpleAdvancements.award(owner, "engineering/easter/cold_reception");
            }
            if ("web".equals(kind) && target instanceof Spider) markPersonalKill(owner, PERSONAL_SPIDER);
            if ("dragonfire".equals(kind) && target instanceof EnderDragon) {
                SimpleAdvancements.award(owner, "engineering/easter/dragon_these_nuts");
            }
            if (isFireKind(kind) && isCookableMob(target)) {
                SimpleAdvancements.award(owner, "engineering/easter/well_done");
            }
        }

        ServerPlayer evokerOwner = resolvePlayer(level.getServer(), data.getString(EVOKER_OWNER));
        if (evokerOwner != null && target instanceof Evoker && level.getGameTime() <= data.getLong(EVOKER_EXPIRES)) {
            ArcaneMilestoneTracker.award(evokerOwner, "engineering/evoker_fang_staff", ArcaneMilestoneTracker.EVOKER);
            markPersonalKill(evokerOwner, PERSONAL_EVOKER);
        }

        ServerPlayer shulkerOwner = resolvePlayer(level.getServer(), data.getString(SHULKER_OWNER));
        if (shulkerOwner != null && source != null && "fall".equals(source.getMsgId())) {
            SimpleAdvancements.award(shulkerOwner, "engineering/easter/air_mail");
        }
    }

    public static void tick(MinecraftServer server) {
        if (server == null || server.getTickCount() % 5 != 0) return;
        Iterator<Map.Entry<ServerLevel, Set<UUID>>> levels = SHULKER_TRACKED.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ServerLevel, Set<UUID>> levelEntry = levels.next();
            ServerLevel level = levelEntry.getKey();
            if (level.getServer() != server) continue;
            long now = level.getGameTime();
            Iterator<UUID> tracked = levelEntry.getValue().iterator();
            while (tracked.hasNext()) {
                Entity entity = level.getEntity(tracked.next());
                if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                    tracked.remove();
                    continue;
                }
                CompoundTag data = target.getPersistentData();
                if (!data.contains(SHULKER_OWNER) || now > data.getLong(SHULKER_EXPIRES)) {
                    tracked.remove();
                    continue;
                }
                double maxY = Math.max(data.getDouble(SHULKER_MAX_Y), target.getY());
                data.putDouble(SHULKER_MAX_Y, maxY);
                if (maxY - data.getDouble(SHULKER_START_Y) >= 12.0D) {
                    ServerPlayer owner = resolvePlayer(server, data.getString(SHULKER_OWNER));
                    if (owner != null) SimpleAdvancements.award(owner, "engineering/easter/ground_control");
                    tracked.remove();
                }
            }
            if (levelEntry.getValue().isEmpty()) levels.remove();
        }
    }

    private static void checkCrowdControl(ServerPlayer owner, LivingEntity target) {
        if (owner == null || target == null) return;
        boolean webbed = owner.getUUID().toString().equals(target.getPersistentData().getString(WEB_OWNER));
        if (webbed && target.hasEffect(MobEffects.POISON)
            && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
            && target.hasEffect(MobEffects.LEVITATION)) {
            SimpleAdvancements.award(owner, "engineering/easter/crowd_control_department");
        }
    }

    private static boolean isFireKind(String kind) {
        return "inferno".equals(kind) || "ghastfire".equals(kind) || "dragonfire".equals(kind) || "lightning".equals(kind);
    }

    private static boolean isCookableMob(LivingEntity target) {
        return target instanceof Cow || target instanceof Pig || target instanceof Sheep || target instanceof Chicken
            || target instanceof Rabbit || target instanceof Cod || target instanceof Salmon;
    }

    private static void markPersonalKill(ServerPlayer player, int bit) {
        int mask = player.getPersistentData().getInt(PERSONAL_KILLS) | bit;
        player.getPersistentData().putInt(PERSONAL_KILLS, mask);
        if ((mask & ALL_PERSONAL) == ALL_PERSONAL) {
            SimpleAdvancements.award(player, "engineering/easter/this_seems_personal");
        }
    }

    private static ServerPlayer resolvePlayer(MinecraftServer server, String uuid) {
        if (server == null || uuid == null || uuid.isBlank()) return null;
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
