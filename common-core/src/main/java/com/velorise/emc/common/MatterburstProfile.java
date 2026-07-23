package com.velorise.emc.common;

/** Core progression and per-tick terrain budget for Matterburst explosives. */
public final class MatterburstProfile {
    public static final int MAX_BLOCKS_PER_TICK = 8_192;
    private MatterburstProfile() {}
    private static int index(int coreTier) { return Math.max(1, Math.min(4, coreTier)) - 1; }
    public static int maxBlocksPerTick() { return GameplayConfig.current().machines.matterburst.maxBlocksPerTick; }
    public static int radius(int coreTier) { return GameplayConfig.current().machines.matterburst.radius[index(coreTier)]; }
    public static int depth(int coreTier) {
        var settings = GameplayConfig.current().machines.matterburst;
        return Math.max(settings.minimumDepth, Math.round(radius(coreTier) * settings.depthMultiplier));
    }
    public static float entityDamageRadius(int coreTier) {
        return radius(coreTier) * GameplayConfig.current().machines.matterburst.entityDamageRadiusMultiplier;
    }
    public static float maxDamage(int coreTier) {
        var settings = GameplayConfig.current().machines.matterburst;
        return settings.baseDamage + radius(coreTier) * settings.damagePerRadius;
    }
}
