package com.velorise.emc.common;

public final class HourglassProfileTest {
    private HourglassProfileTest() {}

    public static void main(String[] args) {
        require(HourglassProfile.clampCoreLevel(-1) == 1, "Core I lower clamp");
        require(HourglassProfile.clampCoreLevel(9) == 4, "Core IV upper clamp");
        require(HourglassProfile.radius(0) == 4 && HourglassProfile.radius(4) == 10, "Core I-IV range table");
        require(HourglassProfile.pulseIntervalTicks() == 20, "One-second pulse");
        require(HourglassProfile.bonusTicksPerPulse() == 20, "Fixed +20 virtual ticks");
        require(HourglassProfile.emcPerSuccessfulTick() == 2L, "Successful-tick cost");
        int due = 0;
        for (int tick = 0; tick < 20; tick++) if (HourglassProfile.isPulseDue(tick, 12345L)) due++;
        require(due == 1, "Each Hourglass must pulse exactly once per second");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
