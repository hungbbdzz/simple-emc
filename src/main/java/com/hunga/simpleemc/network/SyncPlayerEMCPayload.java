package com.hunga.simpleemc.network;

import com.hunga.simpleemc.SimpleEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncPlayerEMCPayload(long emc, List<ResourceLocation> learned) implements CustomPacketPayload {
    public static final Type<SyncPlayerEMCPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEMC.MODID, "sync_player_emc"));

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerEMCPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeLong(payload.emc());
            buf.writeVarInt(payload.learned().size());
            for (ResourceLocation rl : payload.learned()) {
                buf.writeResourceLocation(rl);
            }
        },
        buf -> {
            long emc = buf.readLong();
            int size = buf.readVarInt();
            List<ResourceLocation> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(buf.readResourceLocation());
            }
            return new SyncPlayerEMCPayload(emc, list);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
