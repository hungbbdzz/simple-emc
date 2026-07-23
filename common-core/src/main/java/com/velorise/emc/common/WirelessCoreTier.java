package com.velorise.emc.common;

/** Loader-independent identity and progression requirement for Wireless Core I-IV. */
public enum WirelessCoreTier {
    I(1, TechnologyTier.OVERWORLD),
    II(2, TechnologyTier.NETHER),
    III(3, TechnologyTier.END),
    IV(4, TechnologyTier.DEEP_DARK);

    private final int level;
    private final TechnologyTier technologyTier;

    WirelessCoreTier(int level, TechnologyTier technologyTier) {
        this.level = level;
        this.technologyTier = technologyTier;
    }

    public int level() {
        return level;
    }

    public int maximumMode() {
        return level;
    }

    public TechnologyTier technologyTier() {
        return technologyTier;
    }

    public String displayName() {
        return switch (this) {
            case I -> "Overworld";
            case II -> "Infernal";
            case III -> "End";
            case IV -> "Echoing";
        };
    }

    public WirelessCoreTier previous() {
        return switch (this) {
            case I -> I;
            case II -> I;
            case III -> II;
            case IV -> III;
        };
    }

    public static WirelessCoreTier byLevel(int level) {
        if (level <= 1) return I;
        if (level == 2) return II;
        if (level == 3) return III;
        return IV;
    }
}
