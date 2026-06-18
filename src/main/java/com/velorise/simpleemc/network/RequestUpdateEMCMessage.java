package com.velorise.simpleemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RequestUpdateEMCMessage {
    private final Map<ResourceLocation, Long> overrides;

    public RequestUpdateEMCMessage(Map<ResourceLocation, Long> overrides) {
        this.overrides = overrides;
    }

    public Map<ResourceLocation, Long> getOverrides() { return overrides; }

    public static void encode(RequestUpdateEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.overrides.size());
        msg.overrides.forEach((rl, val) -> {
            buf.writeResourceLocation(rl);
            buf.writeLong(val);
        });
    }

    public static RequestUpdateEMCMessage decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readResourceLocation(), buf.readLong());
        }
        return new RequestUpdateEMCMessage(map);
    }

    public static void handle(RequestUpdateEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ServerMessageHandler.handleUpdateEMC(msg, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
