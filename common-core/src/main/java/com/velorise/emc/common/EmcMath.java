package com.velorise.emc.common;

/**
 * Loader-independent helpers for EMC arithmetic.
 */
public final class EmcMath {
    private EmcMath() {
    }

    /**
     * Adds a non-negative EMC amount without overflowing and clamps the result to {@code maximum}.
     */
    public static long saturatingAdd(long current, long amount, long maximum) {
        if (current < 0L || amount < 0L || maximum < 0L) {
            throw new IllegalArgumentException("EMC values must be non-negative");
        }
        if (current >= maximum || amount == 0L) {
            return Math.min(current, maximum);
        }
        long remaining = maximum - current;
        return amount >= remaining ? maximum : current + amount;
    }
    /** Multiplies non-negative EMC values without overflowing and clamps to {@code maximum}. */
    public static long saturatingMultiply(long left, long right, long maximum) {
        if (left < 0L || right < 0L || maximum < 0L) {
            throw new IllegalArgumentException("EMC values must be non-negative");
        }
        if (left == 0L || right == 0L) return 0L;
        if (left > maximum / right) return maximum;
        return Math.min(maximum, left * right);
    }

}
