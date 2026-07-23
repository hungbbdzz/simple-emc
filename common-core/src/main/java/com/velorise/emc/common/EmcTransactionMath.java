package com.velorise.emc.common;

/** Pure arithmetic used by loader-specific EMC transaction services. */
public final class EmcTransactionMath {
    private EmcTransactionMath() {}

    public static boolean canSpend(long balance, long cost, long reserve) {
        if (cost <= 0L) return true;
        if (balance < 0L || reserve < 0L || balance < cost) return false;
        return balance - cost >= reserve;
    }

    public static long saturatingAdd(long value, long delta) {
        if (delta <= 0L) return value;
        return value > Long.MAX_VALUE - delta ? Long.MAX_VALUE : value + delta;
    }
}
