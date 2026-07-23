package com.velorise.emc.common;

public final class ResonanceJukeboxProfileTest {
    public static void main(String[] args) {
        require(ResonanceJukeboxProfile.discSlots(1) == 8, "Core I slots");
        require(ResonanceJukeboxProfile.discSlots(2) == 12, "Core II slots");
        require(ResonanceJukeboxProfile.discSlots(3) == 16, "Core III slots");
        require(ResonanceJukeboxProfile.discSlots(4) == 20, "Core IV slots");
        require(ResonanceJukeboxProfile.DISC_SLOTS == 20, "physical playlist slots");
        require(ResonanceJukeboxProfile.FUEL_SLOT == 20, "fuel slot follows playlist");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
