package com.velorise.emc.common;

import java.util.Locale;

/** Server-configured objective requirements and multiplayer scaling rules. */
public final class WorldProgressionProfile {
    private WorldProgressionProfile() {}

    public enum ScalingMode {
        NONE,
        LINEAR,
        MULTIPLICATIVE;

        public static ScalingMode byName(String value) {
            if (value == null) return MULTIPLICATIVE;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MULTIPLICATIVE;
            }
        }
    }

    public static long baseRequirement(WorldProgressionObjective objective) {
        GameplayConfig.WorldTierProgression cfg = GameplayConfig.current().progression.worldTiers;
        return switch (objective) {
            case OVERWORLD_TOTAL -> cfg.overworld.totalKills;
            case ZOMBIE_FAMILY -> cfg.overworld.zombieFamily;
            case SKELETON_FAMILY -> cfg.overworld.skeletonFamily;
            case SPIDER_FAMILY -> cfg.overworld.spiderFamily;
            case CREEPER -> cfg.overworld.creeper;
            case ENDERMAN_OVERWORLD -> cfg.overworld.enderman;
            case WITCH -> cfg.overworld.witch;
            case ILLAGER -> cfg.overworld.illager;
            case NETHER_TOTAL -> cfg.nether.totalKills;
            case PIGLIN_FAMILY -> cfg.nether.piglinFamily;
            case ZOMBIFIED_PIGLIN -> cfg.nether.zombifiedPiglin;
            case BLAZE -> cfg.nether.blaze;
            case MAGMA_CUBE -> cfg.nether.magmaCube;
            case GHAST -> cfg.nether.ghast;
            case WITHER_SKELETON -> cfg.nether.witherSkeleton;
            case WITHER -> cfg.nether.wither;
            case INFERNAL_CRUCIBLE -> 1L;
            case ENDERMAN_END -> cfg.end.enderman;
            case SHULKER -> cfg.end.shulker;
            case ENDER_DRAGON -> cfg.end.enderDragon;
            case AETHERIC_BULWARK -> 1L;
            case GUARDIAN -> cfg.deepWorld.guardian;
            case ELDER_GUARDIAN -> cfg.deepWorld.elderGuardian;
            case WARDEN -> cfg.deepWorld.warden;
            case DEEPBOUND_WARDEN -> 1L;
        };
    }

    public static int countedContributors(int contributors) {
        int cap = GameplayConfig.current().progression.worldTiers.maximumContributors;
        int value = Math.max(1, contributors);
        return cap <= 0 ? value : Math.min(value, cap);
    }

    public static double scale(int contributors) {
        GameplayConfig.WorldTierProgression cfg = GameplayConfig.current().progression.worldTiers;
        int additional = Math.max(0, countedContributors(contributors) - 1);
        double raw = switch (ScalingMode.byName(cfg.scalingMode)) {
            case NONE -> 1.0D;
            case LINEAR -> 1.0D + cfg.linearIncreasePerAdditionalPlayer * additional;
            case MULTIPLICATIVE -> Math.pow(cfg.perAdditionalPlayerMultiplier, additional);
        };
        if (!Double.isFinite(raw)) raw = cfg.maximumScale;
        return Math.max(1.0D, Math.min(cfg.maximumScale, raw));
    }

    public static long required(WorldProgressionObjective objective, int contributors) {
        long base = Math.max(0L, baseRequirement(objective));
        if (base == 0L) return 0L;
        GameplayConfig.WorldTierProgression cfg = GameplayConfig.current().progression.worldTiers;
        if (objective.bossObjective() && !cfg.scaleBossObjectives) return base;
        double scaled = base * scale(contributors);
        if (!Double.isFinite(scaled) || scaled >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(base, (long)Math.ceil(scaled));
    }
}
