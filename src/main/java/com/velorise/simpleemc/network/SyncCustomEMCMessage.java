package com.velorise.simpleemc.network;

import com.velorise.simpleemc.EMCRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCustomEMCMessage {
    private final Map<ResourceLocation, Long> overrides;

    public SyncCustomEMCMessage(Map<ResourceLocation, Long> overrides) {
        this.overrides = overrides;
    }

    public Map<ResourceLocation, Long> getOverrides() { return overrides; }

    public static void encode(SyncCustomEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.overrides.size());
        msg.overrides.forEach((rl, val) -> {
            buf.writeResourceLocation(rl);
            buf.writeLong(val);
        });
    }

    public static SyncCustomEMCMessage decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readResourceLocation(), buf.readLong());
        }
        return new SyncCustomEMCMessage(map);
    }

    public static void handle(SyncCustomEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientMessageHandler.handleSyncCustomEMC(msg)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
