package com.velorise.emc.common;

import java.util.Map;

public final class FishingEconomyProfileTest {
    private static final Map<String, Long> EMC = Map.ofEntries(
        Map.entry("cod", 10L),
        Map.entry("salmon", 20L),
        Map.entry("tropical_fish", 30L),
        Map.entry("pufferfish", 40L),
        Map.entry("lily_pad", 1L),
        Map.entry("leather_boots", 2L),
        Map.entry("leather", 3L),
        Map.entry("bone", 4L),
        Map.entry("potion", 5L),
        Map.entry("string", 6L),
        Map.entry("fishing_rod", 7L),
        Map.entry("bowl", 8L),
        Map.entry("stick", 9L),
        Map.entry("ink_sac", 10L),
        Map.entry("tripwire_hook", 11L),
        Map.entry("rotten_flesh", 12L),
        Map.entry("name_tag", 100L),
        Map.entry("saddle", 200L),
        Map.entry("bow", 300L),
        Map.entry("enchanted_book", 400L),
        Map.entry("nautilus_shell", 500L)
    );

    public static void main(String[] args) {
        require(FishingEconomyProfile.expectedBaitEmc(0, key -> EMC.getOrDefault(key, 0L)) == 27L,
            "Luck 0 expected value");
        require(FishingEconomyProfile.expectedBaitEmc(1, key -> EMC.getOrDefault(key, 0L)) == 33L,
            "Luck I expected value");
        require(FishingEconomyProfile.expectedBaitEmc(2, key -> EMC.getOrDefault(key, 0L)) == 38L,
            "Luck II expected value");
        require(FishingEconomyProfile.expectedBaitEmc(3, key -> EMC.getOrDefault(key, 0L)) == 43L,
            "Luck III expected value");

        Map<String, Long> defaults = Map.ofEntries(
            Map.entry("cod", 64L),
            Map.entry("salmon", 64L),
            Map.entry("tropical_fish", 64L),
            Map.entry("pufferfish", 64L),
            Map.entry("lily_pad", 16L),
            Map.entry("leather_boots", 256L),
            Map.entry("leather", 64L),
            Map.entry("bone", 32L),
            Map.entry("potion", 64L),
            Map.entry("string", 16L),
            Map.entry("fishing_rod", 56L),
            Map.entry("bowl", 12L),
            Map.entry("stick", 8L),
            Map.entry("ink_sac", 16L),
            Map.entry("tripwire_hook", 76L),
            Map.entry("rotten_flesh", 16L),
            Map.entry("name_tag", 192L),
            Map.entry("saddle", 192L),
            Map.entry("bow", 72L),
            Map.entry("enchanted_book", 2_048L),
            Map.entry("nautilus_shell", 1_024L)
        );
        require(FishingEconomyProfile.expectedBaitEmc(0, key -> defaults.getOrDefault(key, 0L)) == 90L,
            "default Luck 0 bait");
        require(FishingEconomyProfile.expectedBaitEmc(1, key -> defaults.getOrDefault(key, 0L)) == 101L,
            "default Luck I bait");
        require(FishingEconomyProfile.expectedBaitEmc(2, key -> defaults.getOrDefault(key, 0L)) == 113L,
            "default Luck II bait");
        require(FishingEconomyProfile.expectedBaitEmc(3, key -> defaults.getOrDefault(key, 0L)) == 124L,
            "default Luck III bait");

        require(FishingEconomyProfile.totalStake(16, UtilityItemProfile.FISHING_BAIT_EMC) == UtilityItemProfile.FISHING_BAIT_EMC, "Core IV fixed catch stake");
        require(FishingEconomyProfile.totalStake(5, Long.MAX_VALUE) == Long.MAX_VALUE,
            "stake overflow saturation");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
