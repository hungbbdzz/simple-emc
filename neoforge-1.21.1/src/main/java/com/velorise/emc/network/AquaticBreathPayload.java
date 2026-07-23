package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AquaticBreathPayload() implements CustomPacketPayload {
    public static final Type<AquaticBreathPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "aquatic_breath"));
    public static final StreamCodec<FriendlyByteBuf, AquaticBreathPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {}, buffer -> new AquaticBreathPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
