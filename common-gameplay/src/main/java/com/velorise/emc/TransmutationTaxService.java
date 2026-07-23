package com.velorise.emc;

import com.velorise.emc.common.EmcValueMode;
import com.velorise.emc.common.TransmutationTaxProfile;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared facade around the loader-independent tax meter and loader-specific PlayerEMC data. */
public final class TransmutationTaxService {
    private TransmutationTaxService() {}

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static EmcValueMode currentMode() {
        return EMCRegistry.getEffectiveValueMode();
    }

    public static long previewUsage(PlayerEMC data) {
        return previewUsage(data, nowMillis());
    }

    public static long previewUsage(PlayerEMC data, long nowMillis) {
        if (data == null) return 0L;
        return TransmutationTaxProfile.decay(
            data.getTransmutationUsage(),
            data.getTransmutationUsageDecayMillis(),
            nowMillis,
            TransmutationTaxProfile.settings(currentMode())
        ).usage();
    }

    public static TransmutationTaxProfile.Quote quote(PlayerEMC data, long unitEmc, int count) {
        return quote(data, unitEmc, count, nowMillis());
    }

    public static TransmutationTaxProfile.Quote quote(PlayerEMC data, long unitEmc, int count, long nowMillis) {
        return TransmutationTaxProfile.quote(
            unitEmc,
            count,
            previewUsage(data, nowMillis),
            TransmutationTaxProfile.settings(currentMode())
        );
    }

    public static int maxAffordableCount(PlayerEMC data, long unitEmc, int maximumCount) {
        return maxAffordableCount(data, unitEmc, maximumCount, nowMillis());
    }

    public static int maxAffordableCount(PlayerEMC data, long unitEmc, int maximumCount, long nowMillis) {
        if (data == null) return 0;
        return TransmutationTaxProfile.maxAffordableCount(
            unitEmc,
            maximumCount,
            previewUsage(data, nowMillis),
            data.getEmc(),
            TransmutationTaxProfile.settings(currentMode())
        );
    }

    /** Charges the exact request and adds one meter point per item materialized successfully. */
    public static ChargeResult charge(PlayerEMC data, long unitEmc, int count) {
        return charge(data, unitEmc, count, nowMillis());
    }

    public static ChargeResult charge(PlayerEMC data, long unitEmc, int count, long nowMillis) {
        if (data == null || unitEmc <= 0L || count <= 0) return ChargeResult.failed();

        applyDecay(data, nowMillis);
        TransmutationTaxProfile.Settings settings = TransmutationTaxProfile.settings(currentMode());
        TransmutationTaxProfile.Quote quote = TransmutationTaxProfile.quote(
            unitEmc,
            count,
            data.getTransmutationUsage(),
            settings
        );
        if (quote.totalCost() <= 0L || !data.removeEmc(quote.totalCost())) return ChargeResult.failed();

        data.setTransmutationUsage(quote.endingUsage());
        // Do not reset this timestamp on every withdrawal. The meter must keep recovering
        // every second even while materialization requests continue.
        data.setTransmutationUsageDecayMillis(settings.enabled()
            ? Math.max(1L, data.getTransmutationUsageDecayMillis())
            : 0L);
        data.setTransmutationTaxRemainder(0L); // legacy alpha field; percentage fractions are retired
        data.addMaterializedItems(count);
        data.addMaterializationTaxPaid(quote.tax());
        return new ChargeResult(true, quote);
    }

    public static void applyDecay(PlayerEMC data, long nowMillis) {
        if (data == null) return;
        TransmutationTaxProfile.DecayResult result = TransmutationTaxProfile.decay(
            data.getTransmutationUsage(),
            data.getTransmutationUsageDecayMillis(),
            nowMillis,
            TransmutationTaxProfile.settings(currentMode())
        );
        data.setTransmutationUsage(result.usage());
        data.setTransmutationUsageDecayMillis(result.lastDecayMillis());
        data.setTransmutationTaxRemainder(0L);
    }

    public static long currentPoints(PlayerEMC data) {
        return previewUsage(data);
    }

    public static long currentSurchargePerItem(PlayerEMC data) {
        return TransmutationTaxProfile.surchargePerNextItem(
            currentPoints(data),
            TransmutationTaxProfile.settings(currentMode())
        );
    }

    public static String currentTaxText(PlayerEMC data) {
        return TransmutationTaxProfile.formatSurcharge(currentSurchargePerItem(data));
    }

    public static long recoveryPointsPerSecond() {
        return TransmutationTaxProfile.settings(currentMode()).recoveryPointsPerSecond();
    }

    public static List<Component> meterTooltip(PlayerEMC data) {
        TransmutationTaxProfile.Settings settings = TransmutationTaxProfile.settings(currentMode());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Transmutation Tax"));
        if (!settings.enabled()) {
            lines.add(Component.literal("§7Disabled for the current EMC profile."));
            return lines;
        }
        lines.add(Component.literal("§7Current: §f" + String.format("%,d", currentPoints(data))
            + " points §8| §6" + currentTaxText(data)));
        lines.add(Component.literal("§7Recovery: §a" + String.format("%,d", settings.recoveryPointsPerSecond())
            + " point/s"));
        long[] thresholds = settings.pointThresholds();
        long[] surcharges = settings.extraEmcPerItem();
        long lower = 1L;
        for (int i = 0; i < thresholds.length; i++) {
            lines.add(Component.literal("§8" + String.format("%,d", lower) + "–"
                + String.format("%,d", thresholds[i]) + " points: §f"
                + TransmutationTaxProfile.formatSurcharge(surcharges[i])));
            lower = thresholds[i] + 1L;
        }
        lines.add(Component.literal("§8" + String.format("%,d", lower) + "+ points: §f"
            + TransmutationTaxProfile.formatSurcharge(surcharges[surcharges.length - 1])));
        return lines;
    }

    public static int meterFill(PlayerEMC data, int width) {
        if (width <= 0) return 0;
        long points = currentPoints(data);
        long maximum = TransmutationTaxProfile.meterMaximum(
            TransmutationTaxProfile.settings(currentMode()));
        if (points <= 0L) return 0;
        if (points >= maximum) return width;
        return (int) Math.min(width, (points * width) / maximum);
    }

    public record ChargeResult(boolean success, TransmutationTaxProfile.Quote quote) {
        private static ChargeResult failed() {
            return new ChargeResult(false, null);
        }
    }
}
