package com.velorise.emc.common;

/** Balance values for the Purification Grindstone. */
public final class PurificationProfile {
    private PurificationProfile() {}

    /** Core I-IV use one fixed price per successful operation. */
    public static long operationCost(int coreTier) {
        return GameplayConfig.current().machines.purificationGrindstone.operationCost;
    }

    /** Compatibility aliases: enchantment level no longer changes the price. */
    public static long extractionCost(int coreTier) { return operationCost(coreTier); }
    public static long curseRemovalCost(int coreTier) { return operationCost(coreTier); }
}
