package com.hunga.simpleemc.network;

import com.hunga.simpleemc.SimpleEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public record RequestUpdateEMCPayload(Map<ResourceLocation, Long> overrides) implements CustomPacketPayload {
    public static final Type<RequestUpdateEMCPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEMC.MODID, "request_update_emc"));

    public static final StreamCodec<FriendlyByteBuf, RequestUpdateEMCPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.overrides().size());
            payload.overrides().forEach((rl, val) -> {
                buf.writeResourceLocation(rl);
                buf.writeLong(val);
            });
        },
        buf -> {
            int size = buf.readVarInt();
            Map<ResourceLocation, Long> map = new HashMap<>();
            for (int i = 0; i < size; i++) {
                map.put(buf.readResourceLocation(), buf.readLong());
            }
            return new RequestUpdateEMCPayload(map);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
