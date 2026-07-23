package com.velorise.emc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.velorise.emc.common.EmcSourceMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerEMC {
    private long emc;
    private final Set<Item> learnedItems;
    private EmcSourceMode sourceMode;
    private long safetyThreshold;
    private boolean hudEnabled;
    private long inventoryFuelBuffer;
    private long transmutationUsage;
    private long transmutationUsageDecayMillis;
    private long transmutationTaxRemainder;
    private long totalMaterializedItems;
    private long totalMaterializationTaxPaid;

    public static final Codec<PlayerEMC> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("emc", 0L).forGetter(PlayerEMC::getEmc),
        Codec.list(ResourceLocation.CODEC).optionalFieldOf("learned_items", List.of()).forGetter(PlayerEMC::getLearnedItemsRL),
        Codec.INT.optionalFieldOf("source_mode", EmcSourceMode.FUND_ONLY.ordinal()).forGetter(data -> data.getSourceMode().ordinal()),
        Codec.LONG.optionalFieldOf("safety_threshold", 0L).forGetter(PlayerEMC::getSafetyThreshold),
        Codec.BOOL.optionalFieldOf("hud_enabled", true).forGetter(PlayerEMC::isHudEnabled),
        Codec.LONG.optionalFieldOf("inventory_fuel_buffer", 0L).forGetter(PlayerEMC::getInventoryFuelBuffer),
        Codec.LONG.optionalFieldOf("transmutation_usage", 0L).forGetter(PlayerEMC::getTransmutationUsage),
        Codec.LONG.optionalFieldOf("transmutation_usage_decay_millis", 0L).forGetter(PlayerEMC::getTransmutationUsageDecayMillis),
        Codec.LONG.optionalFieldOf("transmutation_tax_remainder", 0L).forGetter(PlayerEMC::getTransmutationTaxRemainder),
        Codec.LONG.optionalFieldOf("total_materialized_items", 0L).forGetter(PlayerEMC::getTotalMaterializedItems),
        Codec.LONG.optionalFieldOf("total_materialization_tax_paid", 0L).forGetter(PlayerEMC::getTotalMaterializationTaxPaid)
    ).apply(instance, PlayerEMC::new));

    public PlayerEMC() {
        this(0L, List.of(), EmcSourceMode.FUND_ONLY.ordinal(), 0L, true, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    public PlayerEMC(long emc, List<ResourceLocation> learnedRLs) {
        this(emc, learnedRLs, EmcSourceMode.FUND_ONLY.ordinal(), 0L, true, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    public PlayerEMC(long emc, List<ResourceLocation> learnedRLs, int sourceMode, long safetyThreshold,
                     boolean hudEnabled, long inventoryFuelBuffer) {
        this(emc, learnedRLs, sourceMode, safetyThreshold, hudEnabled, inventoryFuelBuffer, 0L, 0L, 0L, 0L, 0L);
    }

    public PlayerEMC(long emc, List<ResourceLocation> learnedRLs, int sourceMode, long safetyThreshold,
                     boolean hudEnabled, long inventoryFuelBuffer, long transmutationUsage,
                     long transmutationUsageDecayMillis, long transmutationTaxRemainder,
                     long totalMaterializedItems, long totalMaterializationTaxPaid) {
        this.emc = Math.max(0L, emc);
        this.learnedItems = new HashSet<>();
        for (ResourceLocation id : learnedRLs) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != Items.AIR) learnedItems.add(item);
        }
        this.sourceMode = EmcSourceMode.byId(sourceMode);
        this.safetyThreshold = Math.max(0L, safetyThreshold);
        this.hudEnabled = hudEnabled;
        this.inventoryFuelBuffer = Math.max(0L, inventoryFuelBuffer);
        this.transmutationUsage = Math.max(0L, transmutationUsage);
        this.transmutationUsageDecayMillis = Math.max(0L, transmutationUsageDecayMillis);
        setTransmutationTaxRemainder(transmutationTaxRemainder);
        this.totalMaterializedItems = Math.max(0L, totalMaterializedItems);
        this.totalMaterializationTaxPaid = Math.max(0L, totalMaterializationTaxPaid);
    }

    public long getEmc() { return emc; }
    public void setEmc(long emc) { this.emc = Math.max(0L, emc); }
    public void addEmc(long amount) {
        if (amount <= 0L) return;
        emc = emc > Long.MAX_VALUE - amount ? Long.MAX_VALUE : emc + amount;
    }
    public boolean removeEmc(long amount) {
        if (amount <= 0L) return true;
        if (emc < amount) return false;
        emc -= amount;
        return true;
    }
    public Set<Item> getLearnedItems() { return learnedItems; }
    public List<ResourceLocation> getLearnedItemsRL() {
        List<ResourceLocation> list = new ArrayList<>();
        for (Item item : learnedItems) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) list.add(id);
        }
        return list;
    }
    public boolean learnItem(Item item) { return item != null && item != Items.AIR && learnedItems.add(item); }
    public boolean unlearnItem(Item item) { return learnedItems.remove(item); }
    public EmcSourceMode getSourceMode() { return sourceMode; }
    public void setSourceMode(EmcSourceMode sourceMode) { this.sourceMode = sourceMode == null || sourceMode == EmcSourceMode.HYBRID ? EmcSourceMode.FUND_ONLY : sourceMode; }
    public long getSafetyThreshold() { return safetyThreshold; }
    public void setSafetyThreshold(long safetyThreshold) { this.safetyThreshold = Math.max(0L, safetyThreshold); }
    public boolean isHudEnabled() { return hudEnabled; }
    public void setHudEnabled(boolean hudEnabled) { this.hudEnabled = hudEnabled; }
    public long getInventoryFuelBuffer() { return inventoryFuelBuffer; }
    public void setInventoryFuelBuffer(long inventoryFuelBuffer) { this.inventoryFuelBuffer = Math.max(0L, inventoryFuelBuffer); }
    public long getTransmutationUsage() { return transmutationUsage; }
    public void setTransmutationUsage(long transmutationUsage) { this.transmutationUsage = Math.max(0L, transmutationUsage); }
    public long getTransmutationUsageDecayMillis() { return transmutationUsageDecayMillis; }
    public void setTransmutationUsageDecayMillis(long value) { this.transmutationUsageDecayMillis = Math.max(0L, value); }
    public long getTransmutationTaxRemainder() { return transmutationTaxRemainder; }
    public void setTransmutationTaxRemainder(long value) {
        // Kept only for alpha save/network compatibility after percentage tax removal.
        this.transmutationTaxRemainder = 0L;
    }
    public long getTotalMaterializedItems() { return totalMaterializedItems; }
    public void setTotalMaterializedItems(long amount) { this.totalMaterializedItems = Math.max(0L, amount); }
    public void addMaterializedItems(long amount) { this.totalMaterializedItems = saturatingAdd(this.totalMaterializedItems, amount); }
    public long getTotalMaterializationTaxPaid() { return totalMaterializationTaxPaid; }
    public void setTotalMaterializationTaxPaid(long amount) { this.totalMaterializationTaxPaid = Math.max(0L, amount); }
    public void addMaterializationTaxPaid(long amount) { this.totalMaterializationTaxPaid = saturatingAdd(this.totalMaterializationTaxPaid, amount); }

    private static long saturatingAdd(long current, long amount) {
        if (amount <= 0L) return Math.max(0L, current);
        return current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
    }
}
