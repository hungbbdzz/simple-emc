package com.velorise.emc.common;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Server-authoritative gameplay balance snapshot.
 *
 * <p>The loader modules deserialize {@code config/velorise_emc/gameplay.json}, sanitize it,
 * install it here, and synchronize the normalized snapshot to clients. Structural values
 * such as registry IDs, menu layouts and inventory slot counts intentionally remain code-defined.</p>
 */
public final class GameplayConfig {
    public static final int CURRENT_VERSION = 24;
    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final int MAX_COMPATIBILITY_ENTRIES = 512;
    private static volatile Values active = Values.defaults();

    private GameplayConfig() {}

    public static Values current() {
        return active;
    }

    public static void install(Values values) {
        Values normalized = sanitize(values);
        active = normalized;
        ImbuedMeleeProfile.install(normalized);
    }

    public static void resetToDefaults() {
        install(Values.defaults());
    }

    public static Values sanitize(Values source) {
        Values defaults = Values.defaults();
        Values out = source == null ? defaults : source;
        out.configVersion = CURRENT_VERSION;

        if (out.economy == null) out.economy = defaults.economy;
        if (out.machines == null) out.machines = defaults.machines;
        if (out.items == null) out.items = defaults.items;
        if (out.progression == null) out.progression = defaults.progression;
        if (out.world == null) out.world = defaults.world;

        sanitizeEconomy(out.economy, defaults.economy);
        sanitizeMachines(out.machines, defaults.machines);
        sanitizeItems(out.items, defaults.items);
        sanitizeProgression(out.progression, defaults.progression);
        sanitizeWorld(out.world, defaults.world);
        return out;
    }


    private static void sanitizeEconomy(Economy value, Economy defaults) {
        if (value.transmutationTax == null) value.transmutationTax = defaults.transmutationTax;

        TransmutationTax tax = value.transmutationTax;
        tax.recoveryPointsPerSecond = clamp(tax.recoveryPointsPerSecond, 1L, 1_000_000L);
        tax.pointThresholds = increasingLongArray(
            tax.pointThresholds, defaults.transmutationTax.pointThresholds, 1L, 1_000_000L, 5);
        tax.extraEmcPerItem = longArray(
            tax.extraEmcPerItem, defaults.transmutationTax.extraEmcPerItem, 0L, 1_000_000_000L, 6);
    }

    private static void sanitizeMachines(Machines value, Machines defaults) {
        if (value.hourglass == null) value.hourglass = defaults.hourglass;
        if (value.resonantFurnace == null) value.resonantFurnace = defaults.resonantFurnace;
        if (value.tidalConduit == null) value.tidalConduit = defaults.tidalConduit;
        if (value.matterburst == null) value.matterburst = defaults.matterburst;
        if (value.resonanceJukebox == null) value.resonanceJukebox = defaults.resonanceJukebox;
        if (value.purificationGrindstone == null) value.purificationGrindstone = defaults.purificationGrindstone;

        Hourglass h = value.hourglass;
        h.maxEmc = clamp(h.maxEmc, 1L, Long.MAX_VALUE);
        h.pulseIntervalTicks = clamp(h.pulseIntervalTicks, 1, 1200);
        h.bonusTicksPerPulse = clamp(h.bonusTicksPerPulse, 1, 200);
        h.emcPerSuccessfulTick = clamp(h.emcPerSuccessfulTick, 0L, 1_000_000_000L);
        h.radius = hourglassRadiusArray(h.radius, defaults.hourglass.radius);

        ResonantFurnace furnace = value.resonantFurnace;
        furnace.speedMultiplier = intArray(furnace.speedMultiplier, defaults.resonantFurnace.speedMultiplier, 1, 64);

        sanitizeAura(value.tidalConduit, defaults.tidalConduit);

        Matterburst matterburst = value.matterburst;
        matterburst.radius = intArray(matterburst.radius, defaults.matterburst.radius, 1, 128);
        matterburst.depthMultiplier = clamp(matterburst.depthMultiplier, 0.05F, 4.0F);
        matterburst.minimumDepth = clamp(matterburst.minimumDepth, 1, 128);
        matterburst.entityDamageRadiusMultiplier = clamp(matterburst.entityDamageRadiusMultiplier, 0.0F, 8.0F);
        matterburst.baseDamage = clamp(matterburst.baseDamage, 0.0F, 10_000.0F);
        matterburst.damagePerRadius = clamp(matterburst.damagePerRadius, 0.0F, 1_000.0F);
        matterburst.maxBlocksPerTick = clamp(matterburst.maxBlocksPerTick, 1, 16_384);

        ResonanceJukebox jukebox = value.resonanceJukebox;
        jukebox.maxEmc = clamp(jukebox.maxEmc, 1L, Long.MAX_VALUE);
        jukebox.emcCost = clamp(jukebox.emcCost, 0, 1_000_000_000);
        jukebox.costIntervalTicks = clamp(jukebox.costIntervalTicks, 1, 72_000);

        Purification purification = value.purificationGrindstone;
        purification.operationCost = clamp(purification.operationCost, 0L, 1_000_000_000L);
    }

    private static void sanitizeAura(AuraMachine value, AuraMachine defaults) {
        value.maxEmc = clamp(value.maxEmc, 1L, Long.MAX_VALUE);
        value.range = intArray(value.range, defaults.range, 1, 256);
        value.emcPerSecond = clamp(value.emcPerSecond, 0L, 1_000_000_000L);
        value.pulseIntervalTicks = clamp(value.pulseIntervalTicks, 20, 72_000);
        value.effectDurationTicks = clamp(value.effectDurationTicks, value.pulseIntervalTicks, 72_000);
        value.fuelIntervalTicks = clamp(value.fuelIntervalTicks, 1, 1200);
    }

