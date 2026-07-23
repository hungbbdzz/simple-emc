package com.velorise.emc.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loader-independent registry-ID rules for modded active-melee compatibility.
 *
 * <p>Classification remains loader-local because it also checks Minecraft item classes and tags.
 * This profile provides the synchronized server configuration and per-item balance overrides.</p>
 */
public final class ImbuedMeleeProfile {
    private static volatile Snapshot active = Snapshot.from(GameplayConfig.Values.defaults());

    private ImbuedMeleeProfile() {}

    static void install(GameplayConfig.Values values) {
        active = Snapshot.from(values);
    }

    public static boolean forceRepairOnly(String registryId) {
        return active.forceRepairOnly.contains(registryId);
    }

    /** An override entry is also an explicit melee inclusion. */
    public static boolean configuredMelee(String registryId) {
        return active.forceInclude.contains(registryId) || active.itemOverrides.containsKey(registryId);
    }

    public static float bonusDamage(String registryId, int mode) {
        int index = index(mode);
        OverrideValues override = active.itemOverrides.get(registryId);
        if (override != null && override.bonusDamage != null) return override.bonusDamage[index];
        return active.globalBonusDamage[index];
    }

    public static long activeCostPerSecond(String registryId, int mode) {
        int index = index(mode);
        OverrideValues override = active.itemOverrides.get(registryId);
        if (override != null && override.activeCostPerSecond != null) return override.activeCostPerSecond[index];
        return active.globalActiveCostPerSecond[index];
    }

    private static int index(int mode) {
        return Math.max(0, Math.min(3, mode - 1));
    }

    private record OverrideValues(float[] bonusDamage, long[] activeCostPerSecond) {
        private static OverrideValues from(GameplayConfig.MeleeItemOverride source) {
            if (source == null) return new OverrideValues(null, null);
            return new OverrideValues(
                source.bonusDamage == null ? null : source.bonusDamage.clone(),
                source.activeCostPerSecond == null ? null : source.activeCostPerSecond.clone()
            );
        }
    }

    private record Snapshot(
        Set<String> forceInclude,
        Set<String> forceRepairOnly,
        Map<String, OverrideValues> itemOverrides,
        float[] globalBonusDamage,
        long[] globalActiveCostPerSecond
    ) {
        private static Snapshot from(GameplayConfig.Values values) {
            GameplayConfig.ImbuedMeleeCompatibility compatibility = values.items.imbuedMeleeCompatibility;
            GameplayConfig.CustomUtilities global = values.items.customUtilities;

            Set<String> include = new LinkedHashSet<>();
            Collections.addAll(include, compatibility.forceInclude);
            Set<String> repairOnly = new LinkedHashSet<>();
            Collections.addAll(repairOnly, compatibility.forceRepairOnly);

            Map<String, OverrideValues> overrides = new LinkedHashMap<>();
            for (Map.Entry<String, GameplayConfig.MeleeItemOverride> entry : compatibility.itemOverrides.entrySet()) {
                overrides.put(entry.getKey(), OverrideValues.from(entry.getValue()));
            }

            return new Snapshot(
                Collections.unmodifiableSet(include),
                Collections.unmodifiableSet(repairOnly),
                Collections.unmodifiableMap(overrides),
                global.activeMeleeBonusDamage.clone(),
                global.activeMeleeCostPerSecond.clone()
            );
        }
    }
}
