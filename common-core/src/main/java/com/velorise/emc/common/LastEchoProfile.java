package com.velorise.emc.common;

/** Shared balance and placement rules for the Tier-IV endgame encounter. */
public final class LastEchoProfile {
    public static final int REQUIRED_WORLD_TIER = 4;
    public static final int MIN_DISTANCE = 5_000;
    public static final int MAX_DISTANCE = 10_000;
    public static final int CITADEL_Y = 160;
    public static final int ARENA_RADIUS = 48;
    /** Temporary block-protection volume while the encounter is active. */
    public static final int ARENA_PROTECTION_RADIUS = 64;
    public static final int ARENA_PROTECTION_BELOW = 16;
    public static final int ARENA_PROTECTION_ABOVE = 64;
    public static final int ACTIVATION_RADIUS = 96;
    public static final int BUILD_TRIGGER_RADIUS = 768;
    public static final int COLUMNS_PER_TICK = 96;

    public static final float BASE_HEALTH = 4_000.0F;
    public static final float HEALTH_PER_EXTRA_PLAYER = 2_000.0F;
    public static final float MAX_HEALTH = 10_000.0F;
    public static final int[] CRYSTALS_BY_PHASE = {8, 6, 4, 0};
    public static final int[] ATTACK_INTERVAL = {80, 65, 50, 35};
    public static final float[] PHASE_DAMAGE_MULTIPLIER = {1.0F, 1.25F, 1.55F, 2.0F};
    public static final float[] CRYSTAL_REGEN_PERCENT = {0.0015F, 0.0020F, 0.0025F, 0.0F};

    public static final long HEART_EMC = 134_217_728L;
    public static final long SCALE_EMC = 4_194_304L;
    public static final int MIN_SCALE_DROP = 6;
    public static final int MAX_SCALE_DROP = 10;

    // Post-boss armor. Vanilla armor absorption caps the useful armor points at 20,
    // so the full-set reduction is deliberately separate and applies after the normal armor calculation.
    public static final int[] ECHOBOUND_DEFENSE = {12, 20, 16, 10};
    public static final int[] ECHOBOUND_DURABILITY = {8_000, 12_000, 11_000, 9_000};
    public static final float ECHOBOUND_TOUGHNESS = 8.0F;
    public static final float ECHOBOUND_KNOCKBACK_PER_PIECE = 0.25F;
    /** Full set removes 80% of incoming damage after vanilla armor calculation. */
    public static final float ECHOBOUND_INCOMING_DAMAGE_MULTIPLIER = 0.20F;
    public static final int ECHOBOUND_ENCHANTABILITY = 30;
    public static final int ECHOBOUND_TOOL_DURABILITY = 16_000;
    /** Exactly twice the Tier-IV Echoing tool speed (18.0). */
    public static final float ECHOBOUND_TOOL_SPEED = 36.0F;
    /** Sword base (1 + 3) plus this bonus resolves to 25 attack damage. */
    public static final float ECHOBOUND_ATTACK_BONUS = 21.0F;
    /** Each endgame sword attack sequence opens with five empowered pulses. */
    public static final int ECHOBOUND_EMPOWERED_OPENING_HITS = 5;
    public static final float ECHOBOUND_EMPOWERED_OPENING_DAMAGE = 100.0F;
    /** Twenty rapid follow-up pulses; pulses after the empowered opening deal twenty-five damage. */
    public static final int ECHOBOUND_MELEE_ECHO_HITS = 20;
    public static final float ECHOBOUND_MELEE_ECHO_DAMAGE = 25.0F;
    public static final int ECHOBOUND_SONIC_CHARGE_TICKS = 20;
    public static final int ECHOBOUND_SONIC_DAMAGE_PULSES = 50;
    /** Sonic pulses after the first five empowered pulses return to this damage. */
    public static final float ECHOBOUND_SONIC_DAMAGE_PER_PULSE = 10.0F;
    public static final double ECHOBOUND_SONIC_RANGE = 64.0D;
    public static final long ECHOBOUND_SONIC_EMC_COST = 4_096L;
    public static final int ECHOBOUND_SONIC_COOLDOWN_TICKS = 80;

    private LastEchoProfile() {}

    public static int phase(float health, float maxHealth) {
        if (maxHealth <= 0.0F) return 0;
        float ratio = health / maxHealth;
        if (ratio > 0.75F) return 0;
        if (ratio > 0.50F) return 1;
        if (ratio > 0.25F) return 2;
        return 3;
    }

    public static float scaledHealth(int nearbyPlayers) {
        int extra = Math.max(0, nearbyPlayers - 1);
        return Math.min(MAX_HEALTH, BASE_HEALTH + extra * HEALTH_PER_EXTRA_PLAYER);
    }
}
