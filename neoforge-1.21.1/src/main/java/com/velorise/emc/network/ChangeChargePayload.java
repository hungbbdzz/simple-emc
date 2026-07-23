package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChangeChargePayload(boolean decrease) implements CustomPacketPayload {
    public static final Type<ChangeChargePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "change_charge"));

    public static final StreamCodec<FriendlyByteBuf, ChangeChargePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBoolean(payload.decrease()),
        buf -> new ChangeChargePayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