    private static void sanitizeItems(Items value, Items defaults) {
        if (value.general == null) value.general = defaults.general;
        if (value.fishingRod == null) value.fishingRod = defaults.fishingRod;
        if (value.cursedIgniter == null) value.cursedIgniter = defaults.cursedIgniter;
        if (value.imbuedMiningTools == null) value.imbuedMiningTools = defaults.imbuedMiningTools;
        if (value.aethericElytra == null) value.aethericElytra = defaults.aethericElytra;
        if (value.fixedUtilities == null) value.fixedUtilities = defaults.fixedUtilities;
        if (value.customUtilities == null) value.customUtilities = defaults.customUtilities;
        if (value.imbuedMeleeCompatibility == null) value.imbuedMeleeCompatibility = defaults.imbuedMeleeCompatibility;
        if (value.purifyingMilk == null) value.purifyingMilk = defaults.purifyingMilk;
        if (value.coreArmor == null) value.coreArmor = defaults.coreArmor;
        if (value.arcaneWeapons == null) value.arcaneWeapons = defaults.arcaneWeapons;

        GeneralItems general = value.general;
        general.boundCompassLockCost = clamp(general.boundCompassLockCost, 0L, 1_000_000_000L);
        general.recoveryChannelTicks = clamp(general.recoveryChannelTicks, 1, 72_000);
        general.recoveryCooldownTicks = clamp(general.recoveryCooldownTicks, 0, 720_000);
        general.recoveryMovementTolerance = clamp(general.recoveryMovementTolerance, 0.0D, 16.0D);
        general.recoveryHorizontalSearchRadius = clamp(general.recoveryHorizontalSearchRadius, 0, 32);
        general.recoveryVerticalSearchRadius = clamp(general.recoveryVerticalSearchRadius, 0, 64);

        FishingRod fishing = value.fishingRod;
        fishing.castEmcCost = clamp(fishing.castEmcCost, 0L, 1_000_000_000L);
        fishing.catches = intArray(fishing.catches, defaults.fishingRod.catches, 1, 64);

        CursedIgniter igniter = value.cursedIgniter;
        igniter.cooldownTicks = clamp(igniter.cooldownTicks, 0, 720_000);
        // Cursed Igniter cost is mode-specific; Core only limits the maximum selectable mode.
        igniter.emcCost = longArray(igniter.emcCost, defaults.cursedIgniter.emcCost, 0L, 1_000_000_000L);
        igniter.damage = floatArray(igniter.damage, defaults.cursedIgniter.damage, 0.0F, 10_000.0F);
        igniter.fireSeconds = intArray(igniter.fireSeconds, defaults.cursedIgniter.fireSeconds, 0, 3600);

        ImbuedMiningTools mining = value.imbuedMiningTools;
        mining.hoeHarvestDiameter = oddIntArray(mining.hoeHarvestDiameter, defaults.imbuedMiningTools.hoeHarvestDiameter, 1, 31);

        AethericElytra elytra = value.aethericElytra;
        elytra.boostImpulse = doubleArray(elytra.boostImpulse, defaults.aethericElytra.boostImpulse, 0.0D, 20.0D);
        elytra.boostCooldownTicks = clamp(elytra.boostCooldownTicks, 0, 720_000);

        FixedUtilities fixed = value.fixedUtilities;
        fixed.farSightFovMultiplier = clamp(fixed.farSightFovMultiplier, 0.01D, 1.0D);
        fixed.aquaticActivationCost = clamp(fixed.aquaticActivationCost, 0L, 1_000_000_000L);
        fixed.aquaticWaterBreathingTicks = clamp(fixed.aquaticWaterBreathingTicks, 1, 72_000);
        fixed.aquaticDolphinsGraceTicks = clamp(fixed.aquaticDolphinsGraceTicks, 1, 72_000);


        CustomUtilities custom = value.customUtilities;
        custom.alchemicalAnvilEmcPerMissingLevel = clamp(custom.alchemicalAnvilEmcPerMissingLevel, 0L, 1_000_000_000L);
        custom.activeMeleeCostPerSecond = longArray(custom.activeMeleeCostPerSecond, defaults.customUtilities.activeMeleeCostPerSecond, 0L, 1_000_000_000L);
        custom.activeGenericCostPerSecond = longArray(custom.activeGenericCostPerSecond, defaults.customUtilities.activeGenericCostPerSecond, 0L, 1_000_000_000L);
        custom.activeMeleeBonusDamage = floatArray(custom.activeMeleeBonusDamage, defaults.customUtilities.activeMeleeBonusDamage, 0.0F, 10_000.0F);

        PurifyingMilk purifyingMilk = value.purifyingMilk;
        purifyingMilk.emcCost = flatLongArray(purifyingMilk.emcCost,
            defaults.purifyingMilk.emcCost, 0L, 1_000_000_000L);

        CoreArmor coreArmor = value.coreArmor;
        coreArmor.repairEmcPerDurability = longArray(coreArmor.repairEmcPerDurability, defaults.coreArmor.repairEmcPerDurability, 0L, 1_000_000_000L);
        coreArmor.weaponDamageBonus = floatArray(coreArmor.weaponDamageBonus, defaults.coreArmor.weaponDamageBonus, 0.0F, 100.0F);
        coreArmor.armorBonus = doubleArray(coreArmor.armorBonus, defaults.coreArmor.armorBonus, 0.0D, 100.0D);
        coreArmor.elytraArmorBonus = doubleArray(coreArmor.elytraArmorBonus, defaults.coreArmor.elytraArmorBonus, 0.0D, 100.0D);

        ArcaneWeapons arcane = value.arcaneWeapons;
        arcane.cooldownTicks = clamp(arcane.cooldownTicks, 0, 720_000);
        arcane.infernoCooldownTicks = clamp(arcane.infernoCooldownTicks, 0, 720_000);
        arcane.ghastfireCooldownTicks = clamp(arcane.ghastfireCooldownTicks, 0, 720_000);
        arcane.witherCooldownTicks = clamp(arcane.witherCooldownTicks, 0, 720_000);
        arcane.shulkerCooldownTicks = clamp(arcane.shulkerCooldownTicks, 0, 720_000);
        arcane.guardianCooldownTicks = clamp(arcane.guardianCooldownTicks, 0, 720_000);
        arcane.evokerCooldownTicks = clamp(arcane.evokerCooldownTicks, 0, 720_000);
        arcane.lightningCooldownTicks = clamp(arcane.lightningCooldownTicks, 0, 720_000);
        arcane.frostCooldownTicks = clamp(arcane.frostCooldownTicks, 0, 720_000);
        arcane.webCooldownTicks = clamp(arcane.webCooldownTicks, 0, 720_000);
        arcane.echoChargeTicks = clamp(arcane.echoChargeTicks, 1, 1_200);
        arcane.infernoBurstShots = clamp(arcane.infernoBurstShots, 1, 32);
        arcane.infernoBurstResetTicks = clamp(arcane.infernoBurstResetTicks, 1, 72_000);
        arcane.witherBurstShots = clamp(arcane.witherBurstShots, 1, 32);
        arcane.witherBurstResetTicks = clamp(arcane.witherBurstResetTicks, 1, 72_000);
        arcane.lightningBurstResetTicks = clamp(arcane.lightningBurstResetTicks, 1, 72_000);
        arcane.frostBurstResetTicks = clamp(arcane.frostBurstResetTicks, 1, 72_000);
        arcane.webBurstShots = clamp(arcane.webBurstShots, 1, 32);
        arcane.webBurstResetTicks = clamp(arcane.webBurstResetTicks, 1, 72_000);
        arcane.infernoEmcCost = longArray(arcane.infernoEmcCost, defaults.arcaneWeapons.infernoEmcCost, 0L, 1_000_000_000L);
        arcane.ghastfireEmcCost = longArray(arcane.ghastfireEmcCost, defaults.arcaneWeapons.ghastfireEmcCost, 0L, 1_000_000_000L);
        arcane.dragonfireEmcCost = longArray(arcane.dragonfireEmcCost, defaults.arcaneWeapons.dragonfireEmcCost, 0L, 1_000_000_000L);
        arcane.echoEmcCost = longArray(arcane.echoEmcCost, defaults.arcaneWeapons.echoEmcCost, 0L, 1_000_000_000L);
        arcane.witherEmcCost = longArray(arcane.witherEmcCost, defaults.arcaneWeapons.witherEmcCost, 0L, 1_000_000_000L);
        arcane.shulkerEmcCost = longArray(arcane.shulkerEmcCost, defaults.arcaneWeapons.shulkerEmcCost, 0L, 1_000_000_000L);
        arcane.guardianEmcPerSecond = longArray(arcane.guardianEmcPerSecond, defaults.arcaneWeapons.guardianEmcPerSecond, 0L, 1_000_000_000L);
        arcane.guardianBeamDurationTicks = intArray(arcane.guardianBeamDurationTicks, defaults.arcaneWeapons.guardianBeamDurationTicks, 1, 72_000);
        arcane.evokerEmcCost = longArray(arcane.evokerEmcCost, defaults.arcaneWeapons.evokerEmcCost, 0L, 1_000_000_000L);
        arcane.lightningEmcCost = longArray(arcane.lightningEmcCost, defaults.arcaneWeapons.lightningEmcCost, 0L, 1_000_000_000L);
        arcane.frostEmcCost = longArray(arcane.frostEmcCost, defaults.arcaneWeapons.frostEmcCost, 0L, 1_000_000_000L);
        arcane.webEmcCost = longArray(arcane.webEmcCost, defaults.arcaneWeapons.webEmcCost, 0L, 1_000_000_000L);
        arcane.dragonfireDamage = floatArray(arcane.dragonfireDamage, defaults.arcaneWeapons.dragonfireDamage, 0.0F, 10_000.0F);
        arcane.echoDamage = floatArray(arcane.echoDamage, defaults.arcaneWeapons.echoDamage, 0.0F, 10_000.0F);
        arcane.infernoDamage = floatArray(arcane.infernoDamage, defaults.arcaneWeapons.infernoDamage, 0.0F, 10_000.0F);
        arcane.ghastfireDamage = floatArray(arcane.ghastfireDamage, defaults.arcaneWeapons.ghastfireDamage, 0.0F, 10_000.0F);
        arcane.witherDamage = floatArray(arcane.witherDamage, defaults.arcaneWeapons.witherDamage, 0.0F, 10_000.0F);
        arcane.shulkerDamage = floatArray(arcane.shulkerDamage, defaults.arcaneWeapons.shulkerDamage, 0.0F, 10_000.0F);
        arcane.guardianDamage = floatArray(arcane.guardianDamage, defaults.arcaneWeapons.guardianDamage, 0.0F, 10_000.0F);
        arcane.lightningDamage = floatArray(arcane.lightningDamage, defaults.arcaneWeapons.lightningDamage, 0.0F, 10_000.0F);
        arcane.frostDamage = floatArray(arcane.frostDamage, defaults.arcaneWeapons.frostDamage, 0.0F, 10_000.0F);
        arcane.dragonfireRadius = doubleArray(arcane.dragonfireRadius, defaults.arcaneWeapons.dragonfireRadius, 0.5D, 64.0D);
        arcane.dragonfireRange = doubleArray(arcane.dragonfireRange, defaults.arcaneWeapons.dragonfireRange, 1.0D, 256.0D);
        arcane.infernoRange = doubleArray(arcane.infernoRange, defaults.arcaneWeapons.infernoRange, 1.0D, 256.0D);
        arcane.ghastfireRange = doubleArray(arcane.ghastfireRange, defaults.arcaneWeapons.ghastfireRange, 1.0D, 256.0D);
        arcane.ghastfireRadius = doubleArray(arcane.ghastfireRadius, defaults.arcaneWeapons.ghastfireRadius, 0.5D, 32.0D);
        arcane.witherRange = doubleArray(arcane.witherRange, defaults.arcaneWeapons.witherRange, 1.0D, 256.0D);
        arcane.shulkerRange = doubleArray(arcane.shulkerRange, defaults.arcaneWeapons.shulkerRange, 1.0D, 256.0D);
        arcane.guardianRange = doubleArray(arcane.guardianRange, defaults.arcaneWeapons.guardianRange, 1.0D, 256.0D);
        arcane.evokerRange = doubleArray(arcane.evokerRange, defaults.arcaneWeapons.evokerRange, 1.0D, 256.0D);
        arcane.lightningRange = doubleArray(arcane.lightningRange, defaults.arcaneWeapons.lightningRange, 1.0D, 256.0D);
        arcane.frostRange = doubleArray(arcane.frostRange, defaults.arcaneWeapons.frostRange, 1.0D, 256.0D);
        arcane.webRange = doubleArray(arcane.webRange, defaults.arcaneWeapons.webRange, 1.0D, 256.0D);
        arcane.witherRadius = doubleArray(arcane.witherRadius, defaults.arcaneWeapons.witherRadius, 0.5D, 32.0D);
        arcane.lightningRadius = doubleArray(arcane.lightningRadius, defaults.arcaneWeapons.lightningRadius, 0.5D, 32.0D);
        arcane.witherSeconds = intArray(arcane.witherSeconds, defaults.arcaneWeapons.witherSeconds, 0, 3600);
        arcane.shulkerLevitationSeconds = intArray(arcane.shulkerLevitationSeconds, defaults.arcaneWeapons.shulkerLevitationSeconds, 0, 3600);
        arcane.frostSlowSeconds = intArray(arcane.frostSlowSeconds, defaults.arcaneWeapons.frostSlowSeconds, 0, 3600);
        arcane.webDurationTicks = intArray(arcane.webDurationTicks, defaults.arcaneWeapons.webDurationTicks, 1, 72_000);
        arcane.webVenomDurationTicks = intArray(arcane.webVenomDurationTicks, defaults.arcaneWeapons.webVenomDurationTicks, 0, 72_000);
        arcane.webPoisonAmplifier = intArray(arcane.webPoisonAmplifier, defaults.arcaneWeapons.webPoisonAmplifier, 0, 4);
        arcane.evokerFangCount = intArray(arcane.evokerFangCount, defaults.arcaneWeapons.evokerFangCount, 1, 64);
        arcane.dragonfireCloudDurationTicks = clamp(arcane.dragonfireCloudDurationTicks, 1, 72_000);
        arcane.dragonfirePulseIntervalTicks = clamp(arcane.dragonfirePulseIntervalTicks, 1, 1_200);
        arcane.dragonfirePulseDamageMultiplier = clamp(arcane.dragonfirePulseDamageMultiplier, 0.0F, 10.0F);
        arcane.echoRange = clamp(arcane.echoRange, 1.0D, 256.0D);
        arcane.echoBeamRadius = clamp(arcane.echoBeamRadius, 0.1D, 8.0D);
        arcane.infernoFireSeconds = clamp(arcane.infernoFireSeconds, 0, 3600);
        arcane.ghastfireFireSeconds = clamp(arcane.ghastfireFireSeconds, 0, 3600);

        sanitizeImbuedMeleeCompatibility(value.imbuedMeleeCompatibility, custom);
        if (custom.attractionCatalyst == null) custom.attractionCatalyst = defaults.customUtilities.attractionCatalyst;
        if (custom.chalice == null) custom.chalice = defaults.customUtilities.chalice;
        if (custom.voidTranslocator == null) custom.voidTranslocator = defaults.customUtilities.voidTranslocator;
        if (custom.teleport == null) custom.teleport = defaults.customUtilities.teleport;
        sanitizeAttraction(custom.attractionCatalyst);
        sanitizeChalice(custom.chalice);
        sanitizeTranslocator(custom.voidTranslocator);
        sanitizeTeleport(custom.teleport);
    }

