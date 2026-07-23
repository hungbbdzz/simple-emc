package com.velorise.emc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.velorise.emc.common.EmcEconomyProfile;
import com.velorise.emc.common.EmcItemPolicy;
import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcPolicyProfile;
import com.velorise.emc.common.EmcValueMode;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central EMC price registry.
 *
 * <p>Version 2 separates explicit source prices from recipe-derived prices. Explicit
 * source values and server overrides are locked; every other item is resolved from
 * the cheapest valid recipe. Numeric EMC resolution is intentionally separate from
 * server-authoritative burn, learn and materialize permissions.</p>
 */
public final class EMCRegistry {
    private static final Map<Item, Long> EMC_MAP = new LinkedHashMap<>();
    private static final Map<Item, Long> STATIC_EMC_MAP = new LinkedHashMap<>();
    private static final Map<Item, Long> BALANCED_SOURCE_MAP = new LinkedHashMap<>();
    private static final Map<Item, Long> SHARED_SOURCE_MAP = new LinkedHashMap<>();
    private static final Map<Item, Long> DEFAULT_EMC_MAP = new LinkedHashMap<>();
    private static final Map<Item, Long> LEGACY_FALLBACK_MAP = new HashMap<>();
    private static final Set<Item> CUSTOM_OVERRIDE_ITEMS = new HashSet<>();
    private static final Map<Item, EmcPolicyOverride> SERVER_POLICY_OVERRIDES = new HashMap<>();

    private static Map<Item, Long> clientSyncedOverrides = null;
    private static Map<Item, EmcPolicyOverride> clientSyncedPolicyOverrides = null;
    private static EmcValueMode configuredValueMode = EmcValueMode.DEFAULT;
    private static EmcValueMode clientSyncedValueMode = null;
    private static EmcValueMode activeValueMode = EmcValueMode.DEFAULT;
    private static final File CUSTOM_EMC_FILE = new File("config/velorise_emc/custom_emc.json");
    private static final File VALUE_MODE_FILE = new File("config/velorise_emc/emc_value_mode.json");
    private static final File ITEM_POLICY_FILE = new File("config/velorise_emc/emc_item_policies.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_RESOLVE_ITERATIONS = 128;

    static {
        registerLegacyFallbacks();
        registerBalancedSources();
        configuredValueMode = readValueMode();
        rebuildStaticSources(configuredValueMode);
        EMC_MAP.putAll(STATIC_EMC_MAP);
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);
        loadRecipeCache();
        loadCustomEMC();
        loadItemPolicies();
    }

    private EMCRegistry() {}

    private static void registerBalancedSources() {
        // Basic natural materials
        balancedSource(Items.COBBLESTONE, 2L);
        balancedSource(Items.COBBLED_DEEPSLATE, 4L);
        balancedSource(Items.DIRT, 2L);
        balancedSource(Items.SAND, 2L);
        balancedSource(Items.GRAVEL, 2L);
        balancedSource(Items.TUFF, 2L);
        balancedSource(Items.DEEPSLATE, 4L);
        balancedSource(Items.BASALT, 4L);
        balancedSource(Items.NETHERRACK, 2L);
        balancedSource(Items.COAL, 128L);
        balancedSource(Items.RAW_COPPER, 64L);
        balancedSource(Items.RAW_IRON, 256L);
        balancedSource(Items.RAW_GOLD, 512L);
        balancedSource(Items.COPPER_INGOT, 64L);
        balancedSource(Items.IRON_INGOT, 256L);
        balancedSource(Items.GOLD_INGOT, 512L);
        balancedSource(Items.REDSTONE, 32L);
        balancedSource(Items.LAPIS_LAZULI, 32L);
        balancedSource(Items.QUARTZ, 128L);
        balancedSource(Items.GLOWSTONE_DUST, 64L);
        balancedSource(Items.DIAMOND, 4_096L);
        // Balanced Mode keeps villager trading as a renewable EMC source. The transmutation
        // tax controls repeated materialization instead of blacklisting traded equipment.
        // Resonant Ore and exploration progression remain decisively faster than repeated restocks.
        balancedSource(Items.EMERALD, 512L);
        balancedSource(Items.ANCIENT_DEBRIS, 32_768L);
        // Filled buckets are explicit Balanced sources so Bottomless Bucket logic never falls back to 1 EMC.
        balancedSource(Items.BUCKET, 384L);
        balancedSource(Items.WATER_BUCKET, 400L);
        balancedSource(Items.LAVA_BUCKET, 448L);

        // Woods and crops
        balancedSource(Items.OAK_LOG, 64L);
        balancedSource(Items.SPRUCE_LOG, 64L);
        balancedSource(Items.BIRCH_LOG, 64L);
        balancedSource(Items.JUNGLE_LOG, 64L);
        balancedSource(Items.ACACIA_LOG, 64L);
        balancedSource(Items.DARK_OAK_LOG, 64L);
        balancedSource(Items.MANGROVE_LOG, 64L);
        balancedSource(Items.CHERRY_LOG, 64L);
        balancedSource(Items.SUGAR_CANE, 16L);
        balancedSource(Items.WHEAT, 16L);
        balancedSource(Items.WHEAT_SEEDS, 2L);
        balancedSource(Items.BEETROOT_SEEDS, 2L);
        balancedSource(Items.MELON_SEEDS, 2L);
        balancedSource(Items.PUMPKIN_SEEDS, 2L);
        balancedSource(Items.CARROT, 16L);
        balancedSource(Items.POTATO, 16L);
        balancedSource(Items.APPLE, 64L);

        // Mob drops and unique loot
        balancedSource(Items.ROTTEN_FLESH, 16L);
        balancedSource(Items.BONE, 64L);
        balancedSource(Items.GUNPOWDER, 128L);
        balancedSource(Items.STRING, 32L);
        balancedSource(Items.ENDER_PEARL, 512L);
        balancedSource(Items.BLAZE_ROD, 1_024L);
        balancedSource(Items.GHAST_TEAR, 4_096L);
        balancedSource(Items.WITHER_SKELETON_SKULL, EmcEconomyProfile.BALANCED_WITHER_SKELETON_SKULL);
        balancedSource(Items.TOTEM_OF_UNDYING, EmcEconomyProfile.BALANCED_TOTEM_OF_UNDYING);
        balancedSource(Items.NETHER_STAR, EmcEconomyProfile.BALANCED_NETHER_STAR);
        balancedSource(Items.ELYTRA, EmcEconomyProfile.BALANCED_ELYTRA);
        balancedSource(Items.DRAGON_EGG, EmcEconomyProfile.BALANCED_DRAGON_EGG);
        balancedSource(Items.GOLDEN_APPLE, EmcEconomyProfile.BALANCED_GOLDEN_APPLE);
        balancedSource(Items.ENCHANTED_GOLDEN_APPLE, EmcEconomyProfile.BALANCED_ENCHANTED_GOLDEN_APPLE);
        balancedSource(Items.TRIDENT, EmcEconomyProfile.BALANCED_TRIDENT);
        balancedSource(Items.DIAMOND_HORSE_ARMOR, EmcEconomyProfile.BALANCED_DIAMOND_HORSE_ARMOR);


        // Core Ascension exploration materials. These explicit sources keep the
        // Smithing-template chain resolvable in Balanced Mode even for chest loot
        // and self-duplicating Vanilla templates that have no acyclic base recipe.
        balancedSource(Items.SLIME_BALL, 64L);
        balancedSource(Items.MAGMA_CREAM, 576L);
        balancedSource(Items.CRYING_OBSIDIAN, 128L);
        balancedSource(Items.NETHER_WART, 32L);
        balancedSource(Items.GILDED_BLACKSTONE, 256L);
        balancedSource(Items.SHULKER_SHELL, 4_096L);
        balancedSource(Items.DRAGON_BREATH, 2_048L);
        balancedSource(Items.DRAGON_HEAD, 16_384L);
        balancedSource(Items.END_CRYSTAL, 8_192L);
        balancedSource(Items.CHORUS_FLOWER, 256L);
        balancedSource(Items.END_ROD, 256L);
        balancedSource(Items.PURPUR_BLOCK, 64L);
        balancedSource(Items.ECHO_SHARD, 8_192L);
        balancedSource(Items.RECOVERY_COMPASS, 33_792L);
        balancedSource(Items.DISC_FRAGMENT_5, 1_024L);
        balancedSource(Items.SCULK_CATALYST, 4_096L);
        balancedSource(Items.HEART_OF_THE_SEA, 32_768L);
        balancedSource(Items.SPONGE, 1_024L);
        balancedSource(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, 65_536L);
        balancedSource(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 131_072L);

        // Velorise EMC fuel chain. Values are the actual in-game values; no hidden multiplier.
        sharedSource(VeloriseEMC.ALCHEMICAL_COAL.get(), 512L);
        sharedSource(VeloriseEMC.MOBIUS_FUEL.get(), 2_048L);
        sharedSource(VeloriseEMC.AETERNALIS_FUEL.get(), 8_192L);
        sharedSource(VeloriseEMC.ALCHEMICAL_COAL_BLOCK_ITEM.get(), 4_608L);
        sharedSource(VeloriseEMC.MOBIUS_FUEL_BLOCK_ITEM.get(), 18_432L);
        sharedSource(VeloriseEMC.AETERNALIS_FUEL_BLOCK_ITEM.get(), 73_728L);

        // Core and ascension-template values are fixed sources. Smithing recipes
        // consume templates, so relying only on recursive recipe resolution can
        // leave the result at zero when a template is chest loot or self-duplicates.
        sharedSource(VeloriseEMC.WIRELESS_CORE.get(), 67_584L);
        sharedSource(VeloriseEMC.INFERNAL_ASCENSION_TEMPLATE.get(), 106_464L);
        sharedSource(VeloriseEMC.WIRELESS_CORE_T2.get(), 719_328L);
        sharedSource(VeloriseEMC.END_ASCENSION_TEMPLATE.get(), 193_056L);
        sharedSource(VeloriseEMC.WIRELESS_CORE_T3.get(), 6_685_440L);
        sharedSource(VeloriseEMC.DEEP_CONVERGENCE_TEMPLATE.get(), 466_464L);
        sharedSource(VeloriseEMC.WIRELESS_CORE_T4.get(), 60_709_152L);
        sharedSource(VeloriseEMC.HEART_OF_THE_LAST_ECHO.get(), com.velorise.emc.common.LastEchoProfile.HEART_EMC);
        sharedSource(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get(), com.velorise.emc.common.LastEchoProfile.SCALE_EMC);

        // World-Tier Resonant materials are fixed source values. Equipment remains
        // recipe-derived so every consumed base item, template and ingot is preserved in EMC.
        sharedSource(VeloriseEMC.RAW_VERDANT_RESONANT.get(), 32_768L);
        sharedSource(VeloriseEMC.VERDANT_RESONANT_INGOT.get(), 33_024L);
        sharedSource(VeloriseEMC.VERDANT_RESONANT_ORE_ITEM.get(), 32_768L);
        sharedSource(VeloriseEMC.DEEPSLATE_VERDANT_RESONANT_ORE_ITEM.get(), 32_768L);
        sharedSource(VeloriseEMC.VERDANT_RESONANT_BLOCK_ITEM.get(), 297_216L);
        sharedSource(VeloriseEMC.RAW_INFERNAL_RESONANT.get(), 65_536L);
        sharedSource(VeloriseEMC.INFERNAL_RESONANT_INGOT.get(), 66_048L);
        sharedSource(VeloriseEMC.INFERNAL_RESONANT_ORE_ITEM.get(), 65_536L);
        sharedSource(VeloriseEMC.INFERNAL_RESONANT_BLOCK_ITEM.get(), 594_432L);
        sharedSource(VeloriseEMC.RAW_AETHERIC_RESONANT.get(), 131_072L);
        sharedSource(VeloriseEMC.AETHERIC_RESONANT_INGOT.get(), 132_096L);
        sharedSource(VeloriseEMC.AETHERIC_RESONANT_ORE_ITEM.get(), 131_072L);
        sharedSource(VeloriseEMC.AETHERIC_RESONANT_BLOCK_ITEM.get(), 1_188_864L);
        sharedSource(VeloriseEMC.RAW_ECHOING_RESONANT.get(), 262_144L);
        sharedSource(VeloriseEMC.ECHOING_RESONANT_INGOT.get(), 264_192L);
        sharedSource(VeloriseEMC.ECHOING_RESONANT_ORE_ITEM.get(), 262_144L);
        sharedSource(VeloriseEMC.ECHOING_RESONANT_BLOCK_ITEM.get(), 2_377_728L);

        // Account-bound tables must always retain a stable item EMC value even when
        // recipe resolution skips the reusable Philosopher's Stone catalyst.
        sharedSource(VeloriseEMC.TRANSMUTATION_TABLE_ITEM.get(), 264L);
        sharedSource(VeloriseEMC.ARCANE_TRANSMUTATION_TABLE_ITEM.get(), 590_088L);

    }

