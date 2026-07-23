package com.velorise.emc.common;

/** Shared balance profile for the Anvil + Wireless Core arcane weapon family. */
public final class ArcaneWeaponProfile {
    public enum Kind {
        DRAGONFIRE, ECHO, INFERNO, GHASTFIRE,
        WITHER, SHULKER, GUARDIAN, EVOKER, LIGHTNING, FROST, WEB
    }

    private ArcaneWeaponProfile() {}

    private static int index(int coreTier) {
        return Math.max(0, Math.min(3, coreTier - 1));
    }

    public static int cooldownTicks(Kind kind) {
        var values = GameplayConfig.current().items.arcaneWeapons;
        return switch (kind) {
            case INFERNO -> values.infernoCooldownTicks;
            case GHASTFIRE -> values.ghastfireCooldownTicks;
            case WITHER -> values.witherCooldownTicks;
            case SHULKER -> values.shulkerCooldownTicks;
            case GUARDIAN -> values.guardianCooldownTicks;
            case EVOKER -> values.evokerCooldownTicks;
            case LIGHTNING -> values.lightningCooldownTicks;
            case FROST -> values.frostCooldownTicks;
            case WEB -> values.webCooldownTicks;
            case DRAGONFIRE, ECHO -> values.cooldownTicks;
        };
    }

    /** Compatibility overload retained for older callers. */
    public static int cooldownTicks() {
        return cooldownTicks(Kind.DRAGONFIRE);
    }

    public static int chargeTicks(Kind kind) {
        var values = GameplayConfig.current().items.arcaneWeapons;
        return switch (kind) {
            case ECHO -> values.echoChargeTicks;
            default -> 0;
        };
    }

    public static int echoChargeTicks() {
        return chargeTicks(Kind.ECHO);
    }

    public static int infernoBurstShots() {
        return GameplayConfig.current().items.arcaneWeapons.infernoBurstShots;
    }

    public static int infernoBurstResetTicks() {
        return GameplayConfig.current().items.arcaneWeapons.infernoBurstResetTicks;
    }

    public static int witherBurstShots() {
        return GameplayConfig.current().items.arcaneWeapons.witherBurstShots;
    }

    public static int witherBurstResetTicks() {
        return GameplayConfig.current().items.arcaneWeapons.witherBurstResetTicks;
    }

    public static int lightningBurstResetTicks() {
        return GameplayConfig.current().items.arcaneWeapons.lightningBurstResetTicks;
    }

    public static int frostBurstResetTicks() {
        return GameplayConfig.current().items.arcaneWeapons.frostBurstResetTicks;
    }

    public static int webBurstShots() {
        return GameplayConfig.current().items.arcaneWeapons.webBurstShots;
    }

    public static int webBurstResetTicks() {
        return GameplayConfig.current().items.arcaneWeapons.webBurstResetTicks;
    }

    public static int guardianBeamDurationTicks(int coreTier) {
        return GameplayConfig.current().items.arcaneWeapons.guardianBeamDurationTicks[index(coreTier)];
    }

    public static long guardianEmcPerPulse(int coreTier) {
        return GameplayConfig.current().items.arcaneWeapons.guardianEmcPerSecond[index(coreTier)] / 4L;
    }

    public static long emcCost(Kind kind, int coreTier) {
        int i = index(coreTier);
        var values = GameplayConfig.current().items.arcaneWeapons;
        return switch (kind) {
            case INFERNO -> values.infernoEmcCost[i];
            case GHASTFIRE -> values.ghastfireEmcCost[i];
            case DRAGONFIRE -> values.dragonfireEmcCost[i];
            case ECHO -> values.echoEmcCost[i];
            case WITHER -> values.witherEmcCost[i];
            case SHULKER -> values.shulkerEmcCost[i];
            case GUARDIAN -> values.guardianEmcPerSecond[i];
            case EVOKER -> values.evokerEmcCost[i];
            case LIGHTNING -> values.lightningEmcCost[i];
            case FROST -> values.frostEmcCost[i];
            case WEB -> values.webEmcCost[i];
        };
    }

    /** Compatibility overload: an untagged crafted weapon is Core I. */
    public static long emcCost(Kind kind) {
        return emcCost(kind, 1);
    }

