package com.velorise.emc.common;

import java.util.Set;

/**
 * Shared EMC economy constants for manually priced rare, farmable and technical items.
 *
 * <p>Hand-authored rare-item values use powers of two so they remain easy to reason
 * about and do not drift into arbitrary multi-million prices. Recipe-derived items are
 * intentionally not forced onto powers of two.</p>
 */
public final class EmcEconomyProfile {
    public static final long DEFAULT_GOLDEN_APPLE = 16_384L;
    public static final long BALANCED_GOLDEN_APPLE = 2_048L;

    public static final long DEFAULT_ENCHANTED_GOLDEN_APPLE = 131_072L;
    public static final long BALANCED_ENCHANTED_GOLDEN_APPLE = 65_536L;

    public static final long DEFAULT_TOTEM_OF_UNDYING = 65_536L;
    public static final long BALANCED_TOTEM_OF_UNDYING = 32_768L;

    public static final long DEFAULT_NETHER_STAR = 131_072L;
    public static final long BALANCED_NETHER_STAR = 32_768L;

    public static final long DEFAULT_WITHER_SKELETON_SKULL = 2_048L;
    public static final long BALANCED_WITHER_SKELETON_SKULL = 2_048L;

    public static final long DEFAULT_DRAGON_EGG = 262_144L;
    public static final long BALANCED_DRAGON_EGG = 262_144L;

    public static final long DEFAULT_ELYTRA = 32_768L;
    public static final long BALANCED_ELYTRA = 65_536L;

    public static final long DEFAULT_TRIDENT = 8_192L;
    public static final long BALANCED_TRIDENT = 8_192L;

    public static final long DEFAULT_DIAMOND_HORSE_ARMOR = 32_768L;
    public static final long BALANCED_DIAMOND_HORSE_ARMOR = 16_384L;

    public static final long DEFAULT_HEAVY_CORE = 65_536L;
    public static final long BALANCED_HEAVY_CORE = 65_536L;

    private static final Set<String> NON_TRANSMUTABLE_PATHS = Set.of(
        "bedrock",
        "barrier",
        "light",
        "structure_void",
        "command_block",
        "repeating_command_block",
        "chain_command_block",
        "structure_block",
        "jigsaw",
        "debug_stick",
        "knowledge_book",
        "command_block_minecart",
        "spawner",
        "trial_spawner",
        "vault",
        "end_portal_frame",
        "reinforced_deepslate",
        "budding_amethyst",
        "frogspawn",
        "petrified_oak_slab"
    );

    private EmcEconomyProfile() {}

    public static boolean isPowerOfTwo(long value) {
        return value > 0L && (value & (value - 1L)) == 0L;
    }

    public static boolean isNonTransmutablePath(String path) {
        if (path == null || path.isBlank()) return true;
        return path.endsWith("_spawn_egg") || NON_TRANSMUTABLE_PATHS.contains(path);
    }
}
