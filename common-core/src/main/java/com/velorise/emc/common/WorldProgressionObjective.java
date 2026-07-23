package com.velorise.emc.common;

/** Stable objective ids shared by both loaders, the server config and the progress UI. */
public enum WorldProgressionObjective {
    OVERWORLD_TOTAL(WorldTierStage.OVERWORLD_HUNT, "Total Overworld hostiles", false, true),
    ZOMBIE_FAMILY(WorldTierStage.OVERWORLD_HUNT, "Zombie family", false, false),
    SKELETON_FAMILY(WorldTierStage.OVERWORLD_HUNT, "Skeleton family", false, false),
    SPIDER_FAMILY(WorldTierStage.OVERWORLD_HUNT, "Spider family", false, false),
    CREEPER(WorldTierStage.OVERWORLD_HUNT, "Creeper", false, false),
    ENDERMAN_OVERWORLD(WorldTierStage.OVERWORLD_HUNT, "Overworld Enderman", false, false),
    WITCH(WorldTierStage.OVERWORLD_HUNT, "Witch", false, false),
    ILLAGER(WorldTierStage.OVERWORLD_HUNT, "Illager family", false, false),

    NETHER_TOTAL(WorldTierStage.NETHER_CONQUEST, "Total Nether hostiles", false, true),
    PIGLIN_FAMILY(WorldTierStage.NETHER_CONQUEST, "Piglin family", false, false),
    ZOMBIFIED_PIGLIN(WorldTierStage.NETHER_CONQUEST, "Zombified Piglin", false, false),
    BLAZE(WorldTierStage.NETHER_CONQUEST, "Blaze", false, false),
    MAGMA_CUBE(WorldTierStage.NETHER_CONQUEST, "Magma Cube", false, false),
    GHAST(WorldTierStage.NETHER_CONQUEST, "Ghast", false, false),
    WITHER_SKELETON(WorldTierStage.NETHER_CONQUEST, "Wither Skeleton", false, false),
    WITHER(WorldTierStage.NETHER_CONQUEST, "Wither", true, false),
    INFERNAL_CRUCIBLE(WorldTierStage.NETHER_CONQUEST, "The Infernal Crucible", true, false),

    ENDERMAN_END(WorldTierStage.END_CONQUEST, "Enderman in The End", false, false),
    SHULKER(WorldTierStage.END_CONQUEST, "Shulker", false, false),
    ENDER_DRAGON(WorldTierStage.END_CONQUEST, "Ender Dragon", true, false),
    AETHERIC_BULWARK(WorldTierStage.END_CONQUEST, "Aetheric Bulwark", true, false),

    GUARDIAN(WorldTierStage.DEEP_WORLD_CONQUEST, "Guardian", false, false),
    ELDER_GUARDIAN(WorldTierStage.DEEP_WORLD_CONQUEST, "Elder Guardian", true, false),
    WARDEN(WorldTierStage.DEEP_WORLD_CONQUEST, "Warden", true, false),
    DEEPBOUND_WARDEN(WorldTierStage.DEEP_WORLD_CONQUEST, "Deepbound Warden", true, false);

    private final WorldTierStage stage;
    private final String displayName;
    private final boolean bossObjective;
    private final boolean totalObjective;

    WorldProgressionObjective(WorldTierStage stage, String displayName,
                              boolean bossObjective, boolean totalObjective) {
        this.stage = stage;
        this.displayName = displayName;
        this.bossObjective = bossObjective;
        this.totalObjective = totalObjective;
    }

    public WorldTierStage stage() { return stage; }
    public String displayName() { return displayName; }
    public boolean bossObjective() { return bossObjective; }
    public boolean totalObjective() { return totalObjective; }
}