    public static float damage(Kind kind, int coreTier) {
        int i = index(coreTier);
        return switch (kind) {
            case DRAGONFIRE -> GameplayConfig.current().items.arcaneWeapons.dragonfireDamage[i];
            case ECHO -> GameplayConfig.current().items.arcaneWeapons.echoDamage[i];
            case INFERNO -> GameplayConfig.current().items.arcaneWeapons.infernoDamage[i];
            case GHASTFIRE -> GameplayConfig.current().items.arcaneWeapons.ghastfireDamage[i];
            case WITHER -> GameplayConfig.current().items.arcaneWeapons.witherDamage[i];
            case SHULKER -> GameplayConfig.current().items.arcaneWeapons.shulkerDamage[i];
            case GUARDIAN -> GameplayConfig.current().items.arcaneWeapons.guardianDamage[i];
            case LIGHTNING -> GameplayConfig.current().items.arcaneWeapons.lightningDamage[i];
            case FROST -> GameplayConfig.current().items.arcaneWeapons.frostDamage[i];
            case EVOKER, WEB -> 0.0F;
        };
    }

    public static double range(Kind kind, int coreTier) {
        int i = index(coreTier);
        var values = GameplayConfig.current().items.arcaneWeapons;
        return switch (kind) {
            case DRAGONFIRE -> values.dragonfireRange[i];
            case INFERNO -> values.infernoRange[i];
            case GHASTFIRE -> values.ghastfireRange[i];
            case WITHER -> values.witherRange[i];
            case SHULKER -> values.shulkerRange[i];
            case GUARDIAN -> values.guardianRange[i];
            case EVOKER -> values.evokerRange[i];
            case LIGHTNING -> values.lightningRange[i];
            case FROST -> values.frostRange[i];
            case WEB -> values.webRange[i];
            case ECHO -> values.echoRange;
        };
    }

    public static double echoRange() { return GameplayConfig.current().items.arcaneWeapons.echoRange; }
    public static double echoBeamRadius() { return GameplayConfig.current().items.arcaneWeapons.echoBeamRadius; }
    public static double dragonfireRadius(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.dragonfireRadius[index(coreTier)]; }
    public static double dragonfireRange(int coreTier) { return range(Kind.DRAGONFIRE, coreTier); }
    public static double infernoRange(int coreTier) { return range(Kind.INFERNO, coreTier); }
    public static double ghastfireRange(int coreTier) { return range(Kind.GHASTFIRE, coreTier); }
    public static double ghastfireRadius(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.ghastfireRadius[index(coreTier)]; }
    public static double witherRadius(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.witherRadius[index(coreTier)]; }
    public static double lightningRadius(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.lightningRadius[index(coreTier)]; }

    public static int dragonfireCloudDurationTicks() { return GameplayConfig.current().items.arcaneWeapons.dragonfireCloudDurationTicks; }
    public static int dragonfirePulseIntervalTicks() { return GameplayConfig.current().items.arcaneWeapons.dragonfirePulseIntervalTicks; }
    public static float dragonfirePulseDamageMultiplier() { return GameplayConfig.current().items.arcaneWeapons.dragonfirePulseDamageMultiplier; }
    public static int infernoFireSeconds() { return GameplayConfig.current().items.arcaneWeapons.infernoFireSeconds; }
    public static int ghastfireFireSeconds() { return GameplayConfig.current().items.arcaneWeapons.ghastfireFireSeconds; }
    public static int witherSeconds(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.witherSeconds[index(coreTier)]; }
    public static int shulkerLevitationSeconds(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.shulkerLevitationSeconds[index(coreTier)]; }
    public static int frostSlowSeconds(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.frostSlowSeconds[index(coreTier)]; }
    public static int webDurationTicks(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.webDurationTicks[index(coreTier)]; }
    public static int webVenomDurationTicks(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.webVenomDurationTicks[index(coreTier)]; }
    public static int webPoisonAmplifier(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.webPoisonAmplifier[index(coreTier)]; }
    public static int evokerFangCount(int coreTier) { return GameplayConfig.current().items.arcaneWeapons.evokerFangCount[index(coreTier)]; }
    public static int witherSkullCount(int coreTier) { return index(coreTier) + 1; }
    public static int shulkerShotCount(int coreTier) { return index(coreTier) + 1; }
    public static int evokerWaveCount(int coreTier) { return index(coreTier) + 1; }
    public static int lightningBurstUses(int coreTier) { return index(coreTier) + 1; }
    public static int frostBurstUses(int coreTier) { return index(coreTier) + 1; }

}
