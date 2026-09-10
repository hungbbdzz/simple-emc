package com.velorise.emc.network;

import com.velorise.emc.EMCRegistry;
import com.velorise.emc.EMCConfigScreen;
import com.velorise.emc.ClientWorldProgress;
import com.velorise.emc.capability.PlayerEMCCapability;
import com.velorise.emc.common.EmcSourceMode;
import com.velorise.emc.common.EmcPolicyOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.velorise.emc.MutableJukeboxSoundInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClientMessageHandler {
    private record JukeboxSoundKey(ResourceLocation dimension, long blockPos) {}

    private static final Map<JukeboxSoundKey, MutableJukeboxSoundInstance> JUKEBOX_SOUNDS = new HashMap<>();
    private static int lastEmcSyncFingerprint = Integer.MIN_VALUE;
    private static boolean pendingEmcRecalculation;

    public static void handleSyncPlayerEMC(SyncPlayerEMCMessage msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientWorldProgress.install(msg.worldProgressCounts(), msg.worldProgressContributors());
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
                data.setSourceMode(EmcSourceMode.byId(msg.sourceMode()));
                data.setSafetyThreshold(msg.safetyThreshold());
                data.setHudEnabled(msg.hudEnabled());
                data.setInventoryFuelBuffer(msg.inventoryFuelBuffer());
                data.setTransmutationUsage(msg.transmutationUsage());
                // The server sends an absolute timestamp. Re-anchor it to the client's clock
                // so the UI preview is not distorted by server/client clock skew.
                data.setTransmutationUsageDecayMillis(System.currentTimeMillis());
                data.setTransmutationTaxRemainder(msg.transmutationTaxRemainder());
                data.setTotalMaterializedItems(msg.totalMaterializedItems());
                data.setTotalMaterializationTaxPaid(msg.totalMaterializationTaxPaid());
            });
        }
    }

    public static void handleSyncCustomEMC(SyncCustomEMCMessage msg) {
        int fingerprint = Objects.hash(msg.getValueMode(), msg.getOverrides(), msg.getPolicyOverrides());
        if (fingerprint == lastEmcSyncFingerprint) return;

        EMCRegistry.setClientSyncedValueMode(msg.getValueMode());
        Map<Item, Long> clientOverrides = new HashMap<>();
        msg.getOverrides().forEach((rl, val) -> {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != Items.AIR) clientOverrides.put(item, val);
        });
        EMCRegistry.setClientSyncedOverrides(clientOverrides);
        Map<Item, EmcPolicyOverride> clientPolicies = new HashMap<>();
        msg.getPolicyOverrides().forEach((rl, policy) -> {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != Items.AIR && policy != null) clientPolicies.put(item, policy);
        });
        EMCRegistry.setClientSyncedPolicyOverrides(clientPolicies);

        lastEmcSyncFingerprint = fingerprint;
        pendingEmcRecalculation = true;
        flushPendingEmcRecalculation();
    }

    public static void flushPendingEmcRecalculation() {
        if (!pendingEmcRecalculation) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.getConnection() == null) return;
        pendingEmcRecalculation = false;
        EMCRegistry.clientReloadAndRecalculate(
            minecraft.getConnection().getRecipeManager(), level.registryAccess());
    }

    public static void clearServerSyncState() {
        clearJukeboxAudio();
        lastEmcSyncFingerprint = Integer.MIN_VALUE;
        pendingEmcRecalculation = false;
        EMCRegistry.resetClientRecalculationGuard();
    }

    public static void handleOpenConfigScreen(OpenConfigScreenMessage msg) {
        Minecraft.getInstance().setScreen(new EMCConfigScreen(null));
    }


    public static void handleJukeboxAudio(JukeboxAudioMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        JukeboxSoundKey key = new JukeboxSoundKey(level.dimension().location(), message.pos().asLong());
        if (message.action() == JukeboxAudioMessage.Action.VOLUME) {
            MutableJukeboxSoundInstance current = JUKEBOX_SOUNDS.get(key);
            if (current != null) current.setPlaybackVolume(message.volume());
            return;
        }

        MutableJukeboxSoundInstance previous = JUKEBOX_SOUNDS.remove(key);
        if (previous != null) minecraft.getSoundManager().stop(previous);
        if (message.action() != JukeboxAudioMessage.Action.PLAY || message.soundId() == null) return;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(message.soundId());
        if (sound == null) return;
        MutableJukeboxSoundInstance instance = new MutableJukeboxSoundInstance(sound, message.pos(), message.volume());
        JUKEBOX_SOUNDS.put(key, instance);
        minecraft.getSoundManager().play(instance);
    }

    public static void clearJukeboxAudio() {
        Minecraft minecraft = Minecraft.getInstance();
        for (MutableJukeboxSoundInstance sound : JUKEBOX_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        JUKEBOX_SOUNDS.clear();
    }
    public static void handleSyncGameplayConfig(SyncGameplayConfigMessage message) {
        com.velorise.emc.GameplayConfigManager.applyClientJson(message.json());
    }

}
