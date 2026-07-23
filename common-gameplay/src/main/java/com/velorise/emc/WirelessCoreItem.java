package com.velorise.emc;

import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.world.item.Item;

/** A concrete Core tier item used by Smithing recipes and equipment upgrades. */
public class WirelessCoreItem extends Item {
    private final WirelessCoreTier tier;

    public WirelessCoreItem(Properties properties, WirelessCoreTier tier) {
        super(properties);
        this.tier = tier == null ? WirelessCoreTier.I : tier;
    }

    public WirelessCoreTier tier() {
        return tier;
    }
}
