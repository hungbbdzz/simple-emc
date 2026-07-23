package com.velorise.emc.common;

/** Loader-independent balance values for condensed-core passive EMC blocks. */
public final class CondensedCoreGeneratorProfile {
    private static final int BASE_RADIUS = 3;
    private static final long[] EMC_PER_SECOND = {512L, 4_608L, 41_472L, 373_248L};

    private CondensedCoreGeneratorProfile() {}

    public static int clampTier(int tier) {
        return Math.max(1, Math.min(4, tier));
    }

    public static long emcPerSecond(int tier) {
        return EMC_PER_SECOND[clampTier(tier) - 1];
    }

    /** Tier I starts at 3 blocks; every later tier adds two blocks. */
    public static int radius(int tier) {
        return BASE_RADIUS + (clampTier(tier) - 1) * 2;
    }

    public static int maximumRadius() {
        return radius(4);
    }
}
