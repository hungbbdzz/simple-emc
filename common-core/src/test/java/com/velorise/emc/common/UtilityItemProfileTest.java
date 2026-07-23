package com.velorise.emc.common;

public final class UtilityItemProfileTest {
    public static void main(String[] args) {
        require(UtilityItemProfile.fishingCatches(1) == 2, "fishing Core I");
        require(UtilityItemProfile.fishingCatches(4) == 16, "fishing Core IV");
        require(UtilityItemProfile.fishingCastCost(1) == 256L, "fishing fixed cost I");
        require(UtilityItemProfile.fishingCastCost(4) == 256L, "fishing fixed cost IV");
        require(UtilityItemProfile.cursedIgniterDamage(4) == 16.0F, "igniter damage IV");
        require(UtilityItemProfile.cursedIgniterCost(1) == 1_024L, "igniter fixed cost I");
        require(UtilityItemProfile.cursedIgniterCost(4) == 1_024L, "igniter fixed cost IV");
        require(Math.abs(UtilityItemProfile.recoveryMovementTolerance() - 0.5D) < 0.000001D, "recovery movement tolerance");
        require(UtilityItemProfile.recoveryHorizontalSearchRadius() == 2, "recovery horizontal search");
        require(UtilityItemProfile.recoveryVerticalSearchRadius() == 5, "recovery vertical search");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
