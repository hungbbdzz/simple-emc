package com.velorise.emc.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DeepboundStorageProfileTest {
    @Test
    void capacitiesAndPagesFollowCoreTier() {
        assertEquals(54, DeepboundStorageProfile.slotCount(WirelessCoreTier.I));
        assertEquals(81, DeepboundStorageProfile.slotCount(WirelessCoreTier.II));
        assertEquals(108, DeepboundStorageProfile.slotCount(WirelessCoreTier.III));
        assertEquals(162, DeepboundStorageProfile.slotCount(WirelessCoreTier.IV));

        assertEquals(2, DeepboundStorageProfile.pageCount(WirelessCoreTier.I));
        assertEquals(3, DeepboundStorageProfile.pageCount(WirelessCoreTier.II));
        assertEquals(4, DeepboundStorageProfile.pageCount(WirelessCoreTier.III));
        assertEquals(6, DeepboundStorageProfile.pageCount(WirelessCoreTier.IV));
    }

    @Test
    void pageAndSlotBoundsAreClamped() {
        assertEquals(0, DeepboundStorageProfile.clampPage(WirelessCoreTier.I, -5));
        assertEquals(1, DeepboundStorageProfile.clampPage(WirelessCoreTier.I, 99));
        assertTrue(DeepboundStorageProfile.isAccessibleSlot(WirelessCoreTier.III, 107));
        assertFalse(DeepboundStorageProfile.isAccessibleSlot(WirelessCoreTier.III, 108));
    }
}