    private static void registerLegacyFallbacks() {
        legacy(Items.COBBLESTONE, 1L);
        legacy(Items.COBBLED_DEEPSLATE, 1L);
        legacy(Items.STONE, 1L);
        legacy(Items.DEEPSLATE, 1L);
        legacy(Items.DIRT, 1L);
        legacy(Items.GRASS_BLOCK, 2L);
        legacy(Items.SAND, 1L);
        legacy(Items.GRAVEL, 1L);
        legacy(Items.CLAY_BALL, 64L);
        legacy(Items.CLAY, 256L);
        legacy(Items.FLINT, 4L);
        legacy(Items.OBSIDIAN, 64L);
        legacy(Items.CRYING_OBSIDIAN, 96L);
        legacy(Items.TUFF, 1L);
        legacy(Items.CALCITE, 4L);
        legacy(Items.BASALT, 4L);
        legacy(Items.NETHERRACK, 1L);
        legacy(Items.SOUL_SAND, 49L);
        legacy(Items.SOUL_SOIL, 49L);
        legacy(Items.CRIMSON_NYLIUM, 4L);
        legacy(Items.WARPED_NYLIUM, 4L);
        legacy(Items.GILDED_BLACKSTONE, 128L);
        legacy(Items.PODZOL, 2L);
        legacy(Items.ROOTED_DIRT, 2L);
        legacy(Items.MUD, 4L);
        legacy(Items.FARMLAND, 2L);
        legacy(Items.DIRT_PATH, 2L);
        legacy(Items.MYCELIUM, 2L);
        legacy(Items.SUSPICIOUS_SAND, 2L);
        legacy(Items.SUSPICIOUS_GRAVEL, 2L);
        legacy(Items.POINTED_DRIPSTONE, 4L);
        legacy(Items.DRIPSTONE_BLOCK, 16L);
        legacy(Items.INFESTED_STONE, 1L);
        legacy(Items.INFESTED_COBBLESTONE, 1L);
        legacy(Items.INFESTED_STONE_BRICKS, 1L);
        legacy(Items.INFESTED_MOSSY_STONE_BRICKS, 1L);
        legacy(Items.INFESTED_CRACKED_STONE_BRICKS, 1L);
        legacy(Items.INFESTED_CHISELED_STONE_BRICKS, 1L);
        legacy(Items.INFESTED_DEEPSLATE, 1L);
        legacy(Items.WHITE_WOOL, 48L);
        legacy(Items.ORANGE_WOOL, 48L);
        legacy(Items.MAGENTA_WOOL, 48L);
        legacy(Items.LIGHT_BLUE_WOOL, 48L);
        legacy(Items.YELLOW_WOOL, 48L);
        legacy(Items.LIME_WOOL, 48L);
        legacy(Items.PINK_WOOL, 48L);
        legacy(Items.GRAY_WOOL, 48L);
        legacy(Items.LIGHT_GRAY_WOOL, 48L);
        legacy(Items.CYAN_WOOL, 48L);
        legacy(Items.PURPLE_WOOL, 48L);
        legacy(Items.BLUE_WOOL, 48L);
        legacy(Items.BROWN_WOOL, 48L);
        legacy(Items.GREEN_WOOL, 48L);
        legacy(Items.RED_WOOL, 48L);
        legacy(Items.BLACK_WOOL, 48L);
        legacy(Items.WHITE_CARPET, 32L);
        legacy(Items.ORANGE_CARPET, 32L);
        legacy(Items.MAGENTA_CARPET, 32L);
        legacy(Items.LIGHT_BLUE_CARPET, 32L);
        legacy(Items.YELLOW_CARPET, 32L);
        legacy(Items.LIME_CARPET, 32L);
        legacy(Items.PINK_CARPET, 32L);
        legacy(Items.GRAY_CARPET, 32L);
        legacy(Items.LIGHT_GRAY_CARPET, 32L);
        legacy(Items.CYAN_CARPET, 32L);
        legacy(Items.PURPLE_CARPET, 32L);
        legacy(Items.BLUE_CARPET, 32L);
        legacy(Items.BROWN_CARPET, 32L);
        legacy(Items.GREEN_CARPET, 32L);
        legacy(Items.RED_CARPET, 32L);
        legacy(Items.BLACK_CARPET, 32L);
        legacy(Items.MOSS_CARPET, 1L);
        legacy(Items.OAK_LOG, 32L);
        legacy(Items.SPRUCE_LOG, 32L);
        legacy(Items.BIRCH_LOG, 32L);
        legacy(Items.JUNGLE_LOG, 32L);
        legacy(Items.ACACIA_LOG, 32L);
        legacy(Items.DARK_OAK_LOG, 32L);
        legacy(Items.MANGROVE_LOG, 32L);
        legacy(Items.CHERRY_LOG, 32L);
        legacy(Items.CRIMSON_STEM, 32L);
        legacy(Items.WARPED_STEM, 32L);
        legacy(Items.CRIMSON_HYPHAE, 32L);
        legacy(Items.WARPED_HYPHAE, 32L);
        legacy(Items.OAK_WOOD, 32L);
        legacy(Items.SPRUCE_WOOD, 32L);
        legacy(Items.BIRCH_WOOD, 32L);
        legacy(Items.JUNGLE_WOOD, 32L);
        legacy(Items.ACACIA_WOOD, 32L);
        legacy(Items.DARK_OAK_WOOD, 32L);
        legacy(Items.MANGROVE_WOOD, 32L);
        legacy(Items.CHERRY_WOOD, 32L);
        legacy(Items.CAMPFIRE, 140L);
        legacy(Items.SOUL_CAMPFIRE, 157L);
        legacy(Items.OAK_PLANKS, 8L);
        legacy(Items.SPRUCE_PLANKS, 8L);
        legacy(Items.BIRCH_PLANKS, 8L);
        legacy(Items.JUNGLE_PLANKS, 8L);
        legacy(Items.ACACIA_PLANKS, 8L);
        legacy(Items.DARK_OAK_PLANKS, 8L);
        legacy(Items.MANGROVE_PLANKS, 8L);
        legacy(Items.CHERRY_PLANKS, 8L);
        legacy(Items.OAK_SAPLING, 32L);
        legacy(Items.SPRUCE_SAPLING, 32L);
        legacy(Items.BIRCH_SAPLING, 32L);
        legacy(Items.JUNGLE_SAPLING, 32L);
        legacy(Items.ACACIA_SAPLING, 32L);
        legacy(Items.DARK_OAK_SAPLING, 32L);
        legacy(Items.CHERRY_SAPLING, 32L);
        legacy(Items.MANGROVE_PROPAGULE, 32L);
        legacy(Items.OAK_LEAVES, 1L);
        legacy(Items.SPRUCE_LEAVES, 1L);
        legacy(Items.BIRCH_LEAVES, 1L);
        legacy(Items.JUNGLE_LEAVES, 1L);
        legacy(Items.ACACIA_LEAVES, 1L);
        legacy(Items.DARK_OAK_LEAVES, 1L);
        legacy(Items.CHERRY_LEAVES, 1L);
        legacy(Items.MANGROVE_LEAVES, 1L);
        legacy(Items.AZALEA_LEAVES, 1L);
        legacy(Items.FLOWERING_AZALEA_LEAVES, 1L);
        legacy(Items.MANGROVE_ROOTS, 2L);
        legacy(Items.MUDDY_MANGROVE_ROOTS, 4L);
        legacy(Items.COAL, 128L);
        legacy(Items.CHARCOAL, 128L);
        legacy(Items.RAW_IRON, 256L);
        legacy(Items.RAW_GOLD, 2048L);
        legacy(Items.RAW_COPPER, 128L);
        legacy(Items.IRON_INGOT, 256L);
        legacy(Items.GOLD_INGOT, 2048L);
        legacy(Items.COPPER_INGOT, 128L);
        legacy(Items.REDSTONE, 64L);
        legacy(Items.LAPIS_LAZULI, 64L);
        legacy(Items.DIAMOND, 8192L);
        legacy(Items.EMERALD, 16384L);
        legacy(Items.NETHERITE_INGOT, 57344L);
        legacy(Items.NETHERITE_SCRAP, 12288L);
        legacy(Items.QUARTZ, 256L);
        legacy(Items.GLOWSTONE_DUST, 384L);
        legacy(Items.COAL_BLOCK, 1152L);
        legacy(Items.IRON_BLOCK, 2304L);
        legacy(Items.GOLD_BLOCK, 18432L);
        legacy(Items.COPPER_BLOCK, 1152L);
        legacy(Items.REDSTONE_BLOCK, 576L);
        legacy(Items.LAPIS_BLOCK, 576L);
        legacy(Items.DIAMOND_BLOCK, 73728L);
        legacy(Items.EMERALD_BLOCK, 147456L);
        legacy(Items.NETHERITE_BLOCK, 516096L);
        legacy(Items.AMETHYST_SHARD, 32L);
        legacy(Items.AMETHYST_BLOCK, 128L);
        legacy(Items.SMALL_AMETHYST_BUD, 32L);
        legacy(Items.MEDIUM_AMETHYST_BUD, 64L);
        legacy(Items.LARGE_AMETHYST_BUD, 96L);
        legacy(Items.AMETHYST_CLUSTER, 128L);
        legacy(Items.EXPOSED_COPPER, 1152L);
        legacy(Items.WEATHERED_COPPER, 1152L);
        legacy(Items.OXIDIZED_COPPER, 1152L);
        legacy(Items.ROTTEN_FLESH, 24L);
        legacy(Items.BONE, 144L);
        legacy(Items.GUNPOWDER, 192L);
        legacy(Items.STRING, 12L);
        legacy(Items.SPIDER_EYE, 128L);
        legacy(Items.FEATHER, 48L);
        legacy(Items.LEATHER, 64L);
        legacy(Items.SLIME_BALL, 32L);
        legacy(Items.ENDER_PEARL, 1024L);
        legacy(Items.BLAZE_ROD, 1536L);
        legacy(Items.GHAST_TEAR, 4096L);
        legacy(Items.NETHER_STAR, EmcEconomyProfile.DEFAULT_NETHER_STAR);
        legacy(Items.SADDLE, 192L);
        legacy(Items.NAME_TAG, 192L);
        legacy(Items.ELYTRA, EmcEconomyProfile.DEFAULT_ELYTRA);
        legacy(Items.SCUTE, 128L);
        legacy(Items.BELL, 1024L);
        legacy(Items.DRAGON_EGG, EmcEconomyProfile.DEFAULT_DRAGON_EGG);
        legacy(Items.ECHO_SHARD, 2048L);
        legacy(Items.TOTEM_OF_UNDYING, EmcEconomyProfile.DEFAULT_TOTEM_OF_UNDYING);
        legacy(Items.TRIDENT, EmcEconomyProfile.DEFAULT_TRIDENT);
        legacy(Items.PHANTOM_MEMBRANE, 192L);
        legacy(Items.EXPERIENCE_BOTTLE, 128L);
        legacy(Items.SHULKER_SHELL, 1024L);
        legacy(Items.DRAGON_BREATH, 1024L);
        legacy(Items.POTION, 64L);
        legacy(Items.SPLASH_POTION, 96L);
        legacy(Items.LINGERING_POTION, 128L);
        legacy(Items.TIPPED_ARROW, 32L);
        legacy(Items.WRITTEN_BOOK, 96L);
        legacy(Items.ENCHANTED_BOOK, 2048L);
        legacy(Items.DIAMOND_HORSE_ARMOR, EmcEconomyProfile.DEFAULT_DIAMOND_HORSE_ARMOR);
        legacy(Items.CHIPPED_ANVIL, 7936L);
        legacy(Items.DAMAGED_ANVIL, 7936L);
        legacy(Items.FIREWORK_STAR, 256L);
        legacy(Items.SUSPICIOUS_STEW, 96L);
        legacy(Items.GOAT_HORN, 256L);
        legacy(Items.SKELETON_SKULL, 1024L);
        legacy(Items.WITHER_SKELETON_SKULL, EmcEconomyProfile.DEFAULT_WITHER_SKELETON_SKULL);
        legacy(Items.PLAYER_HEAD, 1024L);
        legacy(Items.ZOMBIE_HEAD, 1024L);
        legacy(Items.CREEPER_HEAD, 1024L);
        legacy(Items.DRAGON_HEAD, 4096L);
        legacy(Items.PIGLIN_HEAD, 1024L);
        legacy(Items.WHEAT, 24L);
        legacy(Items.BREAD, 72L);
        legacy(Items.APPLE, 128L);
        legacy(Items.GOLDEN_APPLE, EmcEconomyProfile.DEFAULT_GOLDEN_APPLE);
        legacy(Items.ENCHANTED_GOLDEN_APPLE, EmcEconomyProfile.DEFAULT_ENCHANTED_GOLDEN_APPLE);
        legacy(Items.PORKCHOP, 64L);
        legacy(Items.COOKED_PORKCHOP, 64L);
        legacy(Items.BEEF, 64L);
        legacy(Items.COOKED_BEEF, 64L);
        legacy(Items.CHICKEN, 64L);
        legacy(Items.COOKED_CHICKEN, 64L);
        legacy(Items.MUTTON, 64L);
        legacy(Items.COOKED_MUTTON, 64L);
        legacy(Items.SUGAR_CANE, 32L);
        legacy(Items.KELP, 1L);
        legacy(Items.BAMBOO, 8L);
        legacy(Items.CACTUS, 16L);
        legacy(Items.NETHER_WART, 24L);
        legacy(Items.CRIMSON_FUNGUS, 32L);
        legacy(Items.WARPED_FUNGUS, 32L);
        legacy(Items.BROWN_MUSHROOM, 32L);
        legacy(Items.RED_MUSHROOM, 32L);
        legacy(Items.MOSS_BLOCK, 12L);
        legacy(Items.CHORUS_FRUIT, 192L);
        legacy(Items.WHEAT_SEEDS, 16L);
        legacy(Items.BEETROOT_SEEDS, 16L);
        legacy(Items.MELON_SEEDS, 16L);
        legacy(Items.PUMPKIN_SEEDS, 16L);
        legacy(Items.CARROT, 64L);
        legacy(Items.POTATO, 64L);
        legacy(Items.POISONOUS_POTATO, 64L);
        legacy(Items.COCOA_BEANS, 16L);
        legacy(Items.HONEYCOMB, 16L);
        legacy(Items.INK_SAC, 16L);
        legacy(Items.GLOW_INK_SAC, 16L);
        legacy(Items.SWEET_BERRIES, 16L);
        legacy(Items.GLOW_BERRIES, 16L);
        legacy(Items.RABBIT, 64L);
        legacy(Items.COOKED_RABBIT, 64L);
        legacy(Items.RABBIT_FOOT, 128L);
        legacy(Items.EGG, 16L);
        legacy(Items.BEE_NEST, 64L);
        legacy(Items.SPONGE, 128L);
        legacy(Items.WET_SPONGE, 128L);
        legacy(Items.COBWEB, 128L);
        legacy(Items.NAUTILUS_SHELL, 1024L);
        legacy(Items.HEART_OF_THE_SEA, 32768L);
        legacy(Items.PRISMARINE_SHARD, 256L);
        legacy(Items.PRISMARINE_CRYSTALS, 512L);
        legacy(Items.SEA_PICKLE, 16L);
        legacy(Items.ICE, 1L);
        legacy(Items.PACKED_ICE, 9L);
        legacy(Items.BLUE_ICE, 81L);
        legacy(Items.SNOWBALL, 1L);
        legacy(Items.SNOW, 1L);
        legacy(Items.TUBE_CORAL, 16L);
        legacy(Items.BRAIN_CORAL, 16L);
        legacy(Items.BUBBLE_CORAL, 16L);
        legacy(Items.FIRE_CORAL, 16L);
        legacy(Items.HORN_CORAL, 16L);
        legacy(Items.TUBE_CORAL_FAN, 16L);
        legacy(Items.BRAIN_CORAL_FAN, 16L);
        legacy(Items.BUBBLE_CORAL_FAN, 16L);
        legacy(Items.FIRE_CORAL_FAN, 16L);
        legacy(Items.HORN_CORAL_FAN, 16L);
        legacy(Items.TUBE_CORAL_BLOCK, 64L);
        legacy(Items.BRAIN_CORAL_BLOCK, 64L);
        legacy(Items.BUBBLE_CORAL_BLOCK, 64L);
        legacy(Items.FIRE_CORAL_BLOCK, 64L);
        legacy(Items.HORN_CORAL_BLOCK, 64L);
        legacy(Items.DEAD_TUBE_CORAL, 4L);
        legacy(Items.DEAD_BRAIN_CORAL, 4L);
        legacy(Items.DEAD_BUBBLE_CORAL, 4L);
        legacy(Items.DEAD_FIRE_CORAL, 4L);
        legacy(Items.DEAD_HORN_CORAL, 4L);
        legacy(Items.DEAD_TUBE_CORAL_FAN, 4L);
        legacy(Items.DEAD_BRAIN_CORAL_FAN, 4L);
        legacy(Items.DEAD_BUBBLE_CORAL_FAN, 4L);
        legacy(Items.DEAD_FIRE_CORAL_FAN, 4L);
        legacy(Items.DEAD_HORN_CORAL_FAN, 4L);
        legacy(Items.DEAD_TUBE_CORAL_BLOCK, 16L);
        legacy(Items.DEAD_BRAIN_CORAL_BLOCK, 16L);
        legacy(Items.DEAD_BUBBLE_CORAL_BLOCK, 16L);
        legacy(Items.DEAD_FIRE_CORAL_BLOCK, 16L);
        legacy(Items.DEAD_HORN_CORAL_BLOCK, 16L);
        legacy(Items.DANDELION, 16L);
        legacy(Items.POPPY, 16L);
        legacy(Items.BLUE_ORCHID, 16L);
        legacy(Items.ALLIUM, 16L);
        legacy(Items.AZURE_BLUET, 16L);
        legacy(Items.RED_TULIP, 16L);
        legacy(Items.ORANGE_TULIP, 16L);
        legacy(Items.WHITE_TULIP, 16L);
        legacy(Items.PINK_TULIP, 16L);
        legacy(Items.OXEYE_DAISY, 16L);
        legacy(Items.CORNFLOWER, 16L);
        legacy(Items.LILY_OF_THE_VALLEY, 16L);
        legacy(Items.WITHER_ROSE, 2048L);
        legacy(Items.WHITE_DYE, 16L);
        legacy(Items.LIGHT_GRAY_DYE, 16L);
        legacy(Items.GRAY_DYE, 16L);
        legacy(Items.BLACK_DYE, 16L);
        legacy(Items.RED_DYE, 16L);
        legacy(Items.ORANGE_DYE, 16L);
        legacy(Items.YELLOW_DYE, 16L);
        legacy(Items.LIME_DYE, 16L);
        legacy(Items.GREEN_DYE, 16L);
        legacy(Items.CYAN_DYE, 16L);
        legacy(Items.LIGHT_BLUE_DYE, 16L);
        legacy(Items.BLUE_DYE, 16L);
        legacy(Items.PURPLE_DYE, 16L);
        legacy(Items.MAGENTA_DYE, 16L);
        legacy(Items.PINK_DYE, 16L);
        legacy(Items.BROWN_DYE, 16L);
        legacy(Items.TORCHFLOWER, 32L);
        legacy(Items.SPORE_BLOSSOM, 128L);
        legacy(Items.PITCHER_POD, 32L);
        legacy(Items.TORCHFLOWER_SEEDS, 16L);
        legacy(Items.SUNFLOWER, 32L);
        legacy(Items.LILAC, 32L);
        legacy(Items.ROSE_BUSH, 32L);
        legacy(Items.PEONY, 32L);
        legacy(Items.TALL_GRASS, 2L);
        legacy(Items.LARGE_FERN, 2L);
        legacy(Items.SEAGRASS, 2L);
        legacy(Items.GRASS, 1L);
        legacy(Items.FERN, 1L);
        legacy(Items.DEAD_BUSH, 1L);
        legacy(Items.AZALEA, 32L);
        legacy(Items.FLOWERING_AZALEA, 48L);
        legacy(Items.VINE, 8L);
        legacy(Items.GLOW_LICHEN, 8L);
        legacy(Items.CRIMSON_ROOTS, 2L);
        legacy(Items.WARPED_ROOTS, 2L);
        legacy(Items.NETHER_SPROUTS, 2L);
        legacy(Items.WEEPING_VINES, 4L);
        legacy(Items.TWISTING_VINES, 4L);
        legacy(Items.HANGING_ROOTS, 2L);
        legacy(Items.BIG_DRIPLEAF, 16L);
        legacy(Items.SMALL_DRIPLEAF, 16L);
        legacy(Items.PINK_PETALS, 16L);
        legacy(Items.BROWN_MUSHROOM_BLOCK, 8L);
        legacy(Items.RED_MUSHROOM_BLOCK, 8L);
        legacy(Items.MUSHROOM_STEM, 8L);
        legacy(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 57345L);
        legacy(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        legacy(Items.FLOWER_BANNER_PATTERN, 256L);
        legacy(Items.CREEPER_BANNER_PATTERN, 256L);
        legacy(Items.SKULL_BANNER_PATTERN, 256L);
        legacy(Items.MOJANG_BANNER_PATTERN, 256L);
        legacy(Items.GLOBE_BANNER_PATTERN, 256L);
        legacy(Items.PIGLIN_BANNER_PATTERN, 256L);
        legacy(Items.ANGLER_POTTERY_SHERD, 128L);
        legacy(Items.ARCHER_POTTERY_SHERD, 128L);
        legacy(Items.ARMS_UP_POTTERY_SHERD, 128L);
        legacy(Items.BLADE_POTTERY_SHERD, 128L);
        legacy(Items.BREWER_POTTERY_SHERD, 128L);
        legacy(Items.BURN_POTTERY_SHERD, 128L);
        legacy(Items.DANGER_POTTERY_SHERD, 128L);
        legacy(Items.EXPLORER_POTTERY_SHERD, 128L);
        legacy(Items.FRIEND_POTTERY_SHERD, 128L);
        legacy(Items.HEART_POTTERY_SHERD, 128L);
        legacy(Items.HEARTBREAK_POTTERY_SHERD, 128L);
        legacy(Items.HOWL_POTTERY_SHERD, 128L);
        legacy(Items.MINER_POTTERY_SHERD, 128L);
        legacy(Items.MOURNER_POTTERY_SHERD, 128L);
        legacy(Items.PLENTY_POTTERY_SHERD, 128L);
        legacy(Items.PRIZE_POTTERY_SHERD, 128L);
        legacy(Items.SHEAF_POTTERY_SHERD, 128L);
        legacy(Items.SHELTER_POTTERY_SHERD, 128L);
        legacy(Items.SKULL_POTTERY_SHERD, 128L);
        legacy(Items.SNORT_POTTERY_SHERD, 128L);
        legacy(Items.MUSIC_DISC_13, 2048L);
        legacy(Items.MUSIC_DISC_CAT, 2048L);
        legacy(Items.MUSIC_DISC_BLOCKS, 2048L);
        legacy(Items.MUSIC_DISC_CHIRP, 2048L);
        legacy(Items.MUSIC_DISC_FAR, 2048L);
        legacy(Items.MUSIC_DISC_MALL, 2048L);
        legacy(Items.MUSIC_DISC_MELLOHI, 2048L);
        legacy(Items.MUSIC_DISC_STAL, 2048L);
        legacy(Items.MUSIC_DISC_STRAD, 2048L);
        legacy(Items.MUSIC_DISC_WARD, 2048L);
        legacy(Items.MUSIC_DISC_11, 2048L);
        legacy(Items.MUSIC_DISC_WAIT, 2048L);
        legacy(Items.MUSIC_DISC_OTHERSIDE, 2048L);
        legacy(Items.MUSIC_DISC_RELIC, 2048L);
        legacy(Items.MUSIC_DISC_5, 2048L);
        legacy(Items.MUSIC_DISC_PIGSTEP, 2048L);
        legacy(Items.DISC_FRAGMENT_5, 256L);
        legacy(Items.OCHRE_FROGLIGHT, 1024L);
        legacy(Items.VERDANT_FROGLIGHT, 1024L);
        legacy(Items.PEARLESCENT_FROGLIGHT, 1024L);
        legacy(Items.NETHERITE_SWORD, 131077L);
        legacy(Items.NETHERITE_SHOVEL, 122889L);
        legacy(Items.NETHERITE_PICKAXE, 139273L);
        legacy(Items.NETHERITE_AXE, 139273L);
        legacy(Items.NETHERITE_HOE, 131081L);
        legacy(Items.NETHERITE_HELMET, 155649L);
        legacy(Items.NETHERITE_CHESTPLATE, 180225L);
        legacy(Items.NETHERITE_LEGGINGS, 172033L);
        legacy(Items.NETHERITE_BOOTS, 147457L);
        legacy(Items.CHORUS_PLANT, 64L);
        legacy(Items.CHORUS_FLOWER, 96L);
        legacy(Items.CARVED_PUMPKIN, 16L);
        legacy(Items.JACK_O_LANTERN, 24L);
        legacy(Items.LILY_PAD, 16L);
        legacy(Items.SCULK, 4L);
        legacy(Items.SCULK_VEIN, 1L);
        legacy(Items.SCULK_CATALYST, 128L);
        legacy(Items.SCULK_SENSOR, 256L);
        legacy(Items.CALIBRATED_SCULK_SENSOR, 512L);
        legacy(Items.SCULK_SHRIEKER, 512L);
        legacy(Items.END_STONE, 4L);
        legacy(Items.END_STONE_BRICKS, 4L);
        legacy(Items.TURTLE_EGG, 192L);
        legacy(Items.SNIFFER_EGG, 1024L);
        legacy(Items.WATER_BUCKET, 768L);
        legacy(Items.LAVA_BUCKET, 832L);
        legacy(Items.POWDER_SNOW_BUCKET, 768L);
        legacy(Items.MILK_BUCKET, 832L);
        legacy(Items.PUFFERFISH_BUCKET, 832L);
        legacy(Items.SALMON_BUCKET, 832L);
        legacy(Items.COD_BUCKET, 832L);
        legacy(Items.TROPICAL_FISH_BUCKET, 832L);
        legacy(Items.COD, 64L);
        legacy(Items.COOKED_COD, 64L);
        legacy(Items.SALMON, 64L);
        legacy(Items.COOKED_SALMON, 64L);
        legacy(Items.TROPICAL_FISH, 64L);
        legacy(Items.PUFFERFISH, 64L);
        legacy(Items.CAKE, 1024L);
        legacy(Items.SHROOMLIGHT, 64L);
        legacy(Items.WARPED_WART_BLOCK, 96L);
    }

    private static void balancedSource(Item item, long emc) {
        if (item != null && item != Items.AIR && emc > 0L) BALANCED_SOURCE_MAP.put(item, emc);
    }

    private static void sharedSource(Item item, long emc) {
        if (item != null && item != Items.AIR && emc > 0L) SHARED_SOURCE_MAP.put(item, emc);
    }

    private static void rebuildStaticSources(EmcValueMode mode) {
        activeValueMode = mode == null ? EmcValueMode.DEFAULT : mode;
        STATIC_EMC_MAP.clear();
        if (activeValueMode == EmcValueMode.BALANCED) STATIC_EMC_MAP.putAll(BALANCED_SOURCE_MAP);
        else STATIC_EMC_MAP.putAll(LEGACY_FALLBACK_MAP);
        STATIC_EMC_MAP.putAll(SHARED_SOURCE_MAP);
        STATIC_EMC_MAP.keySet().removeIf(EMCRegistry::isHardBlocked);
    }

    public static boolean isHardBlocked(Item item) {
        if (item == null || item == Items.AIR) return true;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null
            && "minecraft".equals(id.getNamespace())
            && EmcEconomyProfile.isNonTransmutablePath(id.getPath());
    }

    private static File getRecipeCacheFile() {
        return new File("config/velorise_emc/recipe_emc_cache_" + activeValueMode.name().toLowerCase(java.util.Locale.ROOT) + "_v7.json");
    }

    private static File getUnresolvedReportFile() {
        return new File("config/velorise_emc/unresolved_emc_items_" + activeValueMode.name().toLowerCase(java.util.Locale.ROOT) + ".txt");
    }

    private static EmcValueMode readValueMode() {
        if (!VALUE_MODE_FILE.exists()) return EmcValueMode.DEFAULT;
        try (FileReader reader = new FileReader(VALUE_MODE_FILE)) {
            Map<String, String> data = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            return data == null ? EmcValueMode.DEFAULT : EmcValueMode.byName(data.get("mode"));
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to load EMC value mode: " + e.getMessage());
            return EmcValueMode.DEFAULT;
        }
    }

    private static void saveValueMode() {
        try {
            File parent = VALUE_MODE_FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            Map<String, String> data = new LinkedHashMap<>();
            data.put("mode", configuredValueMode.name().toLowerCase(java.util.Locale.ROOT));
            try (FileWriter writer = new FileWriter(VALUE_MODE_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to save EMC value mode: " + e.getMessage());
        }
    }

    public static EmcValueMode getConfiguredValueMode() {
        return configuredValueMode;
    }

    public static EmcValueMode getEffectiveValueMode() {
        return clientSyncedValueMode != null ? clientSyncedValueMode : configuredValueMode;
    }

    public static void setConfiguredValueMode(EmcValueMode mode) {
        configuredValueMode = mode == null ? EmcValueMode.DEFAULT : mode;
        rebuildStaticSources(configuredValueMode);

        // Keep main-menu configuration coherent even before a world recipe manager is available.
        DEFAULT_EMC_MAP.clear();
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);
        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        loadRecipeCache();
        loadCustomEMC();
        loadItemPolicies();
        saveValueMode();
    }

    public static void setClientSyncedValueMode(EmcValueMode mode) {
        clientSyncedValueMode = mode;
        rebuildStaticSources(getEffectiveValueMode());
    }

    private static void legacy(Item item, long emc) {
        if (item != null && item != Items.AIR && emc > 0L) LEGACY_FALLBACK_MAP.put(item, emc);
    }

    public static long getDefaultEMC(Item item) {
        return DEFAULT_EMC_MAP.getOrDefault(item, 0L);
    }

    public static void setClientSyncedOverrides(Map<Item, Long> overrides) {
        clientSyncedOverrides = overrides == null ? null : new HashMap<>(overrides);
    }

    public static Map<Item, Long> getEffectiveCustomOverrides() {
        return clientSyncedOverrides != null ? new HashMap<>(clientSyncedOverrides) : readCustomOverrides();
    }

    public static void clearClientSyncedOverrides() {
        clientSyncedOverrides = null;
        clientSyncedPolicyOverrides = null;
        clientSyncedValueMode = null;
        rebuildStaticSources(configuredValueMode);
    }

    public static void setClientSyncedPolicyOverrides(Map<Item, EmcPolicyOverride> overrides) {
        clientSyncedPolicyOverrides = overrides == null ? null : new HashMap<>(overrides);
    }

    public static void loadItemPolicies() {
        SERVER_POLICY_OVERRIDES.clear();
        SERVER_POLICY_OVERRIDES.putAll(readItemPolicies());
    }

    private static Map<Item, EmcPolicyOverride> readItemPolicies() {
        Map<Item, EmcPolicyOverride> result = new HashMap<>();
        if (!ITEM_POLICY_FILE.exists()) return result;
        try (FileReader reader = new FileReader(ITEM_POLICY_FILE)) {
            Map<String, EmcPolicyOverride> map = GSON.fromJson(
                reader, new TypeToken<Map<String, EmcPolicyOverride>>() {}.getType());
            if (map != null) {
                map.forEach((key, override) -> {
                    ResourceLocation id = ResourceLocation.tryParse(key);
                    if (id == null || override == null || override.isInherited()) return;
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item != null && item != Items.AIR && !isHardBlocked(item)) {
                        result.put(item, override);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to load EMC item policies: " + e.getMessage());
        }
        return result;
    }

    public static void replaceItemPolicies(Map<Item, EmcPolicyOverride> overrides) {
        try {
            File parent = ITEM_POLICY_FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            Map<String, EmcPolicyOverride> jsonMap = new java.util.TreeMap<>();
            if (overrides != null) {
                for (Map.Entry<Item, EmcPolicyOverride> entry : overrides.entrySet()) {
                    Item item = entry.getKey();
                    EmcPolicyOverride override = entry.getValue();
                    if (item == null || item == Items.AIR || isHardBlocked(item)
                        || override == null || override.isInherited()) continue;
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null) jsonMap.put(id.toString(), override);
                }
            }
            try (FileWriter writer = new FileWriter(ITEM_POLICY_FILE)) {
                GSON.toJson(jsonMap, writer);
            }
            loadItemPolicies();
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to save EMC item policies: " + e.getMessage());
        }
    }

    public static Map<ResourceLocation, EmcPolicyOverride> getFullPolicyOverrides() {
        Map<ResourceLocation, EmcPolicyOverride> result = new HashMap<>();
        readItemPolicies().forEach((item, override) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) result.put(id, override);
        });
        return result;
    }

    public static Map<Item, EmcPolicyOverride> getEffectivePolicyOverrides() {
        Map<Item, EmcPolicyOverride> source = clientSyncedPolicyOverrides != null
            ? clientSyncedPolicyOverrides : SERVER_POLICY_OVERRIDES;
        return new HashMap<>(source);
    }

    public static EmcPolicyOverride getPolicyOverride(Item item) {
        if (item == null || item == Items.AIR) return EmcPolicyOverride.inherit();
        Map<Item, EmcPolicyOverride> source = clientSyncedPolicyOverrides != null
            ? clientSyncedPolicyOverrides : SERVER_POLICY_OVERRIDES;
        return source.getOrDefault(item, EmcPolicyOverride.inherit());
    }

    public static EmcItemPolicy getPolicy(Item item) {
        if (item == null || item == Items.AIR) return EmcItemPolicy.disabled("Invalid item");
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String itemId = id == null ? "" : id.toString();
        return EmcPolicyProfile.resolve(
            getEffectiveValueMode(), itemId, isHardBlocked(item), getEMC(item) > 0L, getPolicyOverride(item));
    }

    public static boolean canBurn(Item item) { return getPolicy(item).burnable(); }
    public static boolean canLearn(Item item) { return getPolicy(item).learnable(); }
    public static boolean canMaterialize(Item item) { return getPolicy(item).materializable(); }
    public static boolean isVisibleInTransmutation(Item item) { return getPolicy(item).visible(); }

    public static boolean isBalancedRestricted(Item item) {
        ResourceLocation id = item == null ? null : BuiltInRegistries.ITEM.getKey(item);
        return id != null && EmcPolicyProfile.isBalancedRestricted(id.toString());
    }

    public static void loadCustomEMC() {
        CUSTOM_OVERRIDE_ITEMS.clear();
        loadOverridesInto(EMC_MAP, CUSTOM_OVERRIDE_ITEMS, readCustomOverrides());
    }

    private static Map<Item, Long> readCustomOverrides() {
        Map<Item, Long> result = new HashMap<>();
        if (!CUSTOM_EMC_FILE.exists()) return result;
        try (FileReader reader = new FileReader(CUSTOM_EMC_FILE)) {
            Map<String, Long> customMap = GSON.fromJson(reader, new TypeToken<Map<String, Long>>() {}.getType());
            if (customMap != null) {
                customMap.forEach((key, value) -> {
                    ResourceLocation id = ResourceLocation.tryParse(key);
                    if (id == null || value == null || value <= 0L) return;
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item != null && item != Items.AIR) result.put(item, value);
                });
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to load custom EMC: " + e.getMessage());
        }
        return result;
    }

    private static void loadOverridesInto(Map<Item, Long> target, Set<Item> locked, Map<Item, Long> overrides) {
        overrides.forEach((item, value) -> {
            if (!isHardBlocked(item) && value != null && value > 0L) {
                target.put(item, value);
                locked.add(item);
            }
        });
    }

    public static void loadRecipeCache() {
        if (!getRecipeCacheFile().exists()) return;
        try (FileReader reader = new FileReader(getRecipeCacheFile())) {
            Map<String, Long> cacheMap = GSON.fromJson(reader, new TypeToken<Map<String, Long>>() {}.getType());
            if (cacheMap == null) return;
            cacheMap.forEach((key, value) -> {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null || value == null || value <= 0L) return;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null && item != Items.AIR && !isHardBlocked(item) && !STATIC_EMC_MAP.containsKey(item)) {
                    EMC_MAP.put(item, value);
                }
            });
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to load recipe EMC cache v4: " + e.getMessage());
        }
    }

