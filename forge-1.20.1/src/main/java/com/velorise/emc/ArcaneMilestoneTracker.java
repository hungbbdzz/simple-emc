package com.velorise.emc;

import net.minecraft.server.level.ServerPlayer;

/** Tracks the eleven mob-themed Arcane Weapon field tests and awards the capstone challenge. */
public final class ArcaneMilestoneTracker {
    public static final int DRAGONFIRE = 1 << 0;
    public static final int ECHO = 1 << 1;
    public static final int INFERNO = 1 << 2;
    public static final int GHASTFIRE = 1 << 3;
    public static final int WITHER = 1 << 4;
    public static final int SHULKER = 1 << 5;
    public static final int GUARDIAN = 1 << 6;
    public static final int EVOKER = 1 << 7;
    public static final int STORMCALLER = 1 << 8;
    public static final int FROST = 1 << 9;
    public static final int WEB = 1 << 10;

    private static final int ALL = (1 << 11) - 1;
    private static final String TAG = "VeloriseEMCArcaneFieldTests";

    private ArcaneMilestoneTracker() {}

    public static void award(ServerPlayer player, String advancementPath, int milestone) {
        if (player == null) return;
        SimpleAdvancements.award(player, advancementPath);
        int completed = player.getPersistentData().getInt(TAG) | milestone;
        player.getPersistentData().putInt(TAG, completed);
        if ((completed & ALL) == ALL) {
            SimpleAdvancements.award(player, "engineering/arcane_arsenal");
        }
    }
}
