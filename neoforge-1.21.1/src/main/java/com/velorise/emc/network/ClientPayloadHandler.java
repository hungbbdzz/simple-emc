package com.velorise.emc.network;

import com.velorise.emc.PlayerEMC;
import com.velorise.emc.ClientWorldProgress;
import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.common.EmcPolicyOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.velorise.emc.MutableJukeboxSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public class ClientPayloadHandler {
    private record JukeboxSoundKey(ResourceLocation dimension, long blockPos) {}

    private static final Map<JukeboxSoundKey, MutableJukeboxSoundInstance> JUKEBOX_SOUNDS = new HashMap<>();
    public static void handle(final SyncPlayerEMCPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientWorldProgress.install(payload.worldProgressCounts(), payload.worldProgressContributors());
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(VeloriseEMC.PLAYER_EMC.get(), new PlayerEMC(
                    payload.emc(), payload.learned(), payload.sourceMode(), payload.safetyThreshold(),
                    payload.hudEnabled(), payload.inventoryFuelBuffer(), payload.transmutationUsage(),
                    System.currentTimeMillis(), payload.transmutationTaxRemainder(), payload.totalMaterializedItems(),
                    payload.totalMaterializationTaxPaid()
                ));
            }
        });
    }

    public static void handleCustomEMCSync(final SyncCustomEMCPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            com.velorise.emc.EMCRegistry.setClientSyncedValueMode(payload.valueMode());
            java.util.Map<net.minecraft.world.item.Item, Long> clientOverrides = new java.util.HashMap<>();
            payload.overrides().forEach((rl, val) -> {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    clientOverrides.put(item, val);
                }
            });
            com.velorise.emc.EMCRegistry.setClientSyncedOverrides(clientOverrides);
            java.util.Map<net.minecraft.world.item.Item, EmcPolicyOverride> clientPolicies = new java.util.HashMap<>();
            payload.policyOverrides().forEach((rl, policy) -> {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != net.minecraft.world.item.Items.AIR && policy != null) {
                    clientPolicies.put(item, policy);
                }
            });
            com.velorise.emc.EMCRegistry.setClientSyncedPolicyOverrides(clientPolicies);

            net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                com.velorise.emc.EMCRegistry.clientReloadAndRecalculate(
                    level.getRecipeManager(),
                    level.registryAccess()
                );
            }
        });
    }

    public static void handleOpenConfigScreen(final OpenConfigScreenPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new com.velorise.emc.EMCConfigScreen(null));
        });
    }


    public static void handleJukeboxAudio(final JukeboxAudioPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) return;

            JukeboxSoundKey key = new JukeboxSoundKey(level.dimension().location(), payload.pos().asLong());
            if (payload.action() == JukeboxAudioPayload.Action.VOLUME) {
                MutableJukeboxSoundInstance current = JUKEBOX_SOUNDS.get(key);
                if (current != null) current.setPlaybackVolume(payload.volume());
                return;
            }

            MutableJukeboxSoundInstance previous = JUKEBOX_SOUNDS.remove(key);
            if (previous != null) minecraft.getSoundManager().stop(previous);
            if (payload.action() != JukeboxAudioPayload.Action.PLAY || payload.soundId() == null) return;

            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(payload.soundId());
            if (sound == null) return;
            MutableJukeboxSoundInstance instance = new MutableJukeboxSoundInstance(sound, payload.pos(), payload.volume());
            JUKEBOX_SOUNDS.put(key, instance);
            minecraft.getSoundManager().play(instance);
        });
    }

    public static void clearJukeboxAudio() {
        Minecraft minecraft = Minecraft.getInstance();
        for (MutableJukeboxSoundInstance sound : JUKEBOX_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        JUKEBOX_SOUNDS.clear();
    }
    public static void handleGameplayConfigSync(final SyncGameplayConfigPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> com.velorise.emc.GameplayConfigManager.applyClientJson(payload.json()));
    }

}
