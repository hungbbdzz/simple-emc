package com.velorise.emc.common;

/**
 * Shared input rules for charge/level and cyclic selections.
 *
 * <p>Level values clamp at their ends. Cyclic selections wrap. The caller maps
 * V to {@code backwards == false} and Shift+V to {@code backwards == true}.</p>
 */
public final class InteractionRuleProfile {
    private InteractionRuleProfile() {}

    public static int stepLevel(int current, int min, int max, boolean backwards) {
        if (max < min) throw new IllegalArgumentException("max must be >= min");
        int clamped = Math.max(min, Math.min(max, current));
        return backwards ? Math.max(min, clamped - 1) : Math.min(max, clamped + 1);
    }

    public static int stepCycle(int current, int count, boolean backwards) {
        if (count <= 0) throw new IllegalArgumentException("count must be positive");
        int normalized = Math.floorMod(current, count);
        return Math.floorMod(normalized + (backwards ? -1 : 1), count);
    }
}
