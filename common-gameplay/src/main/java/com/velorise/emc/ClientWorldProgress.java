package com.velorise.emc;

import com.velorise.emc.common.WorldProgressionState;

/** Client-side read-only snapshot installed by the normal player-data sync packet. */
public final class ClientWorldProgress {
    private static volatile WorldProgressionState current = WorldProgressionState.INITIAL;

    private ClientWorldProgress() {}

    public static WorldProgressionState current() { return current; }

    public static void install(long[] counts, int[] contributors) {
        current = new WorldProgressionState(counts, contributors);
    }

    public static void reset() { current = WorldProgressionState.INITIAL; }
}
