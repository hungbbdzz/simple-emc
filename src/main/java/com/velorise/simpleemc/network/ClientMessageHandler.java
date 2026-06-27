package com.velorise.simpleemc.network;

import com.velorise.simpleemc.EMCRegistry;
import com.velorise.simpleemc.EMCConfigScreen;
import com.velorise.simpleemc.capability.PlayerEMCCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

public class ClientMessageHandler {
    
    public static void handleSyncPlayerEMC(SyncPlayerEMCMessage msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
                data.setEmc(msg.emc());
                data.getLearnedItems().clear();
                for (ResourceLocation rl : msg.learned()) {
                    Item item = BuiltInRegistries.ITEM.get(rl);
                    if (item != null && item != Items.AIR) {
                        data.learnItem(item);
                    }
                }
            });
        }
    }

    public static void handleSyncCustomEMC(SyncCustomEMCMessage msg) {
        Map<Item, Long> clientOverrides = new HashMap<>();
        msg.getOverrides().forEach((rl, val) -> {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != Items.AIR) {
                clientOverrides.put(item, val);
            }
        });
        EMCRegistry.setClientSyncedOverrides(clientOverrides);

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            EMCRegistry.clientReloadAndRecalculate(
                level.getRecipeManager(),
                level.registryAccess()
            );
        }
    }

    public static void handleOpenConfigScreen(OpenConfigScreenMessage msg) {
        Minecraft.getInstance().setScreen(new EMCConfigScreen(null));
    }
}
