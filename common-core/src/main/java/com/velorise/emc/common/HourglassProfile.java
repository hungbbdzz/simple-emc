package com.velorise.emc.common;

/** Shared balance and pulse rules for the event-driven Alchemical Hourglass. */
public final class HourglassProfile {
    public static final int MIN_CORE_LEVEL = 1;
    public static final int MAX_CORE_LEVEL = 4;

    private HourglassProfile() {}

    public static int clampCoreLevel(int coreLevel) {
        return Math.max(MIN_CORE_LEVEL, Math.min(MAX_CORE_LEVEL, coreLevel));
    }

    public static long maxEmc() {
        return GameplayConfig.current().machines.hourglass.maxEmc;
    }

    public static int pulseIntervalTicks() {
        return GameplayConfig.current().machines.hourglass.pulseIntervalTicks;
    }

    public static int bonusTicksPerPulse() {
        return GameplayConfig.current().machines.hourglass.bonusTicksPerPulse;
    }

    public static long emcPerSuccessfulTick() {
        return GameplayConfig.current().machines.hourglass.emcPerSuccessfulTick;
    }

    public static int radius(int coreLevel) {
        return GameplayConfig.current().machines.hourglass.radius[clampCoreLevel(coreLevel) - MIN_CORE_LEVEL];
    }


    public static int pulseOffset(long packedBlockPos) {
        return Math.floorMod(Long.hashCode(packedBlockPos), pulseIntervalTicks());
    }

    public static boolean isPulseDue(long gameTime, long packedBlockPos) {
        return Math.floorMod(gameTime, pulseIntervalTicks()) == pulseOffset(packedBlockPos);
    }
}
