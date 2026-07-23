package com.velorise.emc.common;

/** Loader-independent balance and control rules for the Resonance Jukebox. */
public final class ResonanceJukeboxProfile {
    public static final long MAX_EMC = 250_000L;
    public static final int DISC_SLOTS = 20;
    public static final int FUEL_SLOT = DISC_SLOTS;
    public static final int CONTAINER_SIZE = DISC_SLOTS + 1;
    private static final int[] DISC_SLOTS_BY_CORE = {8, 12, 16, 20};
    public static final int EMC_COST = 1;
    public static final int COST_INTERVAL_TICKS = 200;
    private ResonanceJukeboxProfile() {}
    public static long maxEmc() { return GameplayConfig.current().machines.resonanceJukebox.maxEmc; }
    public static int emcCost() { return GameplayConfig.current().machines.resonanceJukebox.emcCost; }
    public static int costIntervalTicks() { return GameplayConfig.current().machines.resonanceJukebox.costIntervalTicks; }
    public static int discSlots(int coreTier) {
        int index = Math.max(0, Math.min(3, coreTier - 1));
        return DISC_SLOTS_BY_CORE[index];
    }
    public static int clampVolume(int volume) { return Math.max(10, Math.min(100, volume)); }
    public static int cycleVolume(int volume, int delta) { return clampVolume(volume + delta); }
}