    private static void sanitizeImbuedMeleeCompatibility(ImbuedMeleeCompatibility value, CustomUtilities global) {
        value.forceInclude = resourceIdArray(value.forceInclude);
        value.forceRepairOnly = resourceIdArray(value.forceRepairOnly);

        Map<String, MeleeItemOverride> normalized = new LinkedHashMap<>();
        if (value.itemOverrides != null) {
            for (Map.Entry<String, MeleeItemOverride> entry : value.itemOverrides.entrySet()) {
                if (normalized.size() >= MAX_COMPATIBILITY_ENTRIES) break;
                String id = normalizeResourceId(entry.getKey());
                if (id == null || normalized.containsKey(id)) continue;

                MeleeItemOverride source = entry.getValue() == null ? new MeleeItemOverride() : entry.getValue();
                MeleeItemOverride out = new MeleeItemOverride();
                out.bonusDamage = optionalFloatArray(source.bonusDamage, global.activeMeleeBonusDamage, 0.0F, 10_000.0F);
                out.activeCostPerSecond = optionalLongArray(source.activeCostPerSecond, global.activeMeleeCostPerSecond, 0L, 1_000_000_000L);
                normalized.put(id, out);
            }
        }
        value.itemOverrides = normalized;
    }

    private static String[] resourceIdArray(String[] source) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (source != null) {
            for (String raw : source) {
                if (normalized.size() >= MAX_COMPATIBILITY_ENTRIES) break;
                String id = normalizeResourceId(raw);
                if (id != null) normalized.add(id);
            }
        }
        return normalized.toArray(String[]::new);
    }

    private static String normalizeResourceId(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 256 || !RESOURCE_ID_PATTERN.matcher(normalized).matches()) return null;
        return normalized;
    }

    private static float[] optionalFloatArray(float[] source, float[] defaults, float min, float max) {
        return source == null || source.length == 0 ? null : floatArray(source, defaults, min, max);
    }

    private static long[] optionalLongArray(long[] source, long[] defaults, long min, long max) {
        return source == null || source.length == 0 ? null : longArray(source, defaults, min, max);
    }

    private static void sanitizeAttraction(AttractionCatalyst value) {
        value.baseDiameter = clamp(value.baseDiameter, 1, 255);
        value.diameterPerCore = clamp(value.diameterPerCore, 0, 64);
        value.emcCostPerTick = clamp(value.emcCostPerTick, 0L, 1_000_000_000L);
        value.pullStrength = clamp(value.pullStrength, 0.0D, 10.0D);
    }

    private static void sanitizeChalice(Chalice value) {
        value.baseDiameter = clamp(value.baseDiameter, 1, 255);
        value.diameterPerCore = clamp(value.diameterPerCore, 0, 64);
        value.verticalRadius = clamp(value.verticalRadius, 0, 16);
        value.voidBaseCost = clamp(value.voidBaseCost, 0L, 1_000_000_000L);
        value.waterCostPerBlock = clamp(value.waterCostPerBlock, 0L, 1_000_000_000L);
        value.lavaCostPerBlock = clamp(value.lavaCostPerBlock, 0L, 1_000_000_000L);
        value.otherFluidCostPerBlock = clamp(value.otherFluidCostPerBlock, 0L, 1_000_000_000L);
        value.growthCostPerBlock = clamp(value.growthCostPerBlock, 0L, 1_000_000_000L);
    }

    private static void sanitizeTranslocator(VoidTranslocator value) {
        value.range = doubleArray(value.range, new double[]{15.0D, 20.0D, 25.0D, 30.0D}, 1.0D, 4096.0D, 4);
        value.emcCost = clamp(value.emcCost, 0L, 1_000_000_000L);
        value.cooldownTicks = clamp(value.cooldownTicks, 0, 720_000);
    }

    private static void sanitizeTeleport(Teleport value) {
        value.crossDimensionSurcharge = clamp(value.crossDimensionSurcharge, 0L, 1_000_000_000L);
        value.emcPerBlock = clamp(value.emcPerBlock, 0.0D, 1_000_000.0D);
        value.magicMirrorEmcPerBlock = doubleArray(value.magicMirrorEmcPerBlock,
            new double[]{2.5D, 2.0D, 1.5D, 1.0D, 0.5D}, 0.0D, 1_000_000.0D, 5);
        value.magicMirrorUseDurationTicks = clamp(value.magicMirrorUseDurationTicks, 1, 72_000);
        value.magicMirrorCooldownTicks = clamp(value.magicMirrorCooldownTicks, 0, 720_000);
    }


    private static void sanitizeWorld(WorldSystems value, WorldSystems defaults) {
        if (value.auraNodes == null) value.auraNodes = defaults.auraNodes;
        if (value.bossPlayerScaling == null) value.bossPlayerScaling = defaults.bossPlayerScaling;
        if (value.mobThreatScaling == null) value.mobThreatScaling = defaults.mobThreatScaling;

        AuraNodes nodes = value.auraNodes;
        nodes.reserve = longArray(nodes.reserve, defaults.auraNodes.reserve, 1L, Long.MAX_VALUE);
        nodes.outputPerPlayer = longArray(nodes.outputPerPlayer, defaults.auraNodes.outputPerPlayer, 1L, 1_000_000_000L);
        nodes.rechargeTicks = intArray(nodes.rechargeTicks, defaults.auraNodes.rechargeTicks, 20, 72_000_000);
        nodes.spacingBlocks = intArray(nodes.spacingBlocks, defaults.auraNodes.spacingBlocks, 128, 100_000);
        nodes.outputRadius = intArray(nodes.outputRadius, defaults.auraNodes.outputRadius, 1, 128);
        nodes.siphonRadius = intArray(nodes.siphonRadius, defaults.auraNodes.siphonRadius, 1, 128);
        nodes.siphonDamage = floatArray(nodes.siphonDamage, defaults.auraNodes.siphonDamage, 0.0F, 1_000.0F);
        nodes.pulseIntervalTicks = clamp(nodes.pulseIntervalTicks, 1, 1_200);
        nodes.spawnExclusionRadius = clamp(nodes.spawnExclusionRadius, 0, 100_000);
        nodes.ancientCitySpawnChance = clamp(nodes.ancientCitySpawnChance, 0.0D, 1.0D);

        BossPlayerScaling boss = value.bossPlayerScaling;
        boss.maxCountedPlayers = clamp(boss.maxCountedPlayers, 0, 10_000);
        boss.healthPerPlayer = clamp(boss.healthPerPlayer, 0.0D, 100_000.0D);
        boss.damagePerPlayer = clamp(boss.damagePerPlayer, 0.0F, 10_000.0F);

        MobThreatScaling mobs = value.mobThreatScaling;
        mobs.healthPerTier = clamp(mobs.healthPerTier, 0.0D, 10_000.0D);
        mobs.damagePerTier = clamp(mobs.damagePerTier, 0.0F, 10_000.0F);
        mobs.lowHealthCapRatio = clamp(mobs.lowHealthCapRatio, 0.0D, 100.0D);
        mobs.healthMultiplierByTier = doubleArray(mobs.healthMultiplierByTier, defaults.mobThreatScaling.healthMultiplierByTier, 1.0D, 20.0D, 5);
        mobs.damageMultiplierByTier = floatArray(mobs.damageMultiplierByTier, defaults.mobThreatScaling.damageMultiplierByTier, 1.0F, 20.0F, 5);
        mobs.armorBonusByTier = doubleArray(mobs.armorBonusByTier, defaults.mobThreatScaling.armorBonusByTier, 0.0D, 100.0D, 5);
        mobs.knockbackResistanceByTier = doubleArray(mobs.knockbackResistanceByTier, defaults.mobThreatScaling.knockbackResistanceByTier, 0.0D, 1.0D, 5);
        mobs.arcaneMobChanceByTier = doubleArray(mobs.arcaneMobChanceByTier, defaults.mobThreatScaling.arcaneMobChanceByTier, 0.0D, 1.0D, 5);
        mobs.excludedEntities = resourceIdArray(mobs.excludedEntities);
    }
    private static void sanitizeProgression(Progression value, Progression defaults) {
        if (value.worldTiers == null) value.worldTiers = defaults.worldTiers;
        sanitizeWorldTierProgression(value.worldTiers, defaults.worldTiers);
    }

    private static void sanitizeWorldTierProgression(WorldTierProgression value, WorldTierProgression defaults) {
        value.scalingMode = WorldProgressionProfile.ScalingMode.byName(value.scalingMode).name();
        value.perAdditionalPlayerMultiplier = clamp(value.perAdditionalPlayerMultiplier, 1.0D, 10.0D);
        value.linearIncreasePerAdditionalPlayer = clamp(value.linearIncreasePerAdditionalPlayer, 0.0D, 10.0D);
        value.maximumScale = clamp(value.maximumScale, 1.0D, 100.0D);
        value.maximumContributors = clamp(value.maximumContributors, 0, 10_000);
        if (value.overworld == null) value.overworld = defaults.overworld;
        if (value.nether == null) value.nether = defaults.nether;
        if (value.end == null) value.end = defaults.end;
        if (value.deepWorld == null) value.deepWorld = defaults.deepWorld;
        sanitizeOverworldRequirements(value.overworld);
        sanitizeNetherRequirements(value.nether);
        sanitizeEndRequirements(value.end);
        sanitizeDeepWorldRequirements(value.deepWorld);
    }

    private static void sanitizeOverworldRequirements(OverworldTierRequirements value) {
        value.totalKills = clamp(value.totalKills, 0L, 1_000_000_000L);
        value.zombieFamily = clamp(value.zombieFamily, 0L, 1_000_000_000L);
        value.skeletonFamily = clamp(value.skeletonFamily, 0L, 1_000_000_000L);
        value.spiderFamily = clamp(value.spiderFamily, 0L, 1_000_000_000L);
        value.creeper = clamp(value.creeper, 0L, 1_000_000_000L);
        value.enderman = clamp(value.enderman, 0L, 1_000_000_000L);
        value.witch = clamp(value.witch, 0L, 1_000_000_000L);
        value.illager = clamp(value.illager, 0L, 1_000_000_000L);
    }

    private static void sanitizeNetherRequirements(NetherTierRequirements value) {
        value.totalKills = clamp(value.totalKills, 0L, 1_000_000_000L);
        value.piglinFamily = clamp(value.piglinFamily, 0L, 1_000_000_000L);
        value.zombifiedPiglin = clamp(value.zombifiedPiglin, 0L, 1_000_000_000L);
        value.blaze = clamp(value.blaze, 0L, 1_000_000_000L);
        value.magmaCube = clamp(value.magmaCube, 0L, 1_000_000_000L);
        value.ghast = clamp(value.ghast, 0L, 1_000_000_000L);
        value.witherSkeleton = clamp(value.witherSkeleton, 0L, 1_000_000_000L);
        value.wither = clamp(value.wither, 0L, 1_000_000_000L);
    }

    private static void sanitizeEndRequirements(EndTierRequirements value) {
        value.enderman = clamp(value.enderman, 0L, 1_000_000_000L);
        value.shulker = clamp(value.shulker, 0L, 1_000_000_000L);
        value.enderDragon = clamp(value.enderDragon, 0L, 1_000_000_000L);
    }

    private static void sanitizeDeepWorldRequirements(DeepWorldTierRequirements value) {
        value.guardian = clamp(value.guardian, 0L, 1_000_000_000L);
        value.elderGuardian = clamp(value.elderGuardian, 0L, 1_000_000_000L);
        value.warden = clamp(value.warden, 0L, 1_000_000_000L);
    }

    private static int[] oddIntArray(int[] source, int[] defaults, int min, int max) {
        int[] result = intArray(source, defaults, min, max);
        for (int i = 0; i < result.length; i++) {
            if ((result[i] & 1) == 0) result[i] = Math.min(max, result[i] + 1);
        }
        return result;
    }

    private static int[] intArray(int[] source, int[] defaults, int min, int max) {
        return intArray(source, defaults, min, max, 4);
    }

    /** Migrates the former [no Core, I, II, III, IV] table to Core I-IV only. */
    private static int[] hourglassRadiusArray(int[] source, int[] defaults) {
        int[] out = Arrays.copyOf(defaults, 4);
        if (source == null) return out;
        int offset = source.length >= 5 ? 1 : 0;
        for (int i = 0; i < 4 && i + offset < source.length; i++) {
            out[i] = clamp(source[i + offset], 1, 128);
        }
        return out;
    }

    private static int[] intArray(int[] source, int[] defaults, int min, int max, int length) {
        int[] out = Arrays.copyOf(defaults, length);
        if (source != null) {
            for (int i = 0; i < Math.min(length, source.length); i++) out[i] = clamp(source[i], min, max);
        }
        return out;
    }

    private static long[] longArray(long[] source, long[] defaults, long min, long max) {
        return longArray(source, defaults, min, max, 4);
    }

    private static long[] longArray(long[] source, long[] defaults, long min, long max, int length) {
        long[] out = Arrays.copyOf(defaults, length);
        if (source != null) {
            for (int i = 0; i < Math.min(length, source.length); i++) out[i] = clamp(source[i], min, max);
        }
        return out;
    }


    /**
     * Retains the legacy four-entry JSON shape while enforcing one operation cost for every Core tier.
     * The first configured value is authoritative; normalization mirrors it to all entries.
     */
    private static long[] flatLongArray(long[] source, long[] defaults, long min, long max) {
        long fallback = defaults == null || defaults.length == 0 ? min : defaults[0];
        long value = source == null || source.length == 0 ? fallback : source[0];
        value = clamp(value, min, max);
        return new long[]{value, value, value, value};
    }


    private static long[] increasingLongArray(long[] source, long[] defaults, long min, long max, int length) {
        long[] out = longArray(source, defaults, min, max, length);
        for (int i = 0; i < out.length; i++) {
            long lower = i == 0 ? min : out[i - 1] + 1L;
            long upper = max - (out.length - 1L - i);
            out[i] = clamp(out[i], lower, Math.max(lower, upper));
        }
        return out;
    }

    private static float[] floatArray(float[] source, float[] defaults, float min, float max) {
        return floatArray(source, defaults, min, max, 4);
    }

    private static float[] floatArray(float[] source, float[] defaults, float min, float max, int length) {
        float[] out = Arrays.copyOf(defaults, length);
        if (source != null) {
            for (int i = 0; i < Math.min(length, source.length); i++) out[i] = clamp(source[i], min, max);
        }
        return out;
    }

    private static double[] doubleArray(double[] source, double[] defaults, double min, double max) {
        return doubleArray(source, defaults, min, max, 4);
    }

    private static double[] doubleArray(double[] source, double[] defaults, double min, double max, int length) {
        double[] out = Arrays.copyOf(defaults, length);
        if (source != null) {
            for (int i = 0; i < Math.min(length, source.length); i++) out[i] = clamp(source[i], min, max);
        }
        return out;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static long clamp(long value, long min, long max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    public static final class Values {
        public int configVersion = CURRENT_VERSION;
        public Economy economy = new Economy();
        public Machines machines = new Machines();
        public Items items = new Items();
        public Progression progression = new Progression();
        public WorldSystems world = new WorldSystems();

        public static Values defaults() { return new Values(); }
    }

    public static final class Economy {
        public TransmutationTax transmutationTax = new TransmutationTax();
    }

    public static final class TransmutationTax {
        /** Default values profile remains tax-free unless a server explicitly enables it. */
        public boolean enabledInDefault = false;
        public boolean enabledInBalanced = true;
        /** Meter points recovered every real-time second, including while materializing. */
        public long recoveryPointsPerSecond = 1L;
        /** Inclusive upper point totals for each tier; the next tier starts after each value. */
        public long[] pointThresholds = {64L, 128L, 256L, 512L, 1_024L};
        /** Flat EMC surcharge per item for the six resulting point tiers. */
        public long[] extraEmcPerItem = {0L, 2L, 4L, 8L, 32L, 64L};
    }

    public static final class Machines {
        public Hourglass hourglass = new Hourglass();
        public ResonantFurnace resonantFurnace = new ResonantFurnace();
        public AuraMachine tidalConduit = new AuraMachine(2_000_000L, new int[]{32, 64, 96, 128}, 64L);
        public Matterburst matterburst = new Matterburst();
        public ResonanceJukebox resonanceJukebox = new ResonanceJukebox();
        public Purification purificationGrindstone = new Purification();
    }

    public static final class Hourglass {
        /** Local buffer; deliberately finite so upgrades remain meaningful. */
        public long maxEmc = 2_000_000L;
        /** The Hourglass batches work once per second instead of polling every tick. */
        public int pulseIntervalTicks = 20;
        /** Fixed acceleration for every tier: +20 virtual machine ticks per pulse. */
        public int bonusTicksPerPulse = 20;
        /** Charged only for virtual ticks that actually progress a target machine. */
        public long emcPerSuccessfulTick = 2L;
        /** Radius for Core I-IV. The Hourglass has no Core-0 variant. */
        public int[] radius = {4, 6, 8, 10};
    }

    public static final class ResonantFurnace {
        /** Cooking-progress multiplier for Core I-IV. */
        public int[] speedMultiplier = {2, 4, 8, 16};
    }

    public static final class AuraMachine {
        public long maxEmc;
        public int[] range;
        /** Fixed unit price; Core changes range and unlocked behavior only. */
        public long emcPerSecond;
        public int pulseIntervalTicks = 200;
        public int effectDurationTicks = 220;
        public int fuelIntervalTicks = 20;

        public AuraMachine() { this(2_000_000L, new int[]{8, 16, 24, 32}, 16L); }
        public AuraMachine(long maxEmc, int[] range, long emcPerSecond) {
            this.maxEmc = maxEmc;
            this.range = range;
            this.emcPerSecond = emcPerSecond;
        }
    }

    public static final class Matterburst {
        public int[] radius = {4, 8, 16, 32};
        public float depthMultiplier = 0.625F;
        public int minimumDepth = 3;
        public float entityDamageRadiusMultiplier = 1.5F;
        public float baseDamage = 24.0F;
        public float damagePerRadius = 4.0F;
        public int maxBlocksPerTick = 8_192;
    }

    public static final class ResonanceJukebox {
        public long maxEmc = 250_000L;
        public int emcCost = 1;
        /** One EMC is consumed every second while a record is playing. */
        public int costIntervalTicks = 20;
    }

    public static final class Purification {
        /** Fixed price per successful operation for the supported Core I-III tiers. */
        public long operationCost = 8_096L;
    }

    public static final class Items {
        public GeneralItems general = new GeneralItems();
        public FishingRod fishingRod = new FishingRod();
        public CursedIgniter cursedIgniter = new CursedIgniter();
        public ImbuedMiningTools imbuedMiningTools = new ImbuedMiningTools();
        public AethericElytra aethericElytra = new AethericElytra();
        public FixedUtilities fixedUtilities = new FixedUtilities();
        public CustomUtilities customUtilities = new CustomUtilities();
        public ImbuedMeleeCompatibility imbuedMeleeCompatibility = new ImbuedMeleeCompatibility();
        public PurifyingMilk purifyingMilk = new PurifyingMilk();
        public CoreArmor coreArmor = new CoreArmor();
        public ArcaneWeapons arcaneWeapons = new ArcaneWeapons();
    }

    public static final class PurifyingMilk {
        /** One-time EMC cost; Core increases effect eligibility but does not increase consumption. */
        public long[] emcCost = {4_096L, 4_096L, 4_096L, 4_096L};
    }

    public static final class CoreArmor {
        /** Fixed repair price for Core I-IV: 64, 32, 16 and 8 EMC per durability by default. */
        public long[] repairEmcPerDurability = {64L, 32L, 16L, 8L};
        /** Passive flat damage added whenever a Core-bound item deals damage. */
        public float[] weaponDamageBonus = {1.0F, 2.0F, 4.0F, 8.0F};
        /** Flat armor points contributed by each Core-bound armor piece. */
        public double[] armorBonus = {1.0D, 2.0D, 4.0D, 8.0D};
        /** Elytra follows the same armor progression as other wearable equipment. */
        public double[] elytraArmorBonus = {1.0D, 2.0D, 4.0D, 8.0D};
    }

    public static final class ArcaneWeapons {
        /** Shared six-second cooldown for Dragonfire Scepter and Resonant Echo. */
        public int cooldownTicks = 120;
        /** Three quick Inferno shots, then three seconds of recovery. */
        public int infernoCooldownTicks = 60;
        public int ghastfireCooldownTicks = 100;
        public int witherCooldownTicks = 100;
        public int shulkerCooldownTicks = 80;
        public int guardianCooldownTicks = 60;
        public int evokerCooldownTicks = 100;
        public int lightningCooldownTicks = 160;
        public int frostCooldownTicks = 30;
        public int webCooldownTicks = 60;

        public int echoChargeTicks = 20;
        public int infernoBurstShots = 3;
        public int infernoBurstResetTicks = 40;
        /** Wither Scepter permits two activations before its normal recovery cooldown. */
        public int witherBurstShots = 2;
        public int witherBurstResetTicks = 40;
        /** Higher Stormcaller tiers gain additional rapid activations before recovery. */
        public int lightningBurstResetTicks = 40;
        /** Core I-IV Frostbolt permits 1/2/3/4 rapid shots before recovery. */
        public int frostBurstResetTicks = 40;
        /** Three Webcaster shots, then its normal recovery cooldown. */
        public int webBurstShots = 3;
        public int webBurstResetTicks = 40;

        public long[] infernoEmcCost = {128L, 256L, 512L, 1_024L};
        public long[] ghastfireEmcCost = {256L, 512L, 1_024L, 2_048L};
        public long[] dragonfireEmcCost = {256L, 512L, 1_024L, 2_048L};
        public long[] echoEmcCost = {512L, 1_024L, 2_048L, 4_096L};
        public long[] witherEmcCost = {384L, 768L, 1_536L, 3_072L};
        /** Shulker keeps a gentler curve even as Core adds bolts and levitation duration. */
        public long[] shulkerEmcCost = {256L, 384L, 512L, 768L};
        public long[] guardianEmcPerSecond = {24L, 48L, 96L, 192L};
        /** Core I-IV Guardian channel limits: 6, 8, 10 and 12 seconds. */
        public int[] guardianBeamDurationTicks = {120, 160, 200, 240};
        public long[] evokerEmcCost = {384L, 768L, 1_536L, 3_072L};
        public long[] lightningEmcCost = {1_024L, 2_048L, 4_096L, 8_192L};
        public long[] frostEmcCost = {64L, 128L, 256L, 512L};
        public long[] webEmcCost = {64L, 128L, 256L, 512L};

        public float[] dragonfireDamage = {8.0F, 12.0F, 16.0F, 20.0F};
        public float[] echoDamage = {10.0F, 15.0F, 20.0F, 25.0F};
        /** Per projectile; a complete burst deals 9/12/15/18 before burning. */
        public float[] infernoDamage = {3.0F, 4.0F, 5.0F, 6.0F};
        /** Restrained +2 damage per Core tier while blast radius grows strongly. */
        public float[] ghastfireDamage = {6.0F, 8.0F, 10.0F, 12.0F};
        public float[] witherDamage = {7.0F, 10.0F, 13.0F, 16.0F};
        /** Shulker scales control duration rather than direct damage. */
        public float[] shulkerDamage = {4.0F, 4.0F, 4.0F, 4.0F};
        public float[] guardianDamage = {6.0F, 9.0F, 12.0F, 15.0F};
        public float[] lightningDamage = {8.0F, 11.0F, 14.0F, 17.0F};
        public float[] frostDamage = {3.0F, 5.0F, 7.0F, 9.0F};

        public double echoRange = 16.0D;
        public double echoBeamRadius = 0.85D;
        public double[] dragonfireRadius = {2.5D, 3.5D, 4.5D, 6.0D};
        public double[] dragonfireRange = {15.0D, 20.0D, 25.0D, 30.0D};
        public double[] infernoRange = {10.0D, 15.0D, 20.0D, 25.0D};
        public double[] ghastfireRange = {10.0D, 15.0D, 20.0D, 25.0D};
        public double[] ghastfireRadius = {3.0D, 4.0D, 5.0D, 6.0D};
        public double[] witherRange = {15.0D, 20.0D, 25.0D, 30.0D};
        public double[] shulkerRange = {14.0D, 18.0D, 22.0D, 26.0D};
        public double[] guardianRange = {14.0D, 18.0D, 22.0D, 26.0D};
        public double[] evokerRange = {8.0D, 11.0D, 14.0D, 17.0D};
        public double[] lightningRange = {16.0D, 20.0D, 24.0D, 28.0D};
        public double[] frostRange = {18.0D, 22.0D, 26.0D, 30.0D};
        public double[] webRange = {16.0D, 20.0D, 24.0D, 28.0D};
        public double[] witherRadius = {1.5D, 1.75D, 2.0D, 2.25D};
        public double[] lightningRadius = {1.5D, 2.0D, 2.5D, 3.0D};

        public int dragonfireCloudDurationTicks = 100;
        public int dragonfirePulseIntervalTicks = 10;
        public float dragonfirePulseDamageMultiplier = 0.12F;
        public int infernoFireSeconds = 5;
        public int ghastfireFireSeconds = 6;
        public int[] witherSeconds = {4, 5, 6, 7};
        public int[] shulkerLevitationSeconds = {3, 4, 5, 6};
        public int[] frostSlowSeconds = {3, 4, 5, 6};
        /** Web terrain remains for 15/30/45/60 seconds. */
        public int[] webDurationTicks = {300, 600, 900, 1_200};
        /** Venom is intentionally shorter-lived than the web: 6/9/12/15 seconds. */
        public int[] webVenomDurationTicks = {120, 180, 240, 300};
        /** Vanilla amplifier values: 0 = Poison I, 1 = Poison II. */
        public int[] webPoisonAmplifier = {0, 0, 0, 1};
        public int[] evokerFangCount = {5, 5, 5, 5};

    }

    public static final class ImbuedMeleeCompatibility {
        /** Registry IDs that must use active-melee behavior even when the item has a custom class. */
        public String[] forceInclude = {};
        /** Registry IDs that remain eligible for Imbuing but receive repair-only behavior. */
        public String[] forceRepairOnly = {};
        /** Per-item melee balance. The presence of an entry also force-includes that item as melee. */
        public Map<String, MeleeItemOverride> itemOverrides = new LinkedHashMap<>();
    }

    public static final class MeleeItemOverride {
        /** Optional four-mode flat damage bonus; null uses the global active-melee values. */
        public float[] bonusDamage;
        /** Optional four-mode EMC/s drain; null uses the global active-melee values. */
        public long[] activeCostPerSecond;
    }

    public static final class GeneralItems {
        public long boundCompassLockCost = 256L;
        public int recoveryChannelTicks = 60;
        public int recoveryCooldownTicks = 300;
        public double recoveryMovementTolerance = 0.5D;
        public int recoveryHorizontalSearchRadius = 2;
        public int recoveryVerticalSearchRadius = 5;
    }

    public static final class FishingRod {
        /** Fixed EMC charged only after a successful catch; Core increases output, not price. */
        public long castEmcCost = 256L;
        public int[] catches = {2, 4, 8, 16};
    }

    public static final class CursedIgniter {
        public int cooldownTicks = 120;
        /** Fixed activation price; Core only increases damage and burn duration. */
        public long[] emcCost = {1_024L, 1_024L, 1_024L, 1_024L};
        public float[] damage = {4, 8, 12, 16};
        public int[] fireSeconds = {5, 8, 12, 16};
    }

    public static final class ImbuedMiningTools {
        /** Odd horizontal harvest diameter for Core-bound hoes. */
        public int[] hoeHarvestDiameter = {3, 5, 7, 9};
    }

    public static final class AethericElytra {
        /** One-shot forward impulse applied by the boost key while gliding. */
        public double[] boostImpulse = {0.90D, 1.10D, 1.35D, 1.65D};
        public int boostCooldownTicks = 60;
    }

    public static final class FixedUtilities {
        public double farSightFovMultiplier = 0.45D;
        public long aquaticActivationCost = 2_048L;
        public int aquaticWaterBreathingTicks = 1_200;
        public int aquaticDolphinsGraceTicks = 600;
    }

    public static final class CustomUtilities {
        public long alchemicalAnvilEmcPerMissingLevel = 128L;
        public long[] activeMeleeCostPerSecond = {64L, 512L, 4_096L, 32_768L};
        public long[] activeGenericCostPerSecond = {32L, 256L, 2_048L, 16_384L};
        public float[] activeMeleeBonusDamage = {1, 2, 3, 4};
        public AttractionCatalyst attractionCatalyst = new AttractionCatalyst();
        public Chalice chalice = new Chalice();
        public VoidTranslocator voidTranslocator = new VoidTranslocator();
        public Teleport teleport = new Teleport();
    }

    public static final class AttractionCatalyst {
        /** Horizontal/cubic diameter without a Core. Core I-IV add two blocks each. */
        public int baseDiameter = 3;
        public int diameterPerCore = 2;
        /** Fixed operating price; Core only expands the collection area. */
        public long emcCostPerTick = 8L;
        public double pullStrength = 0.25D;
    }

    public static final class Chalice {
        /** Horizontal diameter: 3/5/7/9/11 for no Core through Core IV. */
        public int baseDiameter = 3;
        public int diameterPerCore = 2;
        /** Small vertical scan keeps the advertised diameter horizontal and avoids 11^3 bursts. */
        public int verticalRadius = 1;
        public long voidBaseCost = 200L;
        public long waterCostPerBlock = 10L;
        public long lavaCostPerBlock = 50L;
        public long otherFluidCostPerBlock = 20L;
        /** Fixed unit cost for each plant actually grown. */
        public long growthCostPerBlock = 64L;
    }

    public static final class VoidTranslocator {
        /** Base range for Core I-IV. A full Aetheric set adds 20% at runtime. */
        public double[] range = {15.0D, 20.0D, 25.0D, 30.0D};
        /** Fixed activation price independent of Core tier. */
        public long emcCost = 1_024L;
        public int cooldownTicks = 120;
    }

    public static final class Teleport {
        public long crossDimensionSurcharge = 2_500L;
        /** Base rate used by Soulbound Recovery Compass and other teleports. */
        public double emcPerBlock = 2.5D;
        /** Magic Mirror rate for no Core through Core IV. */
        public double[] magicMirrorEmcPerBlock = {2.5D, 2.0D, 1.5D, 1.0D, 0.5D};
        /** Same-dimension maximum distance for Core I-IV. Cross-dimension travel uses the surcharge. */
        public double[] magicMirrorRange = {1_024.0D, 2_048.0D, 4_096.0D, 8_192.0D};
        public int magicMirrorUseDurationTicks = 60;
        public int magicMirrorCooldownTicks = 300;
    }

    public static final class Progression {
        public WorldTierProgression worldTiers = new WorldTierProgression();
    }

    public static final class WorldTierProgression {
        /** NONE, LINEAR or MULTIPLICATIVE. */
        public String scalingMode = "MULTIPLICATIVE";
        /** Used by MULTIPLICATIVE: requirement × multiplier^(contributors - 1). */
        public double perAdditionalPlayerMultiplier = 1.20D;
        /** Used by LINEAR: requirement × (1 + increase × additional contributors). */
        public double linearIncreasePerAdditionalPlayer = 0.50D;
        /** Hard cap applied after either scaling formula. */
        public double maximumScale = 3.0D;
        /** Zero means no contributor cap. */
        public int maximumContributors = 10;
        /** Boss counts stay fixed by default; servers may opt into scaling them. */
        public boolean scaleBossObjectives = false;
        public OverworldTierRequirements overworld = new OverworldTierRequirements();
        public NetherTierRequirements nether = new NetherTierRequirements();
        public EndTierRequirements end = new EndTierRequirements();
        public DeepWorldTierRequirements deepWorld = new DeepWorldTierRequirements();
    }

    public static final class OverworldTierRequirements {
        public long totalKills = 180L;
        public long zombieFamily = 40L;
        public long skeletonFamily = 35L;
        public long spiderFamily = 25L;
        public long creeper = 20L;
        public long enderman = 8L;
        public long witch = 5L;
        public long illager = 12L;
    }

    public static final class NetherTierRequirements {
        public long totalKills = 180L;
        public long piglinFamily = 35L;
        public long zombifiedPiglin = 40L;
        public long blaze = 25L;
        public long magmaCube = 20L;
        public long ghast = 8L;
        public long witherSkeleton = 20L;
        public long wither = 1L;
    }

    public static final class EndTierRequirements {
        public long enderman = 50L;
        public long shulker = 20L;
        public long enderDragon = 1L;
    }

    public static final class DeepWorldTierRequirements {
        public long guardian = 40L;
        public long elderGuardian = 3L;
        public long warden = 1L;
    }


    public static final class WorldSystems {
        public AuraNodes auraNodes = new AuraNodes();
        public BossPlayerScaling bossPlayerScaling = new BossPlayerScaling();
        public MobThreatScaling mobThreatScaling = new MobThreatScaling();
    }

    public static final class AuraNodes {
        public boolean enabled = true;
        /** Ten minutes of active output at the corresponding tier rate. */
        public long[] reserve = {1_228_800L, 4_915_200L, 19_660_800L, 78_643_200L};
        public long[] outputPerPlayer = {2_048L, 8_192L, 32_768L, 131_072L};
        /** Every tier alternates ten active minutes with ten recharge minutes. */
        public int[] rechargeTicks = {12_000, 12_000, 12_000, 12_000};
        public int[] spacingBlocks = {600, 1_000, 1_500, 2_000};
        public int[] outputRadius = {8, 12, 16, 24};
        public int[] siphonRadius = {8, 12, 16, 24};
        public float[] siphonDamage = {1.0F, 2.0F, 4.0F, 8.0F};
        public int pulseIntervalTicks = 20;
        public int spawnExclusionRadius = 256;
        /** Deterministic chance for one Tier IV tower per generated Ancient City structure start. */
        public double ancientCitySpawnChance = 0.50D;
    }

    public static final class BossPlayerScaling {
        public boolean enabled = true;
        /** Zero means no cap. */
        public int maxCountedPlayers = 10;
        public double healthPerPlayer = 100.0D;
        public float damagePerPlayer = 2.0F;
        public boolean enderDragon = true;
        public boolean wither = true;
        public boolean warden = true;
    }

    public static final class MobThreatScaling {
        public boolean enabled = true;
        /** Legacy fields retained so existing configs still load cleanly. */
        public double healthPerTier = 10.0D;
        public float damagePerTier = 2.0F;
        public double lowHealthCapRatio = 0.5D;
        /** Tier 0 through IV milestone multipliers and bonuses. */
        public double[] healthMultiplierByTier = {1.0D, 1.35D, 1.75D, 2.30D, 3.20D};
        public float[] damageMultiplierByTier = {1.0F, 1.20F, 1.45F, 1.80F, 2.30F};
        public double[] armorBonusByTier = {0.0D, 2.0D, 5.0D, 9.0D, 15.0D};
        public double[] knockbackResistanceByTier = {0.0D, 0.0D, 0.05D, 0.10D, 0.20D};
        /** Rare control-oriented Arcane mobs: none at T0-I, then 2%, 5%, 8%. */
        public double[] arcaneMobChanceByTier = {0.0D, 0.0D, 0.02D, 0.05D, 0.08D};
        public boolean scaleModdedHostiles = true;
        public String[] excludedEntities = {};
    }

}
