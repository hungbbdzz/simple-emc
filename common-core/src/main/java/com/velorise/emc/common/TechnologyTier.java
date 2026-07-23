package com.velorise.emc.common;

/** World-level technology milestones used by the 1.9 Wireless Core progression. */
public enum TechnologyTier {
    OVERWORLD(1),
    NETHER(2),
    END(3),
    DEEP_DARK(4);

    private final int id;

    TechnologyTier(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static TechnologyTier byId(int id) {
        if (id <= 1) return OVERWORLD;
        if (id == 2) return NETHER;
        if (id == 3) return END;
        return DEEP_DARK;
    }
}
