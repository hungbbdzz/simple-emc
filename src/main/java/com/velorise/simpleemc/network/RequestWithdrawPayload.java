package com.velorise.simpleemc.network;

import com.velorise.simpleemc.SimpleEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestWithdrawPayload(ResourceLocation itemRL, boolean withdrawStack, boolean targetGrid) implements CustomPacketPayload {
    public static final Type<RequestWithdrawPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEMC.MODID, "request_withdraw"));

    public static final StreamCodec<FriendlyByteBuf, RequestWithdrawPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeResourceLocation(payload.itemRL());
            buf.writeBoolean(payload.withdrawStack());
            buf.writeBoolean(payload.targetGrid());
        },
        buf -> new RequestWithdrawPayload(buf.readResourceLocation(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
