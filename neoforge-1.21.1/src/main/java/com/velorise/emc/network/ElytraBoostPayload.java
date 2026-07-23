package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ElytraBoostPayload() implements CustomPacketPayload {
    public static final Type<ElytraBoostPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "elytra_boost"));
    public static final StreamCodec<FriendlyByteBuf, ElytraBoostPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {}, buffer -> new ElytraBoostPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
