package com.velorise.emc;

/** Compact, locale-neutral EMC labels for the small machine screens. */
public final class EmcTextFormat {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private EmcTextFormat() {}

    public static String compact(long value) {
        if (value < 1_000L) return String.format("%,d", value);
        double scaled = value;
        int suffix = 0;
        while (scaled >= 1_000.0D && suffix < SUFFIXES.length - 1) {
            scaled /= 1_000.0D;
            suffix++;
        }
        return scaled >= 100.0D
            ? String.format("%.0f%s", scaled, SUFFIXES[suffix])
            : String.format("%.1f%s", scaled, SUFFIXES[suffix]);
    }
}
