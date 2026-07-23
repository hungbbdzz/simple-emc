package com.velorise.emc.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResonantMachineProfileTest {
    @Test void clampsCoreTiers() {
        assertEquals(1, ResonantMachineProfile.clampCoreTier(-5));
        assertEquals(4, ResonantMachineProfile.clampCoreTier(99));
    }

    @Test void furnaceSpeedScalesByCore() {
        assertEquals(2, ResonantMachineProfile.furnaceSpeedMultiplier(1));
        assertEquals(4, ResonantMachineProfile.furnaceSpeedMultiplier(2));
        assertEquals(8, ResonantMachineProfile.furnaceSpeedMultiplier(3));
        assertEquals(16, ResonantMachineProfile.furnaceSpeedMultiplier(4));
    }
}