    private static void saveRecipeCache() {
        try {
            File cacheFile = getRecipeCacheFile();
            File parent = cacheFile.getParentFile();
            if (parent != null) parent.mkdirs();
            Map<String, Long> cache = new java.util.TreeMap<>();
            for (Map.Entry<Item, Long> entry : EMC_MAP.entrySet()) {
                if (STATIC_EMC_MAP.containsKey(entry.getKey()) || CUSTOM_OVERRIDE_ITEMS.contains(entry.getKey())) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
                if (id != null && entry.getValue() > 0L) cache.put(id.toString(), entry.getValue());
            }
            try (FileWriter writer = new FileWriter(cacheFile)) {
                GSON.toJson(cache, writer);
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to save recipe EMC cache v4: " + e.getMessage());
        }
    }

    public static void saveCustomEMC(Map<Item, Long> overrides) {
        try {
            File parent = CUSTOM_EMC_FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            Map<String, Long> jsonMap = new java.util.TreeMap<>();
            if (CUSTOM_EMC_FILE.exists()) {
                try (FileReader reader = new FileReader(CUSTOM_EMC_FILE)) {
                    Map<String, Long> existing = GSON.fromJson(reader, new TypeToken<Map<String, Long>>() {}.getType());
                    if (existing != null) jsonMap.putAll(existing);
                } catch (Exception ignored) {}
            }
            for (Map.Entry<Item, Long> entry : overrides.entrySet()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
                if (id == null) continue;
                long value = entry.getValue() == null ? 0L : entry.getValue();
                long defaultValue = getDefaultEMC(entry.getKey());
                if (value <= 0L || value == defaultValue) jsonMap.remove(id.toString());
                else jsonMap.put(id.toString(), value);
            }
            try (FileWriter writer = new FileWriter(CUSTOM_EMC_FILE)) {
                GSON.toJson(jsonMap, writer);
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to save custom EMC: " + e.getMessage());
        }
    }

    /** Replaces every custom override with the supplied set for the currently selected value mode. */
    public static void replaceCustomEMC(Map<Item, Long> overrides) {
        try {
            File parent = CUSTOM_EMC_FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            Map<String, Long> jsonMap = new java.util.TreeMap<>();
            if (overrides != null) {
                for (Map.Entry<Item, Long> entry : overrides.entrySet()) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
                    if (id == null) continue;
                    long value = entry.getValue() == null ? 0L : entry.getValue();
                    long defaultValue = getDefaultEMC(entry.getKey());
                    if (value > 0L && value != defaultValue) jsonMap.put(id.toString(), value);
                }
            }
            try (FileWriter writer = new FileWriter(CUSTOM_EMC_FILE)) {
                GSON.toJson(jsonMap, writer);
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to replace custom EMC: " + e.getMessage());
        }
    }

    public static void reloadAndRecalculate(RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        loadItemPolicies();
        rebuildStaticSources(configuredValueMode);
        DEFAULT_EMC_MAP.clear();
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);
        calculateRecipeEMCForMap(DEFAULT_EMC_MAP, recipeManager, registries, false, Set.of());

        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        CUSTOM_OVERRIDE_ITEMS.clear();
        loadOverridesInto(EMC_MAP, CUSTOM_OVERRIDE_ITEMS, readCustomOverrides());
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, true, CUSTOM_OVERRIDE_ITEMS);
    }

