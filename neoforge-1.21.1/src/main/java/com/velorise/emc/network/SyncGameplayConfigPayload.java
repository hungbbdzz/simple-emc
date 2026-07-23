package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-to-client synchronization of normalized gameplay balance settings. */
public record SyncGameplayConfigPayload(String json) implements CustomPacketPayload {
    private static final int MAX_JSON_LENGTH = 262_144;

    public SyncGameplayConfigPayload {
        json = json == null ? "" : json;
        if (json.length() > MAX_JSON_LENGTH) throw new IllegalArgumentException("Gameplay config payload is too large");
    }

    public static final Type<SyncGameplayConfigPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "sync_gameplay_config")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncGameplayConfigPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeUtf(payload.json(), MAX_JSON_LENGTH),
        buffer -> new SyncGameplayConfigPayload(buffer.readUtf(MAX_JSON_LENGTH))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
