package com.velorise.emc.common;

import java.util.Objects;

/** Loader-independent capacity and paging rules for Deepbound storage blocks. */
public final class DeepboundStorageProfile {
    public static final int SLOTS_PER_PAGE = 27;
    public static final int MAX_SLOTS = 162;

    private DeepboundStorageProfile() {}

    public static int slotCount(WirelessCoreTier tier) {
        Objects.requireNonNull(tier, "tier");
        return switch (tier) {
            case I -> 54;
            case II -> 81;
            case III -> 108;
            case IV -> 162;
        };
    }

    public static int pageCount(WirelessCoreTier tier) {
        return slotCount(tier) / SLOTS_PER_PAGE;
    }

    public static int clampPage(WirelessCoreTier tier, int page) {
        return Math.max(0, Math.min(pageCount(tier) - 1, page));
    }

    public static boolean isAccessibleSlot(WirelessCoreTier tier, int slot) {
        return slot >= 0 && slot < slotCount(tier);
    }
}
