package com.velorise.emc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client synchronization of normalized gameplay balance settings. */
public record SyncGameplayConfigMessage(String json) {
    private static final int MAX_JSON_LENGTH = 262_144;

    public SyncGameplayConfigMessage {
        json = json == null ? "" : json;
        if (json.length() > MAX_JSON_LENGTH) throw new IllegalArgumentException("Gameplay config payload is too large");
    }

    public static void encode(SyncGameplayConfigMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.json, MAX_JSON_LENGTH);
    }

    public static SyncGameplayConfigMessage decode(FriendlyByteBuf buffer) {
        return new SyncGameplayConfigMessage(buffer.readUtf(MAX_JSON_LENGTH));
    }

    public static void handle(SyncGameplayConfigMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
            net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> ClientMessageHandler.handleSyncGameplayConfig(message)
        ));
        context.get().setPacketHandled(true);
    }
}
