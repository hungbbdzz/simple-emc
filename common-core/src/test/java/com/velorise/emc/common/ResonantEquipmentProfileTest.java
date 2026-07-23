package com.velorise.emc.common;

public final class ResonantEquipmentProfileTest {
    public static void main(String[] args) {
        assert ResonantEquipmentProfile.ingotEmc(1) == 32_768L;
        assert ResonantEquipmentProfile.ingotEmc(4) == 262_144L;
        assert ResonantEquipmentProfile.swordDamage(4) == 20.0F;
        assert ResonantEquipmentProfile.toolSpeed(3) == 15.0F;
        assert !ResonantEquipmentProfile.fireproof(1);
        assert ResonantEquipmentProfile.fireproof(2);
        int total = 0;
        for (int i = 0; i < 4; i++) total += ResonantEquipmentProfile.armorDurability(4, i);
        assert total == 3_800;
    }
}
