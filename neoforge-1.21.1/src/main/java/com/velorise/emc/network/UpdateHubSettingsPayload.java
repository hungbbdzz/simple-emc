package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateHubSettingsPayload(int sourceMode, long safetyThreshold, boolean hudEnabled)
    implements CustomPacketPayload {
    public static final Type<UpdateHubSettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "update_hub_settings"));
    public static final StreamCodec<FriendlyByteBuf, UpdateHubSettingsPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.sourceMode());
            buf.writeLong(payload.safetyThreshold());
            buf.writeBoolean(payload.hudEnabled());
        },
        buf -> new UpdateHubSettingsPayload(buf.readVarInt(), buf.readLong(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
