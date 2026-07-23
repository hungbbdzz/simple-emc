package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenConfigScreenPayload() implements CustomPacketPayload {
    public static final Type<OpenConfigScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "open_config_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenConfigScreenPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {},
        buf -> new OpenConfigScreenPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
