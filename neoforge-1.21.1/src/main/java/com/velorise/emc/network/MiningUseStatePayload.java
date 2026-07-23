package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client input heartbeat for context-activated Imbued mining tools. */
public record MiningUseStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<MiningUseStatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "mining_use_state"));

    public static final StreamCodec<FriendlyByteBuf, MiningUseStatePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeBoolean(payload.active),
        buffer -> new MiningUseStatePayload(buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
