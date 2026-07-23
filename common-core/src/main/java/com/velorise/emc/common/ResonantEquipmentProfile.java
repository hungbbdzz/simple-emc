package com.velorise.emc.common;

/** Shared constants for the four World-Tier-gated Resonant equipment sets. */
public final class ResonantEquipmentProfile {
    private ResonantEquipmentProfile() {}

    private static final long[] INGOT_EMC = {0L, 32_768L, 65_536L, 131_072L, 262_144L};
    private static final int[] TOOL_DURABILITY = {0, 1_800, 3_500, 6_000, 10_000};
    private static final float[] TOOL_SPEED = {0.0F, 8.5F, 12.0F, 15.0F, 18.0F};
    private static final float[] SWORD_DAMAGE = {0.0F, 7.5F, 11.0F, 13.0F, 15.0F};
    private static final float[] TOUGHNESS = {0.0F, 2.5F, 4.0F, 5.5F, 7.5F};
    private static final float[] KNOCKBACK_PER_PIECE = {0.0F, 0.05F, 0.15F, 0.20F, 0.25F};
    private static final int[][] DEFENSE = {
        {0, 0, 0, 0},
        {3, 8, 6, 3},
        {4, 9, 7, 3},
        {5, 10, 7, 4},
        {6, 11, 8, 5}
    };
    // helmet, chestplate, leggings, boots; totals are 1550/2200/2800/3800.
    private static final int[][] ARMOR_DURABILITY = {
        {0, 0, 0, 0},
        {310, 451, 423, 366},
        {440, 640, 600, 520},
        {560, 815, 764, 661},
        {760, 1105, 1036, 899}
    };

    public static int clampTier(int tier) { return Math.max(1, Math.min(4, tier)); }
    public static long ingotEmc(int tier) { return INGOT_EMC[clampTier(tier)]; }
    public static int toolDurability(int tier) { return TOOL_DURABILITY[clampTier(tier)]; }
    public static float toolSpeed(int tier) { return TOOL_SPEED[clampTier(tier)]; }
    public static float swordDamage(int tier) { return SWORD_DAMAGE[clampTier(tier)]; }
    /** Tier#getAttackDamageBonus; a sword adds four total damage on top of this value. */
    public static float attackDamageBonus(int tier) { return swordDamage(tier) - 4.0F; }
    public static int enchantability(int tier) { return switch (clampTier(tier)) { case 1 -> 12; case 2 -> 17; case 3 -> 21; default -> 26; }; }
    public static boolean fireproof(int tier) { return clampTier(tier) >= 2; }
    public static float toughness(int tier) { return TOUGHNESS[clampTier(tier)]; }
    public static float knockbackResistancePerPiece(int tier) { return KNOCKBACK_PER_PIECE[clampTier(tier)]; }
    public static int defense(int tier, int slotIndex) { return DEFENSE[clampTier(tier)][Math.max(0, Math.min(3, slotIndex))]; }
    public static int armorDurability(int tier, int slotIndex) { return ARMOR_DURABILITY[clampTier(tier)][Math.max(0, Math.min(3, slotIndex))]; }
}
