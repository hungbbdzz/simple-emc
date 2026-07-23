package com.velorise.emc.common;

public final class EmcEconomyProfileTest {
    public static void main(String[] args) {
        long[] manualValues = {
            EmcEconomyProfile.DEFAULT_GOLDEN_APPLE,
            EmcEconomyProfile.BALANCED_GOLDEN_APPLE,
            EmcEconomyProfile.DEFAULT_ENCHANTED_GOLDEN_APPLE,
            EmcEconomyProfile.BALANCED_ENCHANTED_GOLDEN_APPLE,
            EmcEconomyProfile.DEFAULT_TOTEM_OF_UNDYING,
            EmcEconomyProfile.BALANCED_TOTEM_OF_UNDYING,
            EmcEconomyProfile.DEFAULT_NETHER_STAR,
            EmcEconomyProfile.BALANCED_NETHER_STAR,
            EmcEconomyProfile.DEFAULT_WITHER_SKELETON_SKULL,
            EmcEconomyProfile.BALANCED_WITHER_SKELETON_SKULL,
            EmcEconomyProfile.DEFAULT_DRAGON_EGG,
            EmcEconomyProfile.BALANCED_DRAGON_EGG,
            EmcEconomyProfile.DEFAULT_ELYTRA,
            EmcEconomyProfile.BALANCED_ELYTRA,
            EmcEconomyProfile.DEFAULT_TRIDENT,
            EmcEconomyProfile.BALANCED_TRIDENT,
            EmcEconomyProfile.DEFAULT_DIAMOND_HORSE_ARMOR,
            EmcEconomyProfile.BALANCED_DIAMOND_HORSE_ARMOR,
            EmcEconomyProfile.DEFAULT_HEAVY_CORE,
            EmcEconomyProfile.BALANCED_HEAVY_CORE
        };
        for (long value : manualValues) require(EmcEconomyProfile.isPowerOfTwo(value), "manual value must be a power of two: " + value);

        require(EmcEconomyProfile.DEFAULT_TOTEM_OF_UNDYING >= EmcEconomyProfile.BALANCED_TOTEM_OF_UNDYING, "totem anti-inflation");
        require(EmcEconomyProfile.DEFAULT_NETHER_STAR >= EmcEconomyProfile.BALANCED_NETHER_STAR, "nether star anti-inflation");
        require(EmcEconomyProfile.DEFAULT_ENCHANTED_GOLDEN_APPLE > EmcEconomyProfile.DEFAULT_GOLDEN_APPLE, "default apple ordering");
        require(EmcEconomyProfile.BALANCED_ENCHANTED_GOLDEN_APPLE > EmcEconomyProfile.BALANCED_GOLDEN_APPLE, "balanced apple ordering");
        require(EmcEconomyProfile.isNonTransmutablePath("zombie_spawn_egg"), "spawn eggs must be blocked");
        require(EmcEconomyProfile.isNonTransmutablePath("bedrock"), "bedrock must be blocked");
        require(!EmcEconomyProfile.isNonTransmutablePath("diamond"), "ordinary items must remain transmutable");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
