package com.velorise.emc.common;

/** The four conquest stages that advance a world from Tier 0 to Tier 4. */
public enum WorldTierStage {
    OVERWORLD_HUNT(0, "Overworld Hunt", 1),
    NETHER_CONQUEST(1, "Nether Conquest", 2),
    END_CONQUEST(2, "End Conquest", 3),
    DEEP_WORLD_CONQUEST(3, "Deep World Conquest", 4);

    private final int index;
    private final String displayName;
    private final int unlockedWorldTier;

    WorldTierStage(int index, String displayName, int unlockedWorldTier) {
        this.index = index;
        this.displayName = displayName;
        this.unlockedWorldTier = unlockedWorldTier;
    }

    public int index() { return index; }
    public String displayName() { return displayName; }
    public int unlockedWorldTier() { return unlockedWorldTier; }

    public static WorldTierStage byIndex(int index) {
        if (index <= 0) return OVERWORLD_HUNT;
        if (index == 1) return NETHER_CONQUEST;
        if (index == 2) return END_CONQUEST;
        return DEEP_WORLD_CONQUEST;
    }
}
