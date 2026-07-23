package com.velorise.emc.common;

/** Loader-independent balance table for the Core-powered Tidal Conduit. */
public final class CoreAuraMachineProfile {
    public enum Kind { TIDAL_CONDUIT }

    private CoreAuraMachineProfile() {}

    public static int clampTier(int tier) { return Math.max(1, Math.min(4, tier)); }
    private static GameplayConfig.AuraMachine settings(Kind kind) { return GameplayConfig.current().machines.tidalConduit; }
    public static long maxEmc(Kind kind) { return settings(kind).maxEmc; }
    public static int range(Kind kind, int coreTier) { return settings(kind).range[clampTier(coreTier) - 1]; }
    public static long emcPerSecond(Kind kind) { return settings(kind).emcPerSecond; }
    public static int pulseIntervalTicks(Kind kind) { return settings(kind).pulseIntervalTicks; }
    public static int effectDurationTicks(Kind kind) { return settings(kind).effectDurationTicks; }
    public static int fuelIntervalTicks(Kind kind) { return settings(kind).fuelIntervalTicks; }
    public static long emcPerPulse(Kind kind) {
        long seconds = Math.max(1L, pulseIntervalTicks(kind) / 20L);
        return EmcMath.saturatingMultiply(emcPerSecond(kind), seconds, Long.MAX_VALUE);
    }
    public static int pulseOffset(Kind kind, long packedBlockPos) {
        return Math.floorMod(Long.hashCode(packedBlockPos), pulseIntervalTicks(kind));
    }
    public static int fuelOffset(Kind kind, long packedBlockPos) {
        return Math.floorMod(Long.hashCode(packedBlockPos ^ 0x5DEECE66DL), fuelIntervalTicks(kind));
    }
    public static boolean isPulseDue(Kind kind, long gameTime, long packedBlockPos) {
        return Math.floorMod(gameTime, pulseIntervalTicks(kind)) == pulseOffset(kind, packedBlockPos);
    }
    public static boolean isFuelTickDue(Kind kind, long gameTime, long packedBlockPos) {
        return Math.floorMod(gameTime, fuelIntervalTicks(kind)) == fuelOffset(kind, packedBlockPos);
    }
}
