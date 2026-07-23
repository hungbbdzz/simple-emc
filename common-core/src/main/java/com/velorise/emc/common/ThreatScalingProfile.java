package com.velorise.emc.common;

/** Shared rules for server-player boss scaling and world-tier hostile-mob scaling. */
public final class ThreatScalingProfile {
    private ThreatScalingProfile() {}

    public static int clampPlayerCount(int count) {
        int cap = GameplayConfig.current().world.bossPlayerScaling.maxCountedPlayers;
        int nonNegative = Math.max(0, count);
        return cap == 0 ? nonNegative : Math.min(cap, nonNegative);
    }

    public static double bossBonusHealth(int countedPlayers) {
        return clampPlayerCount(countedPlayers) * GameplayConfig.current().world.bossPlayerScaling.healthPerPlayer;
    }

    public static float bossBonusDamage(int countedPlayers) {
        return clampPlayerCount(countedPlayers) * GameplayConfig.current().world.bossPlayerScaling.damagePerPlayer;
    }

    public static int threatSteps(WorldProgressionState state) {
        return state == null ? 0 : state.worldTier();
    }

    /** Milestone scaling deliberately jumps at each World Tier, Terraria-style. */
    public static double mobBonusHealth(double baseMaxHealth, WorldProgressionState state) {
        int tier = Math.max(0, Math.min(4, threatSteps(state)));
        if (tier == 0) return 0.0D;
        double multiplier = GameplayConfig.current().world.mobThreatScaling.healthMultiplierByTier[tier];
        return Math.max(0.0D, baseMaxHealth) * Math.max(0.0D, multiplier - 1.0D);
    }

    /** Multiplier applied to the final hostile-mob hit, including compatible modded attacks. */
    public static float mobDamageMultiplier(WorldProgressionState state) {
        int tier = Math.max(0, Math.min(4, threatSteps(state)));
        return GameplayConfig.current().world.mobThreatScaling.damageMultiplierByTier[tier];
    }

    public static double mobBonusArmor(WorldProgressionState state) {
        int tier = Math.max(0, Math.min(4, threatSteps(state)));
        return GameplayConfig.current().world.mobThreatScaling.armorBonusByTier[tier];
    }

    public static double mobBonusKnockbackResistance(WorldProgressionState state) {
        int tier = Math.max(0, Math.min(4, threatSteps(state)));
        return GameplayConfig.current().world.mobThreatScaling.knockbackResistanceByTier[tier];
    }

    /** Retained for older callers/config compatibility. */
    public static float mobBonusDamage(WorldProgressionState state) {
        return Math.max(0.0F, mobDamageMultiplier(state) - 1.0F);
    }
}
