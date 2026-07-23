package com.velorise.emc.common;

/** Flat armor granted by Core-bound wearable equipment. */
public final class CoreEquipmentProfile {
    private CoreEquipmentProfile() {}

    private static int index(int coreTier) {
        return Math.max(0, Math.min(3, coreTier - 1));
    }

    public static long repairEmcPerDurability(int coreTier) {
        return GameplayConfig.current().items.coreArmor.repairEmcPerDurability[index(coreTier)];
    }

    public static float weaponDamageBonus(int coreTier) {
        return GameplayConfig.current().items.coreArmor.weaponDamageBonus[index(coreTier)];
    }

    public static int hoeHarvestDiameter(int coreTier) {
        return GameplayConfig.current().items.imbuedMiningTools.hoeHarvestDiameter[index(coreTier)];
    }

    public static int hoeHarvestRadius(int coreTier) {
        return Math.max(0, (hoeHarvestDiameter(coreTier) - 1) / 2);
    }

    public static double armorBonus(int coreTier) {
        return GameplayConfig.current().items.coreArmor.armorBonus[index(coreTier)];
    }

    public static double elytraArmorBonus(int coreTier) {
        return GameplayConfig.current().items.coreArmor.elytraArmorBonus[index(coreTier)];
    }
}
