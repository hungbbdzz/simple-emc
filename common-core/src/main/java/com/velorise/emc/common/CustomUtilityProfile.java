package com.velorise.emc.common;

/** Server-configurable values for custom utility items and shared repair/teleport mechanics. */
public final class CustomUtilityProfile {
    private CustomUtilityProfile() {}
    private static int coreLevel(int level) { return Math.max(0, Math.min(4, level)); }

    public static long alchemicalAnvilEmcPerMissingLevel() { return GameplayConfig.current().items.customUtilities.alchemicalAnvilEmcPerMissingLevel; }
    public static long activeMeleeCostPerSecond(int mode) { return GameplayConfig.current().items.customUtilities.activeMeleeCostPerSecond[Math.max(1, Math.min(4, mode)) - 1]; }
    public static long activeGenericCostPerSecond(int mode) { return GameplayConfig.current().items.customUtilities.activeGenericCostPerSecond[Math.max(1, Math.min(4, mode)) - 1]; }
    public static float activeMeleeBonusDamage(int mode) { return GameplayConfig.current().items.customUtilities.activeMeleeBonusDamage[Math.max(1, Math.min(4, mode)) - 1]; }

    public static int attractionDiameter(int installedCoreLevel) {
        var c = GameplayConfig.current().items.customUtilities.attractionCatalyst;
        return oddDiameter(c.baseDiameter + coreLevel(installedCoreLevel) * c.diameterPerCore);
    }
    public static int attractionRadius(int installedCoreLevel) { return (attractionDiameter(installedCoreLevel) - 1) / 2; }
    public static long attractionCostPerTick() { return GameplayConfig.current().items.customUtilities.attractionCatalyst.emcCostPerTick; }
    public static double attractionPullStrength() { return GameplayConfig.current().items.customUtilities.attractionCatalyst.pullStrength; }

    public static int chaliceDiameter(int installedCoreLevel) {
        var c = GameplayConfig.current().items.customUtilities.chalice;
        return oddDiameter(c.baseDiameter + coreLevel(installedCoreLevel) * c.diameterPerCore);
    }
    public static int chaliceRadius(int installedCoreLevel) { return (chaliceDiameter(installedCoreLevel) - 1) / 2; }
    public static int chaliceVerticalRadius() { return GameplayConfig.current().items.customUtilities.chalice.verticalRadius; }
    public static long chaliceVoidCost(long water, long lava, long other) {
        var c = GameplayConfig.current().items.customUtilities.chalice;
        return saturatingAdd(c.voidBaseCost,
            saturatingAdd(saturatingMultiply(Math.max(0L, water), c.waterCostPerBlock),
                saturatingAdd(saturatingMultiply(Math.max(0L, lava), c.lavaCostPerBlock),
                    saturatingMultiply(Math.max(0L, other), c.otherFluidCostPerBlock))));
    }
    public static long chaliceGrowthCostPerBlock() { return GameplayConfig.current().items.customUtilities.chalice.growthCostPerBlock; }
    public static long chaliceGrowthCost(long blocks) { return saturatingMultiply(Math.max(0L, blocks), chaliceGrowthCostPerBlock()); }

    public static double translocatorRange(int installedCoreLevel) {
        var values = GameplayConfig.current().items.customUtilities.voidTranslocator.range;
        int index = Math.max(0, Math.min(3, coreLevel(installedCoreLevel) - 1));
        return values[index];
    }
    public static long translocatorCost() { return GameplayConfig.current().items.customUtilities.voidTranslocator.emcCost; }
    public static int translocatorCooldownTicks() { return GameplayConfig.current().items.customUtilities.voidTranslocator.cooldownTicks; }

    public static long teleportCrossDimensionSurcharge() { return GameplayConfig.current().items.customUtilities.teleport.crossDimensionSurcharge; }
    public static double teleportEmcPerBlock() { return GameplayConfig.current().items.customUtilities.teleport.emcPerBlock; }
    public static double magicMirrorEmcPerBlock(int installedCoreLevel) {
        var values = GameplayConfig.current().items.customUtilities.teleport.magicMirrorEmcPerBlock;
        return values[coreLevel(installedCoreLevel)];
    }
    public static double magicMirrorRange(int installedCoreLevel) {
        var values = GameplayConfig.current().items.customUtilities.teleport.magicMirrorRange;
        int index = Math.max(0, Math.min(3, coreLevel(installedCoreLevel) - 1));
        return values[index];
    }
    public static int magicMirrorUseDurationTicks() { return GameplayConfig.current().items.customUtilities.teleport.magicMirrorUseDurationTicks; }
    public static int magicMirrorCooldownTicks() { return GameplayConfig.current().items.customUtilities.teleport.magicMirrorCooldownTicks; }

    private static int oddDiameter(int value) {
        int clamped = Math.max(1, value);
        return (clamped & 1) == 0 ? clamped + 1 : clamped;
    }
    private static long saturatingAdd(long a, long b) {
        if (a <= 0L) return Math.max(0L, b);
        if (b <= 0L) return a;
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
    private static long saturatingMultiply(long a, long b) {
        if (a <= 0L || b <= 0L) return 0L;
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }
}
