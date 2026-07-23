package com.velorise.emc.common;

/** Loader-independent balance values for upgraded Vanilla utility items. */
public final class UtilityItemProfile {
    /** Default constants retained for source compatibility. Runtime code should use accessors below. */
    public static final long BOUND_COMPASS_LOCK_COST = 256L;
    public static final int RECOVERY_CHANNEL_TICKS = 60;
    public static final int RECOVERY_COOLDOWN_TICKS = 300;
    public static final int CURSED_IGNITER_COOLDOWN_TICKS = 120;
    public static final long FISHING_CAST_EMC = 90L;
    public static final long FISHING_BAIT_EMC = FISHING_CAST_EMC;

    private UtilityItemProfile() {}
    public static int clampMode(int mode) { return Math.max(1, Math.min(4, mode)); }
    private static int index(int mode) { return clampMode(mode) - 1; }

    public static long boundCompassLockCost() { return GameplayConfig.current().items.general.boundCompassLockCost; }
    public static int recoveryChannelTicks() { return GameplayConfig.current().items.general.recoveryChannelTicks; }
    public static int recoveryCooldownTicks() { return GameplayConfig.current().items.general.recoveryCooldownTicks; }
    public static double recoveryMovementTolerance() { return GameplayConfig.current().items.general.recoveryMovementTolerance; }
    public static int recoveryHorizontalSearchRadius() { return GameplayConfig.current().items.general.recoveryHorizontalSearchRadius; }
    public static int recoveryVerticalSearchRadius() { return GameplayConfig.current().items.general.recoveryVerticalSearchRadius; }
    public static int cursedIgniterCooldownTicks() { return GameplayConfig.current().items.cursedIgniter.cooldownTicks; }
    public static long fishingCastEmcCost() { return GameplayConfig.current().items.fishingRod.castEmcCost; }


    public static int fishingCatches(int coreLevel) { return GameplayConfig.current().items.fishingRod.catches[index(coreLevel)]; }
    public static long fishingCastCost(int coreLevel) { return fishingCastEmcCost(); }

    public static long cursedIgniterCost(int coreLevel) { return GameplayConfig.current().items.cursedIgniter.emcCost[0]; }
    public static float cursedIgniterDamage(int mode) { return GameplayConfig.current().items.cursedIgniter.damage[index(mode)]; }
    public static int cursedIgniterFireSeconds(int mode) { return GameplayConfig.current().items.cursedIgniter.fireSeconds[index(mode)]; }

    public static int hoeHarvestDiameter(int coreLevel) { return GameplayConfig.current().items.imbuedMiningTools.hoeHarvestDiameter[index(coreLevel)]; }
    public static int hoeHarvestRadius(int coreLevel) { return Math.max(0, (hoeHarvestDiameter(coreLevel) - 1) / 2); }


    /**
     * Extra use-duration ticks to consume this server tick. The cumulative-floor schedule
     * supports arbitrary configured multipliers without loader-specific hardcoded modes.
     */

    public static double elytraBoostImpulse(int mode) { return GameplayConfig.current().items.aethericElytra.boostImpulse[index(mode)]; }
    public static int elytraBoostCooldownTicks() { return GameplayConfig.current().items.aethericElytra.boostCooldownTicks; }

}
