package com.velorise.emc.common;

/** Balance-only profile for Vanilla mob equipment added by the global World Tier. */
public final class WorldTierEquipmentProfile {
    private static final float[] EQUIPMENT_CHANCE = {0.0F, 0.18F, 0.32F, 0.48F, 0.64F};
    private static final int[] MAX_SLOTS = {0, 2, 3, 4, 5};
    private static final float[] ENCHANT_CHANCE = {0.0F, 0.08F, 0.18F, 0.32F, 0.48F};
    private static final float[] DIAMOND_CHANCE = {0.0F, 0.0F, 0.05F, 0.20F, 0.45F};

    private WorldTierEquipmentProfile() {}

    private static int tier(int worldTier) {
        return Math.max(0, Math.min(4, worldTier));
    }

    public static float equipmentChance(int worldTier) {
        return EQUIPMENT_CHANCE[tier(worldTier)];
    }

    public static int maxGeneratedSlots(int worldTier) {
        return MAX_SLOTS[tier(worldTier)];
    }

    public static float enchantChance(int worldTier) {
        return ENCHANT_CHANCE[tier(worldTier)];
    }

    public static float diamondChance(int worldTier) {
        return DIAMOND_CHANCE[tier(worldTier)];
    }

    /** Level IV remains exceptional rather than becoming a normal roll. */
    public static int enchantLevel(int worldTier, float roll) {
        return switch (tier(worldTier)) {
            case 1 -> 1;
            case 2 -> roll < 0.25F ? 2 : 1;
            case 3 -> roll < 0.15F ? 3 : 2;
            case 4 -> roll < 0.03F ? 4 : (roll < 0.55F ? 3 : 2);
            default -> 0;
        };
    }
}
