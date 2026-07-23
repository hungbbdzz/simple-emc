package com.velorise.emc.common;

import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * Loader-independent Vanilla fishing probability model used for balance audits.
 *
 * <p>The standard outside-Jungle, open-water loot tables are represented here.
 * Category weights follow Vanilla's entry weight + quality * luck formula:</p>
 *
 * <ul>
 *   <li>Fish: 85 - luck</li>
 *   <li>Junk: 10 - 2 * luck</li>
 *   <li>Treasure: 5 + 2 * luck</li>
 * </ul>
 *
 * <p>Individual item EMC values are resolved by the active loader at runtime, so
 * server overrides and recipe-derived EMC can be compared against the fixed runtime bait.</p>
 */
public final class FishingEconomyProfile {
    private static final WeightedLoot[] FISH = {
        loot("cod", 60, 1),
        loot("salmon", 25, 1),
        loot("tropical_fish", 2, 1),
        loot("pufferfish", 13, 1)
    };

    private static final WeightedLoot[] JUNK = {
        loot("lily_pad", 17, 1),
        loot("leather_boots", 10, 1),
        loot("leather", 10, 1),
        loot("bone", 10, 1),
        loot("potion", 10, 1),
        loot("string", 5, 1),
        loot("fishing_rod", 2, 1),
        loot("bowl", 10, 1),
        loot("stick", 5, 1),
        loot("ink_sac", 1, 10),
        loot("tripwire_hook", 10, 1),
        loot("rotten_flesh", 10, 1)
    };

    private static final WeightedLoot[] TREASURE = {
        loot("name_tag", 1, 1),
        loot("saddle", 1, 1),
        loot("bow", 1, 1),
        loot("fishing_rod", 1, 1),
        // Vanilla transforms the Book entry into an enchanted book.
        loot("enchanted_book", 1, 1),
        loot("nautilus_shell", 1, 1)
    };

    private FishingEconomyProfile() {}

    /** Returns the theoretical EMC expectation at the supplied Luck level. Runtime bait remains fixed. */
    public static long expectedBaitEmc(int luck, ToLongFunction<String> unitEmcResolver) {
        Objects.requireNonNull(unitEmcResolver, "unitEmcResolver");
        int safeLuck = Math.max(0, luck);

        double fishAverage = weightedAverage(FISH, unitEmcResolver);
        double junkAverage = weightedAverage(JUNK, unitEmcResolver);
        double treasureAverage = weightedAverage(TREASURE, unitEmcResolver);

        int fishWeight = Math.max(0, 85 - safeLuck);
        int junkWeight = Math.max(0, 10 - 2 * safeLuck);
        int treasureWeight = Math.max(0, 5 + 2 * safeLuck);
        int totalWeight = fishWeight + junkWeight + treasureWeight;
        if (totalWeight <= 0) return 1L;

        double expected = (fishAverage * fishWeight
            + junkAverage * junkWeight
            + treasureAverage * treasureWeight) / totalWeight;
        if (!Double.isFinite(expected) || expected <= 1.0D) return 1L;
        if (expected >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(1L, Math.round(expected));
    }

    /**
     * Fixed stake for a successful Core-powered catch. Core tier changes the number of
     * result copies, never the EMC price of that fishing action.
     */
    public static long fixedCatchStake(long configuredCost) {
        return Math.max(0L, configuredCost);
    }

    /** Retained for source compatibility with older balance audits. */
    public static long totalStake(int hookCount, long configuredCost) {
        return hookCount <= 0 ? 0L : fixedCatchStake(configuredCost);
    }

    private static double weightedAverage(WeightedLoot[] entries, ToLongFunction<String> resolver) {
        long totalWeight = 0L;
        double weightedValue = 0.0D;
        for (WeightedLoot entry : entries) {
            long unit = Math.max(0L, resolver.applyAsLong(entry.itemKey));
            double stackValue = unit * (double) entry.count;
            weightedValue += stackValue * entry.weight;
            totalWeight += entry.weight;
        }
        return totalWeight <= 0L ? 0.0D : weightedValue / totalWeight;
    }

    private static WeightedLoot loot(String itemKey, int weight, int count) {
        return new WeightedLoot(itemKey, weight, count);
    }

    private static final class WeightedLoot {
        private final String itemKey;
        private final int weight;
        private final int count;

        private WeightedLoot(String itemKey, int weight, int count) {
            this.itemKey = itemKey;
            this.weight = weight;
            this.count = count;
        }
    }
}
