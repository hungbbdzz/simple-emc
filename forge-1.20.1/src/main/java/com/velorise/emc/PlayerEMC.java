package com.velorise.emc;

import com.velorise.emc.common.EmcSourceMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerEMC implements INBTSerializable<CompoundTag> {
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

    public PlayerEMC() {
        this.emc = 0L;
        this.learnedItems = new HashSet<>();
        this.sourceMode = EmcSourceMode.FUND_ONLY;
        this.safetyThreshold = 0L;
        this.hudEnabled = true;
        this.inventoryFuelBuffer = 0L;
        this.transmutationUsage = 0L;
        this.transmutationUsageDecayMillis = 0L;
        this.transmutationTaxRemainder = 0L;
        this.totalMaterializedItems = 0L;
        this.totalMaterializationTaxPaid = 0L;
    }

    public long getEmc() { return emc; }
    public void setEmc(long emc) { this.emc = Math.max(0L, emc); }
    public void addEmc(long amount) {
        if (amount <= 0L) return;
        this.emc = this.emc > Long.MAX_VALUE - amount ? Long.MAX_VALUE : this.emc + amount;
    }
    public boolean removeEmc(long amount) {
        if (amount <= 0L) return true;
        if (this.emc < amount) return false;
        this.emc -= amount;
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
    public void setSourceMode(EmcSourceMode sourceMode) {
        this.sourceMode = sourceMode == null || sourceMode == EmcSourceMode.HYBRID ? EmcSourceMode.FUND_ONLY : sourceMode;
    }
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

    public void copyFrom(PlayerEMC other) {
        this.emc = other.emc;
        this.learnedItems.clear();
        this.learnedItems.addAll(other.learnedItems);
        this.sourceMode = other.sourceMode;
        this.safetyThreshold = other.safetyThreshold;
        this.hudEnabled = other.hudEnabled;
        this.inventoryFuelBuffer = other.inventoryFuelBuffer;
        this.transmutationUsage = other.transmutationUsage;
        this.transmutationUsageDecayMillis = other.transmutationUsageDecayMillis;
        this.transmutationTaxRemainder = other.transmutationTaxRemainder;
        this.totalMaterializedItems = other.totalMaterializedItems;
        this.totalMaterializationTaxPaid = other.totalMaterializationTaxPaid;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("emc", emc);
        tag.putInt("source_mode", sourceMode.ordinal());
        tag.putLong("safety_threshold", safetyThreshold);
        tag.putBoolean("hud_enabled", hudEnabled);
        tag.putLong("inventory_fuel_buffer", inventoryFuelBuffer);
        tag.putLong("transmutation_usage", transmutationUsage);
        tag.putLong("transmutation_usage_decay_millis", transmutationUsageDecayMillis);
        tag.putLong("transmutation_tax_remainder", transmutationTaxRemainder);
        tag.putLong("total_materialized_items", totalMaterializedItems);
        tag.putLong("total_materialization_tax_paid", totalMaterializationTaxPaid);
        ListTag learnedList = new ListTag();
        for (Item item : learnedItems) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) learnedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("learned_items", learnedList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        emc = Math.max(0L, tag.getLong("emc"));
        sourceMode = tag.contains("source_mode", Tag.TAG_INT)
            ? EmcSourceMode.byId(tag.getInt("source_mode")) : EmcSourceMode.FUND_ONLY;
        safetyThreshold = tag.contains("safety_threshold", Tag.TAG_LONG)
            ? Math.max(0L, tag.getLong("safety_threshold")) : 0L;
        hudEnabled = !tag.contains("hud_enabled", Tag.TAG_BYTE) || tag.getBoolean("hud_enabled");
        inventoryFuelBuffer = tag.contains("inventory_fuel_buffer", Tag.TAG_LONG)
            ? Math.max(0L, tag.getLong("inventory_fuel_buffer")) : 0L;
        transmutationUsage = tag.contains("transmutation_usage", Tag.TAG_LONG)
            ? Math.max(0L, tag.getLong("transmutation_usage")) : 0L;
        transmutationUsageDecayMillis = tag.contains("transmutation_usage_decay_millis", Tag.TAG_LONG)
            ? Math.max(0L, tag.getLong("transmutation_usage_decay_millis")) : 0L;
        setTransmutationTaxRemainder(tag.contains("transmutation_tax_remainder", Tag.TAG_LONG)
            ? tag.getLong("transmutation_tax_remainder") : 0L);
        totalMaterializedItems = tag.contains("total_materialized_items", Tag.TAG_LONG)
            ? Math.max(0L, tag.getLong("total_materialized_items")) : 0L;
        totalMaterializationTaxPaid = tag.contains("total_materialization_tax_paid", Tag.TAG_LONG)
            ? Math.max(0L, tag.getLong("total_materialization_tax_paid")) : 0L;
        learnedItems.clear();
        if (tag.contains("learned_items", Tag.TAG_LIST)) {
            ListTag list = tag.getList("learned_items", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null && item != Items.AIR) learnedItems.add(item);
            }
        }
    }
}
