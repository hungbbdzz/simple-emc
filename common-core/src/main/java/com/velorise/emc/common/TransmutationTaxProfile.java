package com.velorise.emc.common;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Loader-independent materialization tax-meter math.
 *
 * <p>Every successfully materialized item adds one point to a per-player meter.
 * The meter recovers continuously in whole-second steps. Each item in a batch is
 * charged the flat EMC surcharge for the point total reached by that item, so a
 * large request is split correctly across all configured thresholds.</p>
 */
public final class TransmutationTaxProfile {
    public static final long MILLIS_PER_SECOND = 1_000L;

    private TransmutationTaxProfile() {}

    public static Settings settings(EmcValueMode mode) {
        GameplayConfig.TransmutationTax config = GameplayConfig.current().economy.transmutationTax;
        boolean enabled = mode == EmcValueMode.BALANCED
            ? config.enabledInBalanced
            : config.enabledInDefault;
        return new Settings(
            enabled,
            config.recoveryPointsPerSecond,
            config.pointThresholds,
            config.extraEmcPerItem
        );
    }

    /**
     * Recovers meter points using elapsed whole seconds while preserving partial seconds.
     * When the meter reaches zero, the timestamp is re-anchored to now so old idle time
     * cannot immediately consume points added by a later materialization.
     */
    public static DecayResult decay(long points, long lastDecayMillis, long nowMillis, Settings settings) {
        long safeNow = Math.max(0L, nowMillis);
        if (!settings.enabled()) return new DecayResult(0L, safeNow);

        long safePoints = Math.max(0L, points);
        if (lastDecayMillis <= 0L) return new DecayResult(safePoints, safeNow);
        if (safePoints == 0L) return new DecayResult(0L, safeNow);
        if (safeNow <= lastDecayMillis) return new DecayResult(safePoints, lastDecayMillis);

        long elapsedSeconds = (safeNow - lastDecayMillis) / MILLIS_PER_SECOND;
        if (elapsedSeconds <= 0L) return new DecayResult(safePoints, lastDecayMillis);

        long recovered = saturatingMultiply(elapsedSeconds, settings.recoveryPointsPerSecond());
        long remaining = Math.max(0L, safePoints - Math.min(safePoints, recovered));
        if (remaining == 0L) return new DecayResult(0L, safeNow);

        long consumedMillis = saturatingMultiply(elapsedSeconds, MILLIS_PER_SECOND);
        long nextTimestamp = saturatingAdd(lastDecayMillis, consumedMillis);
        if (nextTimestamp > safeNow) nextTimestamp = safeNow;
        return new DecayResult(remaining, nextTimestamp);
    }

    /** Returns the surcharge paid by an item that raises the meter to {@code resultingPoints}. */
    public static long surchargeForPoints(long resultingPoints, Settings settings) {
        if (!settings.enabled() || resultingPoints <= 0L) return 0L;

        int tier = 0;
        long[] thresholds = settings.pointThresholds();
        while (tier < thresholds.length && resultingPoints > thresholds[tier]) tier++;

        long[] surcharges = settings.extraEmcPerItem();
        return surcharges[Math.min(tier, surcharges.length - 1)];
    }

    /** Returns the surcharge for the next item materialized at the current meter value. */
    public static long surchargePerNextItem(long currentPoints, Settings settings) {
        return surchargeForPoints(saturatingAdd(Math.max(0L, currentPoints), 1L), settings);
    }

    /** Quotes a request and splits it across point thresholds when necessary. */
    public static Quote quote(long unitEmc, int count, long startingPoints, Settings settings) {
        long points = settings.enabled() ? Math.max(0L, startingPoints) : 0L;
        long initialPoints = points;
        if (unitEmc <= 0L || count <= 0) {
            long surcharge = surchargePerNextItem(points, settings);
            return new Quote(0, points, points, 0L, 0L, 0L, surcharge, surcharge);
        }

        BigInteger baseTotal = BigInteger.valueOf(unitEmc).multiply(BigInteger.valueOf(count));
        BigInteger taxTotal = BigInteger.ZERO;

        if (settings.enabled()) {
            for (int index = 0; index < count; index++) {
                long resultingPoints = saturatingAdd(points, index + 1L);
                taxTotal = taxTotal.add(BigInteger.valueOf(surchargeForPoints(resultingPoints, settings)));
            }
            points = saturatingAdd(points, count);
        }

        BigInteger total = baseTotal.add(taxTotal);
        return new Quote(
            count,
            initialPoints,
            points,
            clampLong(baseTotal),
            clampLong(taxTotal),
            clampLong(total),
            surchargePerNextItem(initialPoints, settings),
            surchargePerNextItem(points, settings)
        );
    }

    public static int maxAffordableCount(long unitEmc, int maximumCount, long startingPoints,
                                         long availableEmc, Settings settings) {
        if (unitEmc <= 0L || maximumCount <= 0 || availableEmc <= 0L) return 0;
        int affordable = 0;
        for (int count = 1; count <= maximumCount; count++) {
            if (quote(unitEmc, count, startingPoints, settings).totalCost() > availableEmc) break;
            affordable = count;
        }
        return affordable;
    }

    public static String formatSurcharge(long surchargePerItem) {
        return "+" + Math.max(0L, surchargePerItem) + " EMC/item";
    }

    public static long meterMaximum(Settings settings) {
        long[] thresholds = settings.pointThresholds();
        return thresholds.length == 0 ? 1L : Math.max(1L, thresholds[thresholds.length - 1]);
    }

    private static long clampLong(BigInteger value) {
        if (value.signum() <= 0) return 0L;
        BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
        return value.compareTo(max) > 0 ? Long.MAX_VALUE : value.longValue();
    }

    private static long saturatingAdd(long left, long right) {
        long safeLeft = Math.max(0L, left);
        if (right <= 0L) return safeLeft;
        if (safeLeft > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return safeLeft + right;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    public record Settings(
        boolean enabled,
        long recoveryPointsPerSecond,
        long[] pointThresholds,
        long[] extraEmcPerItem
    ) {
        public Settings {
            if (recoveryPointsPerSecond <= 0L) {
                throw new IllegalArgumentException("Tax meter recovery must be positive");
            }
            if (pointThresholds == null || pointThresholds.length == 0) {
                throw new IllegalArgumentException("At least one tax threshold is required");
            }
            if (extraEmcPerItem == null || extraEmcPerItem.length != pointThresholds.length + 1) {
                throw new IllegalArgumentException("Tax surcharge count must be threshold count + 1");
            }

            pointThresholds = Arrays.copyOf(pointThresholds, pointThresholds.length);
            extraEmcPerItem = Arrays.copyOf(extraEmcPerItem, extraEmcPerItem.length);

            long previous = 0L;
            for (long threshold : pointThresholds) {
                if (threshold <= previous) throw new IllegalArgumentException("Tax thresholds must increase");
                previous = threshold;
            }
            for (long surcharge : extraEmcPerItem) {
                if (surcharge < 0L) throw new IllegalArgumentException("Tax surcharges cannot be negative");
            }
        }

        @Override
        public long[] pointThresholds() {
            return Arrays.copyOf(pointThresholds, pointThresholds.length);
        }

        @Override
        public long[] extraEmcPerItem() {
            return Arrays.copyOf(extraEmcPerItem, extraEmcPerItem.length);
        }
    }

    public record DecayResult(long usage, long lastDecayMillis) {}

    public record Quote(
        int count,
        long startingUsage,
        long endingUsage,
        long baseCost,
        long tax,
        long totalCost,
        long startingSurchargePerItem,
        long nextSurchargePerItem
    ) {}
}
