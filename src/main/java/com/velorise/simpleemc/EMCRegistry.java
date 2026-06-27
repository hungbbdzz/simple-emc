package com.velorise.simpleemc;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class EMCRegistry {
    private static final Map<Item, Long> EMC_MAP = new HashMap<>();
    private static final Map<Item, Long> STATIC_EMC_MAP = new HashMap<>();
    private static final Map<Item, Long> DEFAULT_EMC_MAP = new HashMap<>();
    private static Map<Item, Long> clientSyncedOverrides = null;
    private static final java.io.File CUSTOM_EMC_FILE = new java.io.File("config/simpleemc/custom_emc.json");
    private static final java.io.File RECIPE_CACHE_FILE = new java.io.File("config/simpleemc/recipe_emc_cache.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static long getDefaultEMC(Item item) {
        return DEFAULT_EMC_MAP.getOrDefault(item, 0L);
    }

    public static void setClientSyncedOverrides(Map<Item, Long> overrides) {
        clientSyncedOverrides = overrides;
    }

    public static void clearClientSyncedOverrides() {
        clientSyncedOverrides = null;
    }

    static {
        // --- 1. Blocks & Basic Materials ---
        register(Items.COBBLESTONE, 1L);
        register(Items.COBBLED_DEEPSLATE, 1L);
        register(Items.STONE, 1L);
        register(Items.DEEPSLATE, 1L);
        register(Items.DIRT, 1L);
        register(Items.GRASS_BLOCK, 2L);
        register(Items.SAND, 1L);
        register(Items.GRAVEL, 1L);
        register(Items.CLAY_BALL, 64L);
        register(Items.CLAY, 256L);
        register(Items.FLINT, 4L);
        register(Items.OBSIDIAN, 64L);
        register(Items.CRYING_OBSIDIAN, 96L);
        register(Items.BEDROCK, 500000000L);
        register(Items.TUFF, 1L);
        register(Items.CALCITE, 4L);
        register(Items.BASALT, 4L);
        register(Items.NETHERRACK, 1L);
        register(Items.SOUL_SAND, 49L);
        register(Items.SOUL_SOIL, 49L);
        register(Items.CRIMSON_NYLIUM, 4L);
        register(Items.WARPED_NYLIUM, 4L);
        register(Items.GILDED_BLACKSTONE, 128L);
        register(Items.PODZOL, 2L);
        register(Items.ROOTED_DIRT, 2L);
        register(Items.MUD, 4L);
        register(Items.FARMLAND, 2L);
        register(Items.DIRT_PATH, 2L);
        register(Items.MYCELIUM, 2L);
        register(Items.SUSPICIOUS_SAND, 2L);
        register(Items.SUSPICIOUS_GRAVEL, 2L);
        register(Items.POINTED_DRIPSTONE, 4L);
        register(Items.DRIPSTONE_BLOCK, 16L);
        register(Items.REINFORCED_DEEPSLATE, 64L);
        register(Items.INFESTED_STONE, 1L);
        register(Items.INFESTED_COBBLESTONE, 1L);
        register(Items.INFESTED_STONE_BRICKS, 1L);
        register(Items.INFESTED_MOSSY_STONE_BRICKS, 1L);
        register(Items.INFESTED_CRACKED_STONE_BRICKS, 1L);
        register(Items.INFESTED_CHISELED_STONE_BRICKS, 1L);
        register(Items.INFESTED_DEEPSLATE, 1L);

        // --- Wool Colors ---
        register(Items.WHITE_WOOL, 48L);
        register(Items.ORANGE_WOOL, 48L);
        register(Items.MAGENTA_WOOL, 48L);
        register(Items.LIGHT_BLUE_WOOL, 48L);
        register(Items.YELLOW_WOOL, 48L);
        register(Items.LIME_WOOL, 48L);
        register(Items.PINK_WOOL, 48L);
        register(Items.GRAY_WOOL, 48L);
        register(Items.LIGHT_GRAY_WOOL, 48L);
        register(Items.CYAN_WOOL, 48L);
        register(Items.PURPLE_WOOL, 48L);
        register(Items.BLUE_WOOL, 48L);
        register(Items.BROWN_WOOL, 48L);
        register(Items.GREEN_WOOL, 48L);
        register(Items.RED_WOOL, 48L);
        register(Items.BLACK_WOOL, 48L);

        // --- 2. Woods, Planks, Saplings & Leaves ---
        register(Items.OAK_LOG, 32L);
        register(Items.SPRUCE_LOG, 32L);
        register(Items.BIRCH_LOG, 32L);
        register(Items.JUNGLE_LOG, 32L);
        register(Items.ACACIA_LOG, 32L);
        register(Items.DARK_OAK_LOG, 32L);
        register(Items.MANGROVE_LOG, 32L);
        register(Items.CHERRY_LOG, 32L);
        
        register(Items.OAK_PLANKS, 8L);
        register(Items.SPRUCE_PLANKS, 8L);
        register(Items.BIRCH_PLANKS, 8L);
        register(Items.JUNGLE_PLANKS, 8L);
        register(Items.ACACIA_PLANKS, 8L);
        register(Items.DARK_OAK_PLANKS, 8L);
        register(Items.MANGROVE_PLANKS, 8L);
        register(Items.CHERRY_PLANKS, 8L);

        register(Items.OAK_SAPLING, 32L);
        register(Items.SPRUCE_SAPLING, 32L);
        register(Items.BIRCH_SAPLING, 32L);
        register(Items.JUNGLE_SAPLING, 32L);
        register(Items.ACACIA_SAPLING, 32L);
        register(Items.DARK_OAK_SAPLING, 32L);
        register(Items.CHERRY_SAPLING, 32L);
        register(Items.MANGROVE_PROPAGULE, 32L);

        register(Items.OAK_LEAVES, 1L);
        register(Items.SPRUCE_LEAVES, 1L);
        register(Items.BIRCH_LEAVES, 1L);
        register(Items.JUNGLE_LEAVES, 1L);
        register(Items.ACACIA_LEAVES, 1L);
        register(Items.DARK_OAK_LEAVES, 1L);
        register(Items.CHERRY_LEAVES, 1L);
        register(Items.MANGROVE_LEAVES, 1L);
        register(Items.AZALEA_LEAVES, 1L);
        register(Items.FLOWERING_AZALEA_LEAVES, 1L);
        register(Items.MANGROVE_ROOTS, 2L);
        register(Items.MUDDY_MANGROVE_ROOTS, 4L);

        // --- 3. Ores, Minerals & Blocks of Ore ---
        register(Items.COAL, 128L);
        register(Items.CHARCOAL, 128L);
        register(SimpleEMC.ALCHEMICAL_COAL.get(), 1024L);
        register(SimpleEMC.MOBIUS_FUEL.get(), 8192L);
        register(SimpleEMC.AETERNALIS_FUEL.get(), 65536L);
        register(SimpleEMC.ALCHEMICAL_COAL_BLOCK_ITEM.get(), 9216L);
        register(SimpleEMC.MOBIUS_FUEL_BLOCK_ITEM.get(), 73728L);
        register(SimpleEMC.AETERNALIS_FUEL_BLOCK_ITEM.get(), 589824L);
        register(Items.RAW_IRON, 256L);
        register(Items.RAW_GOLD, 2048L);
        register(Items.RAW_COPPER, 128L);
        register(Items.IRON_INGOT, 256L);
        register(Items.GOLD_INGOT, 2048L);
        register(Items.COPPER_INGOT, 128L);
        register(Items.REDSTONE, 64L);
        register(Items.LAPIS_LAZULI, 64L);
        register(Items.DIAMOND, 8192L);
        register(Items.EMERALD, 16384L);
        register(Items.NETHERITE_INGOT, 57344L);
        register(Items.NETHERITE_SCRAP, 12288L);
        register(Items.QUARTZ, 256L);
        register(Items.GLOWSTONE_DUST, 384L);

        register(Items.COAL_BLOCK, 1152L);
        register(Items.IRON_BLOCK, 2304L);
        register(Items.GOLD_BLOCK, 18432L);
        register(Items.COPPER_BLOCK, 1152L);
        register(Items.REDSTONE_BLOCK, 576L);
        register(Items.LAPIS_BLOCK, 576L);
        register(Items.DIAMOND_BLOCK, 73728L);
        register(Items.EMERALD_BLOCK, 147456L);
        register(Items.NETHERITE_BLOCK, 516096L);

        register(Items.AMETHYST_SHARD, 32L);
        register(Items.AMETHYST_BLOCK, 128L);
        register(Items.BUDDING_AMETHYST, 128L);
        register(Items.SMALL_AMETHYST_BUD, 32L);
        register(Items.MEDIUM_AMETHYST_BUD, 64L);
        register(Items.LARGE_AMETHYST_BUD, 96L);
        register(Items.AMETHYST_CLUSTER, 128L);

        register(Items.EXPOSED_COPPER, 1152L);
        register(Items.WEATHERED_COPPER, 1152L);
        register(Items.OXIDIZED_COPPER, 1152L);

        // --- 4. Mob Drops & Special Drops ---
        register(Items.ROTTEN_FLESH, 24L);
        register(Items.BONE, 144L);
        register(Items.GUNPOWDER, 192L);
        register(Items.STRING, 12L);
        register(Items.SPIDER_EYE, 128L);
        register(Items.FEATHER, 48L);
        register(Items.LEATHER, 64L);
        register(Items.SLIME_BALL, 32L);
        register(Items.ENDER_PEARL, 1024L);
        register(Items.BLAZE_ROD, 1536L);
        register(Items.GHAST_TEAR, 4096L);
        register(Items.NETHER_STAR, 139264L);
        register(Items.SADDLE, 192L);
        register(Items.NAME_TAG, 192L);
        register(Items.ELYTRA, 8192L);
        register(Items.SCUTE, 128L);
        register(Items.BELL, 1024L);
        register(Items.DRAGON_EGG, 262144L);
        register(Items.ECHO_SHARD, 2048L);
        register(Items.TOTEM_OF_UNDYING, 5000000L);
        register(Items.TRIDENT, 2048L);
        register(Items.PHANTOM_MEMBRANE, 192L);
        register(Items.EXPERIENCE_BOTTLE, 128L);
        register(Items.SHULKER_SHELL, 1024L);
        register(Items.DRAGON_BREATH, 1024L);
        register(Items.POTION, 64L);
        register(Items.SPLASH_POTION, 96L);
        register(Items.LINGERING_POTION, 128L);
        register(Items.TIPPED_ARROW, 32L);
        register(Items.WRITTEN_BOOK, 96L);
        register(Items.ENCHANTED_BOOK, 2048L);
        register(Items.DIAMOND_HORSE_ARMOR, 40960L);
        register(Items.CHIPPED_ANVIL, 7936L);
        register(Items.DAMAGED_ANVIL, 7936L);
        register(Items.FIREWORK_STAR, 256L);
        register(Items.SUSPICIOUS_STEW, 96L);
        register(Items.GOAT_HORN, 256L);

        // Heads & Skulls
        register(Items.SKELETON_SKULL, 1024L);
        register(Items.WITHER_SKELETON_SKULL, 2048L);
        register(Items.PLAYER_HEAD, 1024L);
        register(Items.ZOMBIE_HEAD, 1024L);
        register(Items.CREEPER_HEAD, 1024L);
        register(Items.DRAGON_HEAD, 4096L);
        register(Items.PIGLIN_HEAD, 1024L);

        // --- 5. Foods, Agriculture & Honey ---
        register(Items.WHEAT, 24L);
        register(Items.BREAD, 72L);
        register(Items.APPLE, 128L);
        register(Items.ENCHANTED_GOLDEN_APPLE, 50000000L);
        register(Items.PORKCHOP, 64L);
        register(Items.COOKED_PORKCHOP, 64L);
        register(Items.BEEF, 64L);
        register(Items.COOKED_BEEF, 64L);
        register(Items.CHICKEN, 64L);
        register(Items.COOKED_CHICKEN, 64L);
        register(Items.MUTTON, 64L);
        register(Items.COOKED_MUTTON, 64L);
        register(Items.SUGAR_CANE, 32L);
        register(Items.KELP, 1L);
        register(Items.BAMBOO, 8L);
        register(Items.CACTUS, 16L);
        register(Items.NETHER_WART, 24L);
        register(Items.CRIMSON_FUNGUS, 32L);
        register(Items.WARPED_FUNGUS, 32L);
        register(Items.BROWN_MUSHROOM, 32L);
        register(Items.RED_MUSHROOM, 32L);
        register(Items.MOSS_BLOCK, 12L);
        register(Items.CHORUS_FRUIT, 192L);
        register(Items.WHEAT_SEEDS, 16L);
        register(Items.BEETROOT_SEEDS, 16L);
        register(Items.MELON_SEEDS, 16L);
        register(Items.PUMPKIN_SEEDS, 16L);
        register(Items.CARROT, 64L);
        register(Items.POTATO, 64L);
        register(Items.POISONOUS_POTATO, 64L);
        register(Items.COCOA_BEANS, 16L);
        register(Items.HONEYCOMB, 16L);
        register(Items.INK_SAC, 16L);
        register(Items.GLOW_INK_SAC, 16L);
        register(Items.SWEET_BERRIES, 16L);
        register(Items.GLOW_BERRIES, 16L);
        register(Items.RABBIT, 64L);
        register(Items.COOKED_RABBIT, 64L);
        register(Items.RABBIT_FOOT, 128L);
        register(Items.EGG, 16L);
        register(Items.BEE_NEST, 64L);

        // --- 6. Ocean, Ice & Coral ---
        register(Items.SPONGE, 128L);
        register(Items.WET_SPONGE, 128L);
        register(Items.COBWEB, 128L);
        register(Items.NAUTILUS_SHELL, 1024L);
        register(Items.HEART_OF_THE_SEA, 32768L);
        register(Items.PRISMARINE_SHARD, 256L);
        register(Items.PRISMARINE_CRYSTALS, 512L);
        register(Items.SEA_PICKLE, 16L);
        register(Items.ICE, 1L);
        register(Items.PACKED_ICE, 9L);
        register(Items.BLUE_ICE, 81L);
        register(Items.SNOWBALL, 1L);
        register(Items.SNOW, 1L);

        register(Items.TUBE_CORAL, 16L);
        register(Items.BRAIN_CORAL, 16L);
        register(Items.BUBBLE_CORAL, 16L);
        register(Items.FIRE_CORAL, 16L);
        register(Items.HORN_CORAL, 16L);
        register(Items.TUBE_CORAL_FAN, 16L);
        register(Items.BRAIN_CORAL_FAN, 16L);
        register(Items.BUBBLE_CORAL_FAN, 16L);
        register(Items.FIRE_CORAL_FAN, 16L);
        register(Items.HORN_CORAL_FAN, 16L);
        register(Items.TUBE_CORAL_BLOCK, 64L);
        register(Items.BRAIN_CORAL_BLOCK, 64L);
        register(Items.BUBBLE_CORAL_BLOCK, 64L);
        register(Items.FIRE_CORAL_BLOCK, 64L);
        register(Items.HORN_CORAL_BLOCK, 64L);
        
        register(Items.DEAD_TUBE_CORAL, 4L);
        register(Items.DEAD_BRAIN_CORAL, 4L);
        register(Items.DEAD_BUBBLE_CORAL, 4L);
        register(Items.DEAD_FIRE_CORAL, 4L);
        register(Items.DEAD_HORN_CORAL, 4L);
        register(Items.DEAD_TUBE_CORAL_FAN, 4L);
        register(Items.DEAD_BRAIN_CORAL_FAN, 4L);
        register(Items.DEAD_BUBBLE_CORAL_FAN, 4L);
        register(Items.DEAD_FIRE_CORAL_FAN, 4L);
        register(Items.DEAD_HORN_CORAL_FAN, 4L);
        register(Items.DEAD_TUBE_CORAL_BLOCK, 16L);
        register(Items.DEAD_BRAIN_CORAL_BLOCK, 16L);
        register(Items.DEAD_BUBBLE_CORAL_BLOCK, 16L);
        register(Items.DEAD_FIRE_CORAL_BLOCK, 16L);
        register(Items.DEAD_HORN_CORAL_BLOCK, 16L);

        // --- 7. Flowers & Small Plants ---
        register(Items.DANDELION, 16L);
        register(Items.POPPY, 16L);
        register(Items.BLUE_ORCHID, 16L);
        register(Items.ALLIUM, 16L);
        register(Items.AZURE_BLUET, 16L);
        register(Items.RED_TULIP, 16L);
        register(Items.ORANGE_TULIP, 16L);
        register(Items.WHITE_TULIP, 16L);
        register(Items.PINK_TULIP, 16L);
        register(Items.OXEYE_DAISY, 16L);
        register(Items.CORNFLOWER, 16L);
        register(Items.LILY_OF_THE_VALLEY, 16L);
        register(Items.WITHER_ROSE, 2048L);

        // --- 7a. Dyes ---
        register(Items.WHITE_DYE, 16L);
        register(Items.LIGHT_GRAY_DYE, 16L);
        register(Items.GRAY_DYE, 16L);
        register(Items.BLACK_DYE, 16L);
        register(Items.RED_DYE, 16L);
        register(Items.ORANGE_DYE, 16L);
        register(Items.YELLOW_DYE, 16L);
        register(Items.LIME_DYE, 16L);
        register(Items.GREEN_DYE, 16L);
        register(Items.CYAN_DYE, 16L);
        register(Items.LIGHT_BLUE_DYE, 16L);
        register(Items.BLUE_DYE, 16L);
        register(Items.PURPLE_DYE, 16L);
        register(Items.MAGENTA_DYE, 16L);
        register(Items.PINK_DYE, 16L);
        register(Items.BROWN_DYE, 16L);
        register(Items.TORCHFLOWER, 32L);
        register(Items.SPORE_BLOSSOM, 128L);
        register(Items.PITCHER_POD, 32L);
        register(Items.TORCHFLOWER_SEEDS, 16L);
        register(Items.SUNFLOWER, 32L);
        register(Items.LILAC, 32L);
        register(Items.ROSE_BUSH, 32L);
        register(Items.PEONY, 32L);
        register(Items.TALL_GRASS, 2L);
        register(Items.LARGE_FERN, 2L);
        register(Items.SEAGRASS, 2L);
        register(Items.GRASS, 1L);
        register(Items.FERN, 1L);
        register(Items.DEAD_BUSH, 1L);
        register(Items.AZALEA, 32L);
        register(Items.FLOWERING_AZALEA, 48L);
        register(Items.VINE, 8L);
        register(Items.GLOW_LICHEN, 8L);
        register(Items.CRIMSON_ROOTS, 2L);
        register(Items.WARPED_ROOTS, 2L);
        register(Items.NETHER_SPROUTS, 2L);
        register(Items.WEEPING_VINES, 4L);
        register(Items.TWISTING_VINES, 4L);
        register(Items.HANGING_ROOTS, 2L);
        register(Items.BIG_DRIPLEAF, 16L);
        register(Items.SMALL_DRIPLEAF, 16L);
        register(Items.PINK_PETALS, 16L);
        register(Items.BROWN_MUSHROOM_BLOCK, 8L);
        register(Items.RED_MUSHROOM_BLOCK, 8L);
        register(Items.MUSHROOM_STEM, 8L);

        // --- 8. Templates, Patterns & Sherds ---
        register(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 57345L);
        register(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);
        register(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, 57345L);

        register(Items.FLOWER_BANNER_PATTERN, 256L);
        register(Items.CREEPER_BANNER_PATTERN, 256L);
        register(Items.SKULL_BANNER_PATTERN, 256L);
        register(Items.MOJANG_BANNER_PATTERN, 256L);
        register(Items.GLOBE_BANNER_PATTERN, 256L);
        register(Items.PIGLIN_BANNER_PATTERN, 256L);

        register(Items.ANGLER_POTTERY_SHERD, 128L);
        register(Items.ARCHER_POTTERY_SHERD, 128L);
        register(Items.ARMS_UP_POTTERY_SHERD, 128L);
        register(Items.BLADE_POTTERY_SHERD, 128L);
        register(Items.BREWER_POTTERY_SHERD, 128L);
        register(Items.BURN_POTTERY_SHERD, 128L);
        register(Items.DANGER_POTTERY_SHERD, 128L);
        register(Items.EXPLORER_POTTERY_SHERD, 128L);
        register(Items.FRIEND_POTTERY_SHERD, 128L);
        register(Items.HEART_POTTERY_SHERD, 128L);
        register(Items.HEARTBREAK_POTTERY_SHERD, 128L);
        register(Items.HOWL_POTTERY_SHERD, 128L);
        register(Items.MINER_POTTERY_SHERD, 128L);
        register(Items.MOURNER_POTTERY_SHERD, 128L);
        register(Items.PLENTY_POTTERY_SHERD, 128L);
        register(Items.PRIZE_POTTERY_SHERD, 128L);
        register(Items.SHEAF_POTTERY_SHERD, 128L);
        register(Items.SHELTER_POTTERY_SHERD, 128L);
        register(Items.SKULL_POTTERY_SHERD, 128L);
        register(Items.SNORT_POTTERY_SHERD, 128L);

        // --- 9. Music Discs ---
        register(Items.MUSIC_DISC_13, 2048L);
        register(Items.MUSIC_DISC_CAT, 2048L);
        register(Items.MUSIC_DISC_BLOCKS, 2048L);
        register(Items.MUSIC_DISC_CHIRP, 2048L);
        register(Items.MUSIC_DISC_FAR, 2048L);
        register(Items.MUSIC_DISC_MALL, 2048L);
        register(Items.MUSIC_DISC_MELLOHI, 2048L);
        register(Items.MUSIC_DISC_STAL, 2048L);
        register(Items.MUSIC_DISC_STRAD, 2048L);
        register(Items.MUSIC_DISC_WARD, 2048L);
        register(Items.MUSIC_DISC_11, 2048L);
        register(Items.MUSIC_DISC_WAIT, 2048L);
        register(Items.MUSIC_DISC_OTHERSIDE, 2048L);
        register(Items.MUSIC_DISC_RELIC, 2048L);
        register(Items.MUSIC_DISC_5, 2048L);
        register(Items.MUSIC_DISC_PIGSTEP, 2048L);
        register(Items.DISC_FRAGMENT_5, 256L);

        // --- 10. Froglights & Heads ---
        register(Items.OCHRE_FROGLIGHT, 1024L);
        register(Items.VERDANT_FROGLIGHT, 1024L);
        register(Items.PEARLESCENT_FROGLIGHT, 1024L);
        register(Items.FROGSPAWN, 128L);

        // --- 11. Netherite Equipment ---
        register(Items.NETHERITE_SWORD, 131077L);
        register(Items.NETHERITE_SHOVEL, 122889L);
        register(Items.NETHERITE_PICKAXE, 139273L);
        register(Items.NETHERITE_AXE, 139273L);
        register(Items.NETHERITE_HOE, 131081L);
        register(Items.NETHERITE_HELMET, 155649L);
        register(Items.NETHERITE_CHESTPLATE, 180225L);
        register(Items.NETHERITE_LEGGINGS, 172033L);
        register(Items.NETHERITE_BOOTS, 147457L);

        // --- 12. Survival Items from simpleemc_missing_emc.txt ---
        register(Items.CHORUS_PLANT, 64L);
        register(Items.CHORUS_FLOWER, 96L);
        register(Items.CARVED_PUMPKIN, 16L);
        register(Items.JACK_O_LANTERN, 24L);
        register(Items.LILY_PAD, 16L);
        register(Items.SCULK, 4L);
        register(Items.SCULK_VEIN, 1L);
        register(Items.SCULK_CATALYST, 128L);
        register(Items.SCULK_SENSOR, 256L);
        register(Items.CALIBRATED_SCULK_SENSOR, 512L);
        register(Items.SCULK_SHRIEKER, 512L);
        register(Items.END_STONE, 4L);
        register(Items.END_STONE_BRICKS, 4L);
        register(Items.TURTLE_EGG, 192L);
        register(Items.SNIFFER_EGG, 1024L);
        register(Items.WATER_BUCKET, 768L);
        register(Items.LAVA_BUCKET, 832L);
        register(Items.POWDER_SNOW_BUCKET, 768L);
        register(Items.MILK_BUCKET, 832L);
        register(Items.PUFFERFISH_BUCKET, 832L);
        register(Items.SALMON_BUCKET, 832L);
        register(Items.COD_BUCKET, 832L);
        register(Items.TROPICAL_FISH_BUCKET, 832L);
        register(Items.COD, 64L);
        register(Items.COOKED_COD, 64L);
        register(Items.SALMON, 64L);
        register(Items.COOKED_SALMON, 64L);
        register(Items.TROPICAL_FISH, 64L);
        register(Items.PUFFERFISH, 64L);
        register(Items.CAKE, 1024L);
        register(Items.SHROOMLIGHT, 64L);
        register(Items.WARPED_WART_BLOCK, 96L);

        // Double all values statically to support fractional recipes (e.g. Slabs)
        for (Map.Entry<Item, Long> entry : EMC_MAP.entrySet()) {
            entry.setValue(entry.getValue() * 2);
        }

        // Backup static map
        STATIC_EMC_MAP.putAll(EMC_MAP);
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);

        // Load cached recipe EMC from a previous session
        loadRecipeCache();

        // Load custom EMC overrides
        loadCustomEMC();
    }

    public static void loadCustomEMC() {
        if (!CUSTOM_EMC_FILE.exists()) {
            return;
        }
        try (java.io.FileReader reader = new java.io.FileReader(CUSTOM_EMC_FILE)) {
            java.util.Map<String, Long> customMap = GSON.fromJson(reader, new TypeToken<java.util.Map<String, Long>>(){}.getType());
            if (customMap != null) {
                for (Map.Entry<String, Long> entry : customMap.entrySet()) {
                    ResourceLocation rl = ResourceLocation.tryParse(entry.getKey());
                    if (rl != null) {
                        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                        if (item != null && item != Items.AIR) {
                            EMC_MAP.put(item, entry.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load custom EMC: " + e.getMessage());
        }
    }

    public static void loadRecipeCache() {
        if (!RECIPE_CACHE_FILE.exists()) return;
        try (java.io.FileReader reader = new java.io.FileReader(RECIPE_CACHE_FILE)) {
            Map<String, Long> cacheMap = GSON.fromJson(reader, new TypeToken<Map<String, Long>>(){}.getType());
            if (cacheMap != null) {
                for (Map.Entry<String, Long> entry : cacheMap.entrySet()) {
                    ResourceLocation rl = ResourceLocation.tryParse(entry.getKey());
                    if (rl != null) {
                        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                        if (item != null && item != Items.AIR && !STATIC_EMC_MAP.containsKey(item)) {
                            EMC_MAP.put(item, entry.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load recipe EMC cache: " + e.getMessage());
        }
    }

    private static void saveRecipeCache() {
        try {
            java.io.File parent = RECIPE_CACHE_FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            Map<String, Long> cacheMap = new HashMap<>();
            for (Map.Entry<Item, Long> entry : EMC_MAP.entrySet()) {
                if (!STATIC_EMC_MAP.containsKey(entry.getKey())) {
                    ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey());
                    if (rl != null) cacheMap.put(rl.toString(), entry.getValue());
                }
            }

            try (java.io.FileWriter writer = new java.io.FileWriter(RECIPE_CACHE_FILE)) {
                GSON.toJson(cacheMap, writer);
            }
            System.out.println("[SimpleEMC] Recipe EMC cache saved: " + cacheMap.size()
                + " entries → " + RECIPE_CACHE_FILE.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[SimpleEMC] Failed to save recipe EMC cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveCustomEMC(java.util.Map<Item, Long> overrides) {
        try {
            java.io.File parent = CUSTOM_EMC_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            java.util.Map<String, Long> jsonMap = new java.util.HashMap<>();
            if (CUSTOM_EMC_FILE.exists()) {
                try (java.io.FileReader reader = new java.io.FileReader(CUSTOM_EMC_FILE)) {
                    java.util.Map<String, Long> existing = GSON.fromJson(reader, new TypeToken<java.util.Map<String, Long>>(){}.getType());
                    if (existing != null) {
                        jsonMap.putAll(existing);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            
            for (Map.Entry<Item, Long> entry : overrides.entrySet()) {
                String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey()).toString();
                long val = entry.getValue();
                long defaultVal = getDefaultEMC(entry.getKey());
                if (val <= 0 || val == defaultVal) {
                    jsonMap.remove(key);
                } else {
                    jsonMap.put(key, val);
                }
            }
            
            try (java.io.FileWriter writer = new java.io.FileWriter(CUSTOM_EMC_FILE)) {
                GSON.toJson(jsonMap, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save custom EMC: " + e.getMessage());
        }
    }

    public static void reloadAndRecalculate(net.minecraft.world.item.crafting.RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        DEFAULT_EMC_MAP.clear();
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);
        calculateRecipeEMCForMap(DEFAULT_EMC_MAP, recipeManager, registries, false);

        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        loadCustomEMC();
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, true);
    }

    public static void clientReloadAndRecalculate(net.minecraft.world.item.crafting.RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        DEFAULT_EMC_MAP.clear();
        DEFAULT_EMC_MAP.putAll(STATIC_EMC_MAP);
        calculateRecipeEMCForMap(DEFAULT_EMC_MAP, recipeManager, registries, false);

        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        if (clientSyncedOverrides != null) {
            EMC_MAP.putAll(clientSyncedOverrides);
        } else {
            loadCustomEMC();
        }
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, false);
    }

    public static void applyCustomOverridesAndRecalculate(Map<Item, Long> overrides, net.minecraft.world.item.crafting.RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        EMC_MAP.clear();
        EMC_MAP.putAll(STATIC_EMC_MAP);
        overrides.forEach(EMC_MAP::put);
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, false);
    }

    public static Map<ResourceLocation, Long> getFullCustomOverrides() {
        Map<ResourceLocation, Long> map = new HashMap<>();
        if (!CUSTOM_EMC_FILE.exists()) {
            return map;
        }
        try (java.io.FileReader reader = new java.io.FileReader(CUSTOM_EMC_FILE)) {
            Map<String, Long> customMap = GSON.fromJson(reader, new TypeToken<Map<String, Long>>(){}.getType());
            if (customMap != null) {
                customMap.forEach((k, v) -> {
                    ResourceLocation rl = ResourceLocation.tryParse(k);
                    if (rl != null) {
                        map.put(rl, v);
                    }
                });
            }
        } catch (Exception e) {
            // ignore
        }
        return map;
    }

    public static void register(Item item, long emc) {
        EMC_MAP.put(item, emc);
    }

    public static long getEMC(Item item) {
        return EMC_MAP.getOrDefault(item, 0L);
    }

    public static long getEMC(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return getEMC(stack.getItem()) * stack.getCount();
    }

    public static boolean hasEMC(Item item) {
        return EMC_MAP.containsKey(item);
    }

    public static boolean hasEMC(ItemStack stack) {
        return !stack.isEmpty() && hasEMC(stack.getItem());
    }

    public static boolean hasRecipeCache() {
        return RECIPE_CACHE_FILE.exists();
    }

    public static Map<Item, Long> getEMCMap() {
        return EMC_MAP;
    }

    public static java.util.Set<Item> getAllItems() {
        return EMC_MAP.keySet();
    }

    public static void calculateAllRecipeEMC(RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries) {
        calculateRecipeEMCForMap(EMC_MAP, recipeManager, registries, true);
    }

    public static void calculateRecipeEMCForMap(Map<Item, Long> map, RecipeManager recipeManager, net.minecraft.core.RegistryAccess registries, boolean shouldSaveCache) {
        boolean changed = true;
        int maxIterations = 10;
        int iterations = 0;

        var craftingRecipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        List<Recipe<?>> allRecipes = new ArrayList<>();
        allRecipes.addAll(craftingRecipes);
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.SMELTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.BLASTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.SMOKING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.STONECUTTING));
        allRecipes.addAll(recipeManager.getAllRecipesFor(RecipeType.SMITHING));

        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;

            for (Recipe<?> recipe : allRecipes) {
                ItemStack resultStack = recipe.getResultItem(registries);
                if (resultStack.isEmpty()) continue;

                Item resultItem = resultStack.getItem();
                if (map.containsKey(resultItem)) {
                    continue;
                }

                long totalEMC = 0;
                boolean allInputsHaveEMC = true;

                var ingredients = recipe.getIngredients();
                if (ingredients.isEmpty()) continue;

                for (var ingredient : ingredients) {
                    if (ingredient.isEmpty()) continue;

                    boolean isCatalyst = false;
                    for (ItemStack matchingStack : ingredient.getItems()) {
                        if (matchingStack.getItem() == com.velorise.simpleemc.SimpleEMC.PHILOSOPHERS_STONE.get()) {
                            isCatalyst = true;
                            break;
                        }
                    }
                    if (isCatalyst) continue;

                    long ingredientEMC = 0;
                    for (ItemStack matchingStack : ingredient.getItems()) {
                        long emc = map.getOrDefault(matchingStack.getItem(), 0L);
                        if (emc > 0) {
                            ingredientEMC = emc;
                            break;
                        }
                    }

                    if (ingredientEMC == 0) {
                        allInputsHaveEMC = false;
                        break;
                    }

                    totalEMC += ingredientEMC;
                }

                if (allInputsHaveEMC && totalEMC > 0) {
                    int count = resultStack.getCount();
                    if (count > 0) {
                        long calculatedEMC = Math.max(1L, totalEMC / count);
                        map.put(resultItem, calculatedEMC);
                        changed = true;
                    }
                }
            }

            for (Recipe<?> recipe : allRecipes) {
                ItemStack resultStack = recipe.getResultItem(registries);
                if (resultStack.isEmpty()) continue;

                Item resultItem = resultStack.getItem();
                long outputEMC = map.getOrDefault(resultItem, 0L);
                if (outputEMC == 0) {
                    continue;
                }

                var ingredients = recipe.getIngredients();
                if (ingredients.isEmpty()) continue;

                List<Item> commonItems = null;
                int inputCount = 0;
                boolean isSingleIngredientType = true;

                for (var ingredient : ingredients) {
                    if (ingredient.isEmpty()) continue;

                    boolean isCatalyst = false;
                    for (ItemStack matchingStack : ingredient.getItems()) {
                        if (matchingStack.getItem() == com.velorise.simpleemc.SimpleEMC.PHILOSOPHERS_STONE.get()) {
                            isCatalyst = true;
                            break;
                        }
                    }
                    if (isCatalyst) continue;

                    ItemStack[] matchingStacks = ingredient.getItems();
                    if (matchingStacks.length == 0) {
                        isSingleIngredientType = false;
                        break;
                    }

                    List<Item> slotItems = new ArrayList<>();
                    for (ItemStack stack : matchingStacks) {
                        Item item = stack.getItem();
                        if (!slotItems.contains(item)) {
                            slotItems.add(item);
                        }
                    }

                    if (commonItems == null) {
                        commonItems = slotItems;
                    } else {
                        if (commonItems.size() != slotItems.size() || !commonItems.containsAll(slotItems)) {
                            isSingleIngredientType = false;
                            break;
                        }
                    }

                    inputCount++;
                }

                if (isSingleIngredientType && commonItems != null && inputCount > 0) {
                    long totalOutputEMC = outputEMC * resultStack.getCount();
                    long inputEMC = totalOutputEMC / inputCount;
                    if (inputEMC > 0) {
                        for (Item item : commonItems) {
                            if (!map.containsKey(item)) {
                                map.put(item, inputEMC);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }

        // 3. Special handling for Concrete
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
            if (path.endsWith("_concrete")) {
                if (!map.containsKey(item)) {
                    String powderPath = path + "_powder";
                    Item powderItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        new ResourceLocation("minecraft", powderPath)
                    );
                    if (powderItem != Items.AIR && map.containsKey(powderItem)) {
                        map.put(item, map.getOrDefault(powderItem, 0L));
                    }
                }
            }
        }

        // 4. Special handling for Colored Shulker Boxes
        long normalShulkerEMC = map.getOrDefault(Items.SHULKER_BOX, 0L);
        if (normalShulkerEMC > 0) {
            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
                if (path.endsWith("_shulker_box") && !path.equals("shulker_box")) {
                    if (!map.containsKey(item)) {
                        map.put(item, normalShulkerEMC);
                    }
                }
            }
        }

        if (shouldSaveCache) {
            saveRecipeCache();
        } 
    }

    public static long consumeFuelFromInventory(net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item == SimpleEMC.ALCHEMICAL_COAL.get() ||
                    item == SimpleEMC.MOBIUS_FUEL.get() ||
                    item == SimpleEMC.AETERNALIS_FUEL.get() ||
                    item == SimpleEMC.ALCHEMICAL_COAL_BLOCK_ITEM.get() ||
                    item == SimpleEMC.MOBIUS_FUEL_BLOCK_ITEM.get() ||
                    item == SimpleEMC.AETERNALIS_FUEL_BLOCK_ITEM.get()) {
                    
                    long emc = getEMC(item);
                    if (emc > 0) {
                        stack.shrink(1);
                        return emc;
                    }
                }
            }
        }
        return 0;
    }
}
