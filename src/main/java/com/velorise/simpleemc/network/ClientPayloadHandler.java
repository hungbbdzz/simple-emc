package com.velorise.simpleemc.network;

import com.velorise.simpleemc.PlayerEMC;
import com.velorise.simpleemc.SimpleEMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handle(final SyncPlayerEMCPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(SimpleEMC.PLAYER_EMC.get(), new PlayerEMC(payload.emc(), payload.learned()));
            }
        });
    }

    public static void handleCustomEMCSync(final SyncCustomEMCPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                java.util.Map<net.minecraft.world.item.Item, Long> clientOverrides = new java.util.HashMap<>();
                payload.overrides().forEach((rl, val) -> {
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        clientOverrides.put(item, val);
                    }
                });
                com.velorise.simpleemc.EMCRegistry.applyCustomOverridesAndRecalculate(
                    clientOverrides,
                    level.getRecipeManager(),
                    level.registryAccess()
                );
            }
        });
    }

    public static void handleOpenConfigScreen(final OpenConfigScreenPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new com.velorise.simpleemc.EMCConfigScreen(null));
        });
    }
}
