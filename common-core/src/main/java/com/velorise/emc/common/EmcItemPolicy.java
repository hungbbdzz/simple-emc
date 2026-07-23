package com.velorise.emc.common;

/**
 * Final server-authoritative permissions for an item participating in EMC.
 * Value calculation is intentionally kept separate from these permissions.
 */
public record EmcItemPolicy(
    boolean burnable,
    boolean learnable,
    boolean materializable,
    boolean visible,
    String source
) {
    public static EmcItemPolicy disabled(String source) {
        return new EmcItemPolicy(false, false, false, false, source);
    }

    public static EmcItemPolicy permissive(String source) {
        return new EmcItemPolicy(true, true, true, true, source);
    }
}
