package com.velorise.emc.common;

public final class TransmutationTaxProfileTest {
    public static void main(String[] args) {
        GameplayConfig.resetToDefaults();
        TransmutationTaxProfile.Settings balanced = TransmutationTaxProfile.settings(EmcValueMode.BALANCED);
        TransmutationTaxProfile.Settings defaults = TransmutationTaxProfile.settings(EmcValueMode.DEFAULT);

        require(balanced.enabled(), "balanced tax enabled");
        require(!defaults.enabled(), "default tax disabled");
        require(balanced.recoveryPointsPerSecond() == 1L, "one point recovered per second");
        require(TransmutationTaxProfile.surchargePerNextItem(0L, balanced) == 0L, "first item free");
        require(TransmutationTaxProfile.surchargePerNextItem(63L, balanced) == 0L, "item 64 free");
        require(TransmutationTaxProfile.surchargePerNextItem(64L, balanced) == 2L, "item 65 surcharge");
        require(TransmutationTaxProfile.surchargePerNextItem(128L, balanced) == 4L, "item 129 surcharge");
        require(TransmutationTaxProfile.surchargePerNextItem(256L, balanced) == 8L, "item 257 surcharge");
        require(TransmutationTaxProfile.surchargePerNextItem(512L, balanced) == 32L, "item 513 surcharge");
        require(TransmutationTaxProfile.surchargePerNextItem(1_024L, balanced) == 64L, "item 1025 surcharge");
        require(TransmutationTaxProfile.surchargePerNextItem(10_000L, defaults) == 0L, "default stays untaxed");

        TransmutationTaxProfile.Quote firstStack = TransmutationTaxProfile.quote(3L, 64, 0L, balanced);
        require(firstStack.baseCost() == 192L, "first stack base cost");
        require(firstStack.tax() == 0L, "first stack tax free");

        TransmutationTaxProfile.Quote secondStack = TransmutationTaxProfile.quote(3L, 64, 64L, balanced);
        require(secondStack.tax() == 128L, "second stack two EMC per item");

        TransmutationTaxProfile.Quote fiveHundred = TransmutationTaxProfile.quote(1L, 500, 0L, balanced);
        require(fiveHundred.tax() == 2_592L, "five hundred items split across all active tiers");
        require(fiveHundred.endingUsage() == 500L, "five hundred meter points recorded");
        require(fiveHundred.nextSurchargePerItem() == 8L, "five hundred points remain in eight EMC tier");

        TransmutationTaxProfile.Quote crossedTier = TransmutationTaxProfile.quote(1_000L, 4, 62L, balanced);
        require(crossedTier.baseCost() == 4_000L, "cross-tier base cost");
        require(crossedTier.tax() == 4L, "cross-tier flat surcharge");
        require(crossedTier.endingUsage() == 66L, "cross-tier usage");

        long start = 1_000_000L;
        TransmutationTaxProfile.DecayResult partial = TransmutationTaxProfile.decay(
            500L, start, start + 244_999L, balanced);
        require(partial.usage() == 256L, "whole-second point recovery");
        require(partial.lastDecayMillis() == start + 244_000L, "partial second preserved");
        require(TransmutationTaxProfile.surchargePerNextItem(partial.usage(), balanced) == 8L,
            "tier does not drop until enough points recover");

        TransmutationTaxProfile.DecayResult lowerTier = TransmutationTaxProfile.decay(
            partial.usage(), partial.lastDecayMillis(), start + 245_000L, balanced);
        require(lowerTier.usage() == 255L, "next second recovers one more point");
        require(TransmutationTaxProfile.surchargePerNextItem(lowerTier.usage(), balanced) == 4L,
            "tax falls to the next lower tier");

        TransmutationTaxProfile.DecayResult empty = TransmutationTaxProfile.decay(
            10L, start, start + 30_000L, balanced);
        require(empty.usage() == 0L, "meter reaches zero");
        require(empty.lastDecayMillis() == start + 30_000L, "empty meter re-anchors to now");

        int affordable = TransmutationTaxProfile.maxAffordableCount(10L, 64, 64L, 120L, balanced);
        require(affordable == 10, "maximum affordable count includes flat tax");
        require("+32 EMC/item".equals(TransmutationTaxProfile.formatSurcharge(32L)), "surcharge formatting");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Failed: " + message);
    }
}
