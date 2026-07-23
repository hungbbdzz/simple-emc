package com.velorise.emc.common;

/** Determines where personal EMC-powered items draw energy from. */
public enum EmcSourceMode {
    INVENTORY_ONLY,
    FUND_ONLY,
    /**
     * Legacy save/network value retained for backwards compatibility with 1.8.0.
     * New UI and gameplay never expose Hybrid; legacy Hybrid data migrates to Fund Only.
     */
    @Deprecated
    HYBRID;

    public static EmcSourceMode byId(int id) {
        return switch (id) {
            case 0 -> INVENTORY_ONLY;
            case 1 -> FUND_ONLY;
            default -> FUND_ONLY;
        };
    }

    /** Only the two supported choices are cycled by the EMC Hub. */
    public EmcSourceMode next() {
        return this == FUND_ONLY ? INVENTORY_ONLY : FUND_ONLY;
    }

    public boolean usesInventoryFuel() {
        return this == INVENTORY_ONLY;
    }
}
