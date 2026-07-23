package com.velorise.emc.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImbuedMeleeProfileTest {
    @AfterEach
    void reset() {
        GameplayConfig.resetToDefaults();
    }

    @Test
    void defaultsUseGlobalMeleeBalance() {
        GameplayConfig.install(GameplayConfig.Values.defaults());

        assertFalse(ImbuedMeleeProfile.configuredMelee("example:katana"));
        assertFalse(ImbuedMeleeProfile.forceRepairOnly("example:katana"));
        assertEquals(3.0F, ImbuedMeleeProfile.bonusDamage("example:katana", 3));
        assertEquals(32_768L, ImbuedMeleeProfile.activeCostPerSecond("example:katana", 4));
    }

    @Test
    void registryRulesAndOverridesAreNormalizedAndInstalled() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        GameplayConfig.ImbuedMeleeCompatibility compatibility = values.items.imbuedMeleeCompatibility;
        compatibility.forceInclude = new String[]{" Example:Katana ", "invalid", "example:katana"};
        compatibility.forceRepairOnly = new String[]{"Example:Training_Sword"};
        compatibility.itemOverrides = new LinkedHashMap<>();

        GameplayConfig.MeleeItemOverride override = new GameplayConfig.MeleeItemOverride();
        override.bonusDamage = new float[]{2.0F, 4.0F, 6.0F, 8.0F};
        override.activeCostPerSecond = new long[]{10L, 20L, 30L, 40L};
        compatibility.itemOverrides.put("Example:Greatsword", override);

        GameplayConfig.install(values);

        assertTrue(ImbuedMeleeProfile.configuredMelee("example:katana"));
        assertTrue(ImbuedMeleeProfile.forceRepairOnly("example:training_sword"));
        assertTrue(ImbuedMeleeProfile.configuredMelee("example:greatsword"));
        assertEquals(6.0F, ImbuedMeleeProfile.bonusDamage("example:greatsword", 3));
        assertEquals(40L, ImbuedMeleeProfile.activeCostPerSecond("example:greatsword", 4));
        assertFalse(ImbuedMeleeProfile.configuredMelee("invalid"));
    }

    @Test
    void partialOverrideFallsBackToGlobalFieldValues() {
        GameplayConfig.Values values = GameplayConfig.Values.defaults();
        GameplayConfig.MeleeItemOverride override = new GameplayConfig.MeleeItemOverride();
        override.bonusDamage = new float[]{5.0F};
        values.items.imbuedMeleeCompatibility.itemOverrides.put("example:blade", override);
        GameplayConfig.install(values);

        assertEquals(5.0F, ImbuedMeleeProfile.bonusDamage("example:blade", 1));
        assertEquals(2.0F, ImbuedMeleeProfile.bonusDamage("example:blade", 2));
        assertEquals(4_096L, ImbuedMeleeProfile.activeCostPerSecond("example:blade", 3));
    }
}
