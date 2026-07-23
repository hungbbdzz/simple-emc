package com.velorise.emc.common;

public final class ArcaneWeaponProfileTest {
    public static void main(String[] args) {
        GameplayConfig.resetToDefaults();
        require(ArcaneWeaponProfile.cooldownTicks() == 120, "cooldown");
        require(ArcaneWeaponProfile.emcCost(ArcaneWeaponProfile.Kind.DRAGONFIRE, 1) == 256L, "dragonfire Core I cost");
        require(ArcaneWeaponProfile.emcCost(ArcaneWeaponProfile.Kind.DRAGONFIRE, 4) == 2_048L, "dragonfire Core IV cost");
        require(ArcaneWeaponProfile.emcCost(ArcaneWeaponProfile.Kind.GUARDIAN, 1) == 24L, "guardian Core I EMC per second");
        require(ArcaneWeaponProfile.emcCost(ArcaneWeaponProfile.Kind.GUARDIAN, 4) == 192L, "guardian Core IV EMC per second");
        require(ArcaneWeaponProfile.emcCost(ArcaneWeaponProfile.Kind.SHULKER, 4) == 768L, "shulker control-cost curve");
        require(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.DRAGONFIRE, 4) == 20.0F, "dragonfire damage");
        require(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.ECHO, 1) == 10.0F, "echo Core I damage");
        require(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.ECHO, 4) == 25.0F, "echo Core IV damage");
        require(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.INFERNO, 4) == 6.0F, "inferno per-shot damage");
        require(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.GHASTFIRE, 4) == 12.0F, "gentle ghastfire damage scaling");
        require(ArcaneWeaponProfile.ghastfireRadius(1) == 3.0D, "ghastfire Core I radius");
        require(ArcaneWeaponProfile.ghastfireRadius(4) == 6.0D, "ghastfire Core IV radius");
        require(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.SHULKER, 1)
            == ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.SHULKER, 4), "shulker damage stays fixed");
        require(ArcaneWeaponProfile.shulkerLevitationSeconds(4) == 6, "shulker duration scales");
        require(ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.DRAGONFIRE, 4) == 30.0D, "dragonfire range");
        require(ArcaneWeaponProfile.witherBurstShots() == 2, "wither burst shots");
        require(ArcaneWeaponProfile.witherSkullCount(4) == 4, "wither skull count");
        require(ArcaneWeaponProfile.shulkerShotCount(4) == 4, "shulker shot count");
        require(ArcaneWeaponProfile.evokerWaveCount(1) == 1, "evoker Core I wave count");
        require(ArcaneWeaponProfile.evokerWaveCount(2) == 2, "evoker Core II wave count");
        require(ArcaneWeaponProfile.evokerWaveCount(3) == 3, "evoker Core III wave count");
        require(ArcaneWeaponProfile.evokerWaveCount(4) == 4, "evoker Core IV wave count");
        require(ArcaneWeaponProfile.lightningBurstUses(4) == 4, "stormcaller burst uses");
        require(ArcaneWeaponProfile.guardianBeamDurationTicks(1) == 120, "guardian Core I duration");
        require(ArcaneWeaponProfile.guardianBeamDurationTicks(4) == 240, "guardian Core IV duration");
        require(ArcaneWeaponProfile.webDurationTicks(4) == 1_200, "web duration scaling");
        require(ArcaneWeaponProfile.webBurstShots() == 3, "web burst shots");
        require(CoreEquipmentProfile.armorBonus(4) == 8.0D, "armor bonus");
        require(CoreEquipmentProfile.elytraArmorBonus(4) == 8.0D, "elytra armor bonus");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
