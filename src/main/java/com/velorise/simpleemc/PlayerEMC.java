package com.velorise.simpleemc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

    public static final Codec<PlayerEMC> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("emc").forGetter(PlayerEMC::getEmc),
            Codec.list(ResourceLocation.CODEC).fieldOf("learned_items").forGetter(PlayerEMC::getLearnedItemsRL)
        ).apply(instance, PlayerEMC::new)
    );

    public PlayerEMC() {
        this.emc = 0;
        this.learnedItems = new HashSet<>();
    }

    public PlayerEMC(long emc, List<ResourceLocation> learnedRLs) {
        this.emc = emc;
        this.learnedItems = new HashSet<>();
        for (ResourceLocation rl : learnedRLs) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != Items.AIR) {
                this.learnedItems.add(item);
            }
        }
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
}
