package com.velorise.simpleemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncPlayerEMCMessage {
    private final long emc;
    private final List<ResourceLocation> learned;

    public SyncPlayerEMCMessage(long emc, List<ResourceLocation> learned) {
        this.emc = emc;
        this.learned = learned;
    }

    public long emc() {
        return emc;
    }

    public List<ResourceLocation> learned() {
        return learned;
    }

    public static void encode(SyncPlayerEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.emc);
        buf.writeVarInt(msg.learned.size());
        for (ResourceLocation rl : msg.learned) {
            buf.writeResourceLocation(rl);
        }
    }

    public static SyncPlayerEMCMessage decode(FriendlyByteBuf buf) {
        long emc = buf.readLong();
        int size = buf.readVarInt();
        List<ResourceLocation> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readResourceLocation());
        }
        return new SyncPlayerEMCMessage(emc, list);
    }

    public static void handle(SyncPlayerEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMessageHandler.handleSyncPlayerEMC(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}

