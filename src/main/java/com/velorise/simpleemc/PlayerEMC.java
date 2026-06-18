package com.velorise.simpleemc;

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

    public PlayerEMC() {
        this.emc = 0;
        this.learnedItems = new HashSet<>();
    }

    public long getEmc() {
        return emc;
    }

    public void setEmc(long emc) {
        this.emc = emc;
    }

    public void addEmc(long amount) {
        this.emc += amount;
    }

    public boolean removeEmc(long amount) {
        if (this.emc >= amount) {
            this.emc -= amount;
            return true;
        }
        return false;
    }

    public Set<Item> getLearnedItems() {
        return learnedItems;
    }

    public List<ResourceLocation> getLearnedItemsRL() {
        List<ResourceLocation> list = new ArrayList<>();
        for (Item item : learnedItems) {
            list.add(BuiltInRegistries.ITEM.getKey(item));
        }
        return list;
    }

    public boolean learnItem(Item item) {
        return learnedItems.add(item);
    }

    public boolean unlearnItem(Item item) {
        return learnedItems.remove(item);
    }

    public void copyFrom(PlayerEMC other) {
        this.emc = other.emc;
        this.learnedItems.clear();
        this.learnedItems.addAll(other.learnedItems);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("emc", this.emc);

        ListTag learnedList = new ListTag();
        for (Item item : learnedItems) {
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
            if (rl != null) {
                learnedList.add(StringTag.valueOf(rl.toString()));
            }
        }
        tag.put("learned_items", learnedList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.emc = tag.getLong("emc");
        this.learnedItems.clear();
        if (tag.contains("learned_items", Tag.TAG_LIST)) {
            ListTag list = tag.getList("learned_items", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl != null) {
                    Item item = BuiltInRegistries.ITEM.get(rl);
                    if (item != null && item != Items.AIR) {
                        this.learnedItems.add(item);
                    }
                }
            }
        }
    }
}
