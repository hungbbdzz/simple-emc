package com.velorise.emc.common;

/** Shared balance table for the Core-scaled Resonant Furnace family. */
public final class ResonantMachineProfile {
    private ResonantMachineProfile() {}

    public static int clampCoreTier(int coreTier) { return Math.max(1, Math.min(4, coreTier)); }

    public static int furnaceSpeedMultiplier(int coreTier) {
        return GameplayConfig.current().machines.resonantFurnace.speedMultiplier[clampCoreTier(coreTier) - 1];
    }
}
