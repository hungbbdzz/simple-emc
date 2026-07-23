package com.velorise.emc.common;

/**
 * Selects the built-in EMC economy used before server-side custom overrides.
 */
public enum EmcValueMode {
    DEFAULT(0, "Default"),
    BALANCED(1, "Balanced");

    private final int id;
    private final String displayName;

    EmcValueMode(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public EmcValueMode next() {
        return this == DEFAULT ? BALANCED : DEFAULT;
    }

    public static EmcValueMode byId(int id) {
        return id == BALANCED.id ? BALANCED : DEFAULT;
    }

    public static EmcValueMode byName(String name) {
        if (name == null) return DEFAULT;
        for (EmcValueMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) return mode;
        }
        return DEFAULT;
    }
}
