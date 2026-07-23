package com.velorise.emc.common;

/** Loader-independent balance profile for world-generated EMC Aura Nodes. */
public final class AuraNodeProfile {
    private AuraNodeProfile() {}

    public static int clampTier(int tier) {
        return Math.max(1, Math.min(4, tier));
    }

    public static long reserve(int tier) {
        return GameplayConfig.current().world.auraNodes.reserve[clampTier(tier) - 1];
    }

    /** Configured transfer rate per real-time second at 20 server ticks per second. */
    public static long outputPerPlayer(int tier) {
        return GameplayConfig.current().world.auraNodes.outputPerPlayer[clampTier(tier) - 1];
    }

    /** Transfer amount for one pulse while preserving the configured per-second rate. */
    public static long outputPerPlayerPerPulse(int tier) {
        long perSecond = outputPerPlayer(tier);
        int interval = pulseIntervalTicks();
        if (perSecond > Long.MAX_VALUE / interval) return Long.MAX_VALUE;
        return Math.max(1L, (perSecond * interval + 19L) / 20L);
    }

    public static int rechargeTicks(int tier) {
        return GameplayConfig.current().world.auraNodes.rechargeTicks[clampTier(tier) - 1];
    }

    public static int spacingBlocks(int tier) {
        return GameplayConfig.current().world.auraNodes.spacingBlocks[clampTier(tier) - 1];
    }

    public static int outputRadius(int tier) {
        return GameplayConfig.current().world.auraNodes.outputRadius[clampTier(tier) - 1];
    }

    public static int siphonRadius(int tier) {
        return GameplayConfig.current().world.auraNodes.siphonRadius[clampTier(tier) - 1];
    }

    /** Configured health damage per real-time second. */
    public static float siphonDamage(int tier) {
        return GameplayConfig.current().world.auraNodes.siphonDamage[clampTier(tier) - 1];
    }

    public static float siphonDamagePerPulse(int tier) {
        return siphonDamage(tier) * pulseIntervalTicks() / 20.0F;
    }

    public static int pulseIntervalTicks() {
        return GameplayConfig.current().world.auraNodes.pulseIntervalTicks;
    }

    public static int spawnExclusionRadius() {
        return GameplayConfig.current().world.auraNodes.spawnExclusionRadius;
    }

    public static double ancientCitySpawnChance() {
        return GameplayConfig.current().world.auraNodes.ancientCitySpawnChance;
    }

    public static boolean enabled() {
        return GameplayConfig.current().world.auraNodes.enabled;
    }
}
