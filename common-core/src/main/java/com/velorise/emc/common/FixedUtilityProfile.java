package com.velorise.emc.common;

/** Shared balance and behavior values for fixed utility upgrades. */
public final class FixedUtilityProfile {
    public static final double FAR_SIGHT_FOV_MULTIPLIER = 0.45D;

    private FixedUtilityProfile() {}
    public static double farSightFovMultiplier() { return GameplayConfig.current().items.fixedUtilities.farSightFovMultiplier; }
    public static long aquaticActivationCost() { return GameplayConfig.current().items.fixedUtilities.aquaticActivationCost; }
    public static int aquaticWaterBreathingTicks() { return GameplayConfig.current().items.fixedUtilities.aquaticWaterBreathingTicks; }
    public static int aquaticDolphinsGraceTicks() { return GameplayConfig.current().items.fixedUtilities.aquaticDolphinsGraceTicks; }
}
