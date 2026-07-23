package com.velorise.emc.common;

/** Shared rules for Mode-bearing equipment upgraded with a Wireless Core tier. */
public final class CoreModeRules {
    private CoreModeRules() {}

    public static int effectiveMode(int storedMode, WirelessCoreTier coreTier) {
        int normalized = Math.max(1, Math.min(4, storedMode));
        return Math.min(normalized, coreTier.maximumMode());
    }

    public static boolean canSelectMode(int requestedMode, WirelessCoreTier coreTier) {
        return requestedMode >= 1 && requestedMode <= coreTier.maximumMode();
    }
}
