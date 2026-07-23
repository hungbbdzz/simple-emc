package com.velorise.emc.common;

import java.util.Objects;
import java.util.Optional;

/** Loader-independent progression rules for concrete Wireless Core I-IV items. */
public final class WirelessCoreProfile {
    public static final int CONDENSATION_UNITS = 9;

    private WirelessCoreProfile() {}

    public static Optional<WirelessCoreTier> nextTier(WirelessCoreTier current) {
        Objects.requireNonNull(current, "current");
        return switch (current) {
            case I -> Optional.of(WirelessCoreTier.II);
            case II -> Optional.of(WirelessCoreTier.III);
            case III -> Optional.of(WirelessCoreTier.IV);
            case IV -> Optional.empty();
        };
    }

    public static boolean isDirectUpgrade(WirelessCoreTier current, WirelessCoreTier target) {
        if (current == null || target == null) return false;
        return nextTier(current).map(next -> next == target).orElse(false);
    }

    public static boolean isUnlocked(WorldProgressionState state, WirelessCoreTier target) {
        return state != null && target != null && state.isUnlocked(target);
    }

    public static int condensationUnits(WirelessCoreTier target) {
        Objects.requireNonNull(target, "target");
        return target == WirelessCoreTier.I ? 1 : CONDENSATION_UNITS;
    }

    public static WirelessCoreTier effectiveTier(WirelessCoreTier storedTier) {
        return storedTier == null ? WirelessCoreTier.I : storedTier;
    }
}
