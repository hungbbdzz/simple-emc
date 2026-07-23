package com.velorise.emc.tests;

import com.velorise.emc.common.ArcaneWeaponProfile;
import com.velorise.emc.common.CoreAuraMachineProfile;
import com.velorise.emc.common.CustomUtilityProfile;
import com.velorise.emc.common.EmcValueMode;
import com.velorise.emc.common.GameplayConfig;
import com.velorise.emc.common.HourglassProfile;
import com.velorise.emc.common.PurificationProfile;
import com.velorise.emc.common.ResonantEquipmentProfile;
import com.velorise.emc.common.ResonantMachineProfile;
import com.velorise.emc.common.TransmutationTaxProfile;
import com.velorise.emc.common.UtilityItemProfile;
import com.velorise.emc.common.WirelessCoreProfile;
import com.velorise.emc.common.WirelessCoreTier;

/** Dependency-free alpha.61 balance and structural smoke test. */
public final class RefactorBaselineSmoke {
    private RefactorBaselineSmoke() {}

    public static void main(String[] args) {
        GameplayConfig.resetToDefaults();
        check(GameplayConfig.current().configVersion == 23, "gameplay config version");

        check(WirelessCoreProfile.nextTier(WirelessCoreTier.I).orElseThrow() == WirelessCoreTier.II,
            "Core I upgrades to Core II");
        check(WirelessCoreProfile.nextTier(WirelessCoreTier.III).orElseThrow() == WirelessCoreTier.IV,
            "Core III upgrades to Core IV");
        check(WirelessCoreProfile.nextTier(WirelessCoreTier.IV).isEmpty(), "Core IV is maximum");

        check(ResonantEquipmentProfile.ingotEmc(1) == 32_768L, "Verdant baseline EMC");
        check(ResonantEquipmentProfile.ingotEmc(4) == 262_144L, "Echoing baseline EMC");
        check(!ResonantEquipmentProfile.fireproof(1), "Verdant remains burnable");
        check(ResonantEquipmentProfile.fireproof(2), "Infernal and higher are fireproof");

        int[] speed = {2, 4, 8, 16};
        for (int tier = 1; tier <= 4; tier++) {
            check(ResonantMachineProfile.furnaceBatchSize(tier) == speed[tier - 1],
                "resonant furnace speed tier " + tier);
            check(ArcaneWeaponProfile.range(ArcaneWeaponProfile.Kind.DRAGONFIRE, tier) > 0.0,
                "Dragonfire range tier " + tier);
            check(ArcaneWeaponProfile.emcCost(ArcaneWeaponProfile.Kind.DRAGONFIRE) > 0L,
                "Dragonfire cost");
        }
        check(ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.ECHO, 1) == 10.0F,
            "Resonant Echo Core I damage");
        check(ArcaneWeaponProfile.witherBurstShots() == 2,
            "Wither Scepter casts before reload");
        check(ArcaneWeaponProfile.witherSkullCount(4) == 4,
            "Wither Scepter Core IV skull count");
        check(ArcaneWeaponProfile.shulkerShotCount(4) == 4,
            "Shulker Launcher Core IV volley");
        check(ArcaneWeaponProfile.guardianBeamDurationTicks(1) == 120,
            "Guardian Lens Core I channel duration");
        check(ArcaneWeaponProfile.evokerWaveCount(1) == 1
                && ArcaneWeaponProfile.evokerWaveCount(2) == 2
                && ArcaneWeaponProfile.evokerWaveCount(3) == 3
                && ArcaneWeaponProfile.evokerWaveCount(4) == 4,
            "Evoker Fang Staff Core I-IV waves");
        check(ArcaneWeaponProfile.lightningBurstUses(4) == 4,
            "Stormcaller Core IV calls before reload");
        check(ArcaneWeaponProfile.frostBurstUses(1) == 1
                && ArcaneWeaponProfile.frostBurstUses(2) == 2
                && ArcaneWeaponProfile.frostBurstUses(3) == 3
                && ArcaneWeaponProfile.frostBurstUses(4) == 4,
            "Frostbolt Core I-IV shots before reload");
        check(ArcaneWeaponProfile.ghastfireRadius(1) == 3.0D
                && ArcaneWeaponProfile.ghastfireRadius(4) == 6.0D
                && ArcaneWeaponProfile.damage(ArcaneWeaponProfile.Kind.GHASTFIRE, 4) == 12.0F,
            "Ghastfire Core IV radius and damage");
        check(ArcaneWeaponProfile.webDurationTicks(4) == 1_200,
            "Webcaster Core IV lifetime");

        check(ResonantMachineProfile.furnaceEmcPerCompletedItem() == 16,
            "resonant furnace fixed per-item cost");


        check(HourglassProfile.radius(0) == 2, "Hourglass usable without Core");
        check(HourglassProfile.radius(4) == 10, "Hourglass Core IV radius");
        check(PurificationProfile.operationCost(4) > 0L, "Purification Grindstone supports Core IV");

        check(CustomUtilityProfile.attractionDiameter(1) >= 5, "Attraction Catalyst Tier I area");
        check(CustomUtilityProfile.translocatorRange(4) >= CustomUtilityProfile.translocatorRange(1),
            "Void Translocator scales through Tier IV");
        check(UtilityItemProfile.fishingCatches(4) == 16, "Fishing Rod Tier IV catches");

        TransmutationTaxProfile.Settings tax = TransmutationTaxProfile.settings(EmcValueMode.BALANCED);
        check(tax.enabled(), "Balanced tax enabled");
        check(TransmutationTaxProfile.surchargeForPoints(65L, tax) == 2L, "first tax tier");
        check(TransmutationTaxProfile.surchargeForPoints(2_048L, tax) == 64L, "maximum tax tier");
        check(TransmutationTaxProfile.decay(100L, 1_000L, 11_000L, tax).usage() == 90L,
            "tax recovers one point per second");

        System.out.println("Velorise EMC alpha.61 common-core smoke tests passed.");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
