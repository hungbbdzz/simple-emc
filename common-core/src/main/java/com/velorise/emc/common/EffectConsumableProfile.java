package com.velorise.emc.common;

/** Balance lookups for reusable Core-bound effect consumables. */
public final class EffectConsumableProfile {
    private EffectConsumableProfile() {}

    private static int index(int coreTier) {
        return Math.max(0, Math.min(3, coreTier - 1));
    }

    public static long purificationCost(int coreTier) {
        return GameplayConfig.current().items.purifyingMilk.emcCost[index(coreTier)];
    }

    /** Vanilla amplifiers are zero-based while displayed potion levels are one-based. */
    public static boolean acceptsEffectLevel(int coreTier, int amplifier) {
        return coreTier >= 4 || amplifier + 1 <= Math.max(1, coreTier);
    }
}