    public static void clientReloadAndRecalculate(RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        rebuildStaticSources(getEffectiveValueMode());
        DEFAULT_EMC_MAP.clear();
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);
        calculateRecipeEMCForMap(DEFAULT_EMC_MAP, recipeManager, registries, false, Set.of());

        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        Set<Item> locked = new HashSet<>();
        if (clientSyncedOverrides != null) loadOverridesInto(EMC_MAP, locked, clientSyncedOverrides);
        else loadOverridesInto(EMC_MAP, locked, readCustomOverrides());
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, false, locked);
    }

    public static void applyCustomOverridesAndRecalculate(Map<Item, Long> overrides, RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        Set<Item> locked = new HashSet<>();
        loadOverridesInto(EMC_MAP, locked, overrides);
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, false, locked);
    }

    public static Map<ResourceLocation, Long> getFullCustomOverrides() {
        Map<ResourceLocation, Long> result = new HashMap<>();
        readCustomOverrides().forEach((item, value) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) result.put(id, value);
        });
        return result;
    }

    /** Compatibility method. New code should use explicit source registration internally. */
    public static void register(Item item, long emc) {
        sharedSource(item, emc);
        STATIC_EMC_MAP.put(item, emc);
        EMC_MAP.put(item, emc);
    }

    public static long getEMC(Item item) {
        if (isHardBlocked(item)) return 0L;
        return EMC_MAP.getOrDefault(item, 0L);
    }

    /**
     * Returns the EMC value of one item in this exact stack. Vanilla stacks upgraded through
     * a Wireless Core retain their Vanilla registry id, so their stored upgrade marker must
     * contribute the Core value here rather than in the item-only registry map.
     */
    public static long getUnitEMC(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;

        long registryValue = getEMC(stack.getItem());
        WirelessCoreTier tier = CoreTierData.get(stack);
        long tieredCoreValue = getWirelessCoreValue(tier);
        long coreOneValue = getWirelessCoreValue(WirelessCoreTier.I);

        if (CoreTierData.isCoreItem(stack)) return tieredCoreValue;

        boolean vanillaIdUpgrade = ImbuedEquipmentUtil.isImbued(stack)
            || SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.BOUND_COMPASS)
            || SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.SOULBOUND_RECOVERY_COMPASS)
            || SpecialUpgradeUtil.is(stack, SpecialUpgradeUtil.FAR_SIGHT_SPYGLASS);

        long bodyValue = registryValue;
        long embeddedCoreValue = 0L;
        if (vanillaIdUpgrade) {
            embeddedCoreValue = tieredCoreValue;
        } else if (CoreTierData.hasTier(stack)) {
            // Registered upgraded items resolve with Core I already included in their recipe value.
            // Separate that Core before applying durability so a damaged machine/tool never destroys
            // the recoverable Core value, then add the actual installed tier back in full.
            bodyValue = Math.max(0L, registryValue - coreOneValue);
            embeddedCoreValue = tieredCoreValue;
        }

        long durableBodyValue = durabilityAdjustedValue(bodyValue, stack);
        long storedEmc = storedEmcValue(stack);
        return saturatingAdd(saturatingAdd(durableBodyValue, embeddedCoreValue), storedEmc);
    }

    private static long durabilityAdjustedValue(long value, ItemStack stack) {
        if (value <= 0L || stack == null || !stack.isDamageableItem()) return Math.max(0L, value);
        int maximum = stack.getMaxDamage();
        if (maximum <= 0) return value;
        int remaining = Math.max(0, maximum - Math.max(0, stack.getDamageValue()));
        if (remaining <= 0) return 0L;
        long whole = value / maximum;
        long remainder = value % maximum;
        return whole * remaining + (remainder * remaining) / maximum;
    }

    /** EMC physically stored in the burned stack is refunded in full and is not durability-scaled. */
    private static long storedEmcValue(ItemStack stack) {
        if (stack.getItem() instanceof ChaliceOfTransmutationItem) {
            return ChaliceOfTransmutationItem.getStoredEMC(stack);
        }
        if (stack.getItem() instanceof AttractionCatalystItem) {
            return AttractionCatalystItem.getStoredEMC(stack);
        }
        if (stack.getItem() instanceof VoidTranslocatorItem) {
            return VoidTranslocatorItem.getStoredEMC(stack);
        }

        CompoundTag blockData = BlockItem.getBlockEntityData(stack);
        if (blockData == null) return 0L;
        long stored = Math.max(0L, blockData.getLong("StoredEMC"));
        stored = Math.max(stored, Math.max(0L, blockData.getLong("StoredEmc")));
        return Math.max(stored, Math.max(0L, blockData.getLong("emc")));
    }

    private static long getWirelessCoreValue(WirelessCoreTier tier) {
        return getEMC(CoreTierData.coreItem(tier == null ? WirelessCoreTier.I : tier));
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long saturatingMultiply(long value, long multiplier) {
        if (value <= 0L || multiplier <= 0L) return 0L;
        return value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
    }

    public static long getEMC(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        long each = getUnitEMC(stack);
        return each > Long.MAX_VALUE / Math.max(1, stack.getCount()) ? Long.MAX_VALUE : each * stack.getCount();
    }

    public static boolean hasEMC(Item item) {
        return item != null && item != Items.AIR && getEMC(item) > 0L;
    }

    public static boolean hasEMC(ItemStack stack) {
        return stack != null && !stack.isEmpty() && getUnitEMC(stack) > 0L;
    }

    public static boolean hasRecipeCache() {
        return getRecipeCacheFile().exists();
    }

    public static Map<Item, Long> getEMCMap() {
        return EMC_MAP;
    }

    public static Set<Item> getAllItems() {
        return EMC_MAP.keySet();
    }

    public static void calculateAllRecipeEMC(RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, true, CUSTOM_OVERRIDE_ITEMS);
    }

    public static void calculateRecipeEMCForMap(Map<Item, Long> map, RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries, boolean shouldSaveCache) {
        calculateRecipeEMCForMap(map, recipeManager, registries, shouldSaveCache, Set.of());
    }

    private static void calculateRecipeEMCForMap(Map<Item, Long> map, RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries,
                                                  boolean shouldSaveCache, Set<Item> extraLocked) {
        Set<Item> locked = new HashSet<>(STATIC_EMC_MAP.keySet());
        locked.addAll(extraLocked);
        List<Recipe<?>> allRecipes = new ArrayList<>();
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.CRAFTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.SMELTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.BLASTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.SMOKING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.STONECUTTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.SMITHING));

        boolean changed = true;
        int iteration = 0;
        while (changed && iteration++ < MAX_RESOLVE_ITERATIONS) {
            changed = false;
            for (Recipe<?> recipe : allRecipes) {
                ItemStack resultStack = recipe.getResultItem(registries);
                if (resultStack.isEmpty() || resultStack.getCount() <= 0) continue;
                Item resultItem = resultStack.getItem();
                if (locked.contains(resultItem)) continue;

                // Smithing recipes expose their three slots through SmithingRecipe's
                // predicates. In vanilla 1.20.x getIngredients() is the empty default
                // implementation, which previously broke every derived EMC chain after
                // the directly-crafted Verdant tier.
                long total = recipe instanceof SmithingRecipe smithing
                    ? calculateSmithingInput(smithing, map)
                    : calculateCheapestInput(recipe.getIngredients(), map);
                if (total <= 0L) continue;
                long candidate = Math.max(1L, total / resultStack.getCount());
                long current = map.getOrDefault(resultItem, Long.MAX_VALUE);
                if (candidate < current) {
                    map.put(resultItem, candidate);
                    changed = true;
                }
            }
        }

        applySpecialEquivalences(map, locked);
        applyAnvilUpgradeValues(map, locked);
        List<ResourceLocation> unresolved = applyUniversalFallbacks(map);
        if (shouldSaveCache) {
            saveRecipeCache();
            writeUnresolvedReport(unresolved);
        }
    }

    private static long calculateCheapestInput(List<Ingredient> ingredients, Map<Item, Long> map) {
        long total = 0L;
        boolean foundAny = false;
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            if (isPhilosophersStoneCatalyst(ingredient)) continue;
            long cheapest = Long.MAX_VALUE;
            for (ItemStack option : ingredient.getItems()) {
                long value = map.getOrDefault(option.getItem(), 0L);
                if (value > 0L && value < cheapest) cheapest = value;
            }
            if (cheapest == Long.MAX_VALUE) return 0L;
            if (total > Long.MAX_VALUE - cheapest) total = Long.MAX_VALUE;
            else total += cheapest;
            foundAny = true;
        }
        return foundAny ? total : 0L;
    }

    private static long calculateSmithingInput(SmithingRecipe recipe, Map<Item, Long> map) {
        long template = Long.MAX_VALUE;
        long base = Long.MAX_VALUE;
        long addition = Long.MAX_VALUE;
        for (Map.Entry<Item, Long> entry : map.entrySet()) {
            long value = entry.getValue() == null ? 0L : entry.getValue();
            if (value <= 0L) continue;
            ItemStack stack = entry.getKey().getDefaultInstance();
            if (recipe.isTemplateIngredient(stack) && value < template) template = value;
            if (recipe.isBaseIngredient(stack) && value < base) base = value;
            if (recipe.isAdditionIngredient(stack) && value < addition) addition = value;
        }
        if (template == Long.MAX_VALUE || base == Long.MAX_VALUE || addition == Long.MAX_VALUE) return 0L;
        return saturatingAdd(saturatingAdd(template, base), addition);
    }

    private static boolean isPhilosophersStoneCatalyst(Ingredient ingredient) {
        for (ItemStack option : ingredient.getItems()) {
            if (option.getItem() == VeloriseEMC.PHILOSOPHERS_STONE.get()) return true;
        }
        return false;
    }

    private static void applySpecialEquivalences(Map<Item, Long> map, Set<Item> locked) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !"minecraft".equals(id.getNamespace()) || locked.contains(item)) continue;
            String path = id.getPath();
            if (path.endsWith("_concrete")) {
                Item powder = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("minecraft:" + path + "_powder"));
                long value = map.getOrDefault(powder, 0L);
                if (value > 0L) map.put(item, value);
            }
        }
        long shulker = map.getOrDefault(Items.SHULKER_BOX, 0L);
        if (shulker > 0L) {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null && "minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_shulker_box") && !locked.contains(item)) {
                    map.put(item, shulker);
                }
            }
        }
    }

    /**
     * Direct Vanilla + Wireless Core upgrades are produced by the Anvil rather than recipe JSON.
     * Their EMC therefore has to be derived explicitly after recipe resolution.
     */
    private static void applyAnvilUpgradeValues(Map<Item, Long> map, Set<Item> locked) {
        Item core = VeloriseEMC.WIRELESS_CORE.get();
        setAnvilUpgradeValue(map, locked, VeloriseEMC.RESONANT_FURNACE_ITEM.get(), Items.FURNACE, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.RESONANT_SMOKER_ITEM.get(), Items.SMOKER, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get(), Items.BLAST_FURNACE, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.RESONANCE_JUKEBOX_ITEM.get(), Items.JUKEBOX, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.DEEPBOUND_ENDER_CHEST_ITEM.get(), Items.ENDER_CHEST, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.MATTERBURST_TNT_ITEM.get(), Items.TNT, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.MATTERBURST_MINECART_ITEM.get(), Items.TNT_MINECART, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.ALCHEMICAL_ANVIL_I_ITEM.get(), Items.ANVIL, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.ALCHEMICAL_ANVIL_II_ITEM.get(), VeloriseEMC.ALCHEMICAL_ANVIL_I_ITEM.get(), VeloriseEMC.WIRELESS_CORE_T2.get());
        setAnvilUpgradeValue(map, locked, VeloriseEMC.ALCHEMICAL_ANVIL_III_ITEM.get(), VeloriseEMC.ALCHEMICAL_ANVIL_II_ITEM.get(), VeloriseEMC.WIRELESS_CORE_T3.get());
        setAnvilUpgradeValue(map, locked, VeloriseEMC.ALCHEMICAL_ANVIL_IV_ITEM.get(), VeloriseEMC.ALCHEMICAL_ANVIL_III_ITEM.get(), VeloriseEMC.WIRELESS_CORE_T4.get());
        setAnvilUpgradeValue(map, locked, VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get(), Items.GRINDSTONE, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.TIDAL_TURTLE_HELMET.get(), Items.TURTLE_HELMET, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.BOUND_COMPASS.get(), Items.COMPASS, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.SOULBOUND_RECOVERY_COMPASS.get(), Items.RECOVERY_COMPASS, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.PORTABLE_CRAFTING_TABLE_ITEM.get(), Items.CRAFTING_TABLE, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.PORTABLE_STONECUTTER_ITEM.get(), Items.STONECUTTER, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.PORTABLE_LOOM_ITEM.get(), Items.LOOM, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.PORTABLE_CARTOGRAPHY_TABLE_ITEM.get(), Items.CARTOGRAPHY_TABLE, core);
        setAnvilUpgradeValue(map, locked, VeloriseEMC.PURIFYING_MILK.get(), Items.MILK_BUCKET, core);
        // Arcane weapon bases are shaped recipes; recipe resolution must retain catalyst + Core + Stick.
        setAnvilUpgradeValue(map, locked, VeloriseEMC.CURSED_IGNITER.get(), Items.FLINT_AND_STEEL, core);
    }

    private static void setAnvilUpgradeValue(Map<Item, Long> map, Set<Item> locked, Item output, Item base, Item core) {
        if (output == null || locked.contains(output)) return;
        long baseValue = map.getOrDefault(base, 0L);
        long coreValue = map.getOrDefault(core, 0L);
        if (baseValue <= 0L || coreValue <= 0L) return;
        long value = baseValue > Long.MAX_VALUE - coreValue ? Long.MAX_VALUE : baseValue + coreValue;
        map.put(output, Math.max(1L, value));
    }

    private static List<ResourceLocation> applyUniversalFallbacks(Map<Item, Long> map) {
        List<ResourceLocation> unresolved = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || isHardBlocked(item) || map.getOrDefault(item, 0L) > 0L) continue;
            String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
            if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae")) {
                map.put(item, 32L);
                continue;
            }
            if (path.endsWith("_planks")) {
                map.put(item, 8L);
                continue;
            }
            if (path.endsWith("_sapling")) {
                map.put(item, 32L);
                continue;
            }
            if (path.endsWith("_leaves")) {
                map.put(item, 1L);
                continue;
            }
            if (path.endsWith("_stone") || path.endsWith("_cobblestone") || path.endsWith("_dirt") || path.endsWith("_sand")
                || path.endsWith("_gravel") || path.endsWith("_deepslate") || path.endsWith("_basalt") || path.endsWith("_tuff")
                || path.endsWith("_soil") || path.contains("cobble") || path.contains("stone") || path.contains("rock")) {
                map.put(item, 1L);
                continue;
            }
            if (!"minecraft".equals(id.getNamespace())) continue;
            unresolved.add(id);
            long legacyValue = LEGACY_FALLBACK_MAP.getOrDefault(item, 0L);
            if (legacyValue > 1L) map.put(item, legacyValue);
        }
        unresolved.sort(Comparator.comparing(ResourceLocation::toString));
        return unresolved;
    }

    private static void writeUnresolvedReport(List<ResourceLocation> unresolved) {
        try {
            File reportFile = getUnresolvedReportFile();
            File parent = reportFile.getParentFile();
            if (parent != null) parent.mkdirs();
            try (java.io.PrintWriter writer = new java.io.PrintWriter(reportFile)) {
                writer.println("# Velorise EMC unresolved recipe report");
                writer.println("# These items could not be recipe-resolved. No synthetic 1 EMC value was assigned.");
                for (ResourceLocation id : unresolved) writer.println(id);
            }
        } catch (Exception e) {
            System.err.println("[VeloriseEMC] Failed to write unresolved EMC report: " + e.getMessage());
        }
    }

    public static long getAlchemicalFuelEMC(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        Item item = stack.getItem();
        boolean supported = item == VeloriseEMC.ALCHEMICAL_COAL.get()
            || item == VeloriseEMC.MOBIUS_FUEL.get()
            || item == VeloriseEMC.AETERNALIS_FUEL.get()
            || item == VeloriseEMC.ALCHEMICAL_COAL_BLOCK_ITEM.get()
            || item == VeloriseEMC.MOBIUS_FUEL_BLOCK_ITEM.get()
            || item == VeloriseEMC.AETERNALIS_FUEL_BLOCK_ITEM.get();
        return supported ? Math.max(0L, getEMC(item)) : 0L;
    }

    public static long consumeFuelFromInventory(net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            long value = getAlchemicalFuelEMC(stack);
            if (value > 0L) {
                stack.shrink(1);
                return value;
            }
        }
        return 0L;
    }
}
