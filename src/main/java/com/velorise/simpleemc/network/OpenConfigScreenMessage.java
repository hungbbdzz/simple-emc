package com.velorise.simpleemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenConfigScreenMessage {
    public OpenConfigScreenMessage() {}

    public static void encode(OpenConfigScreenMessage msg, FriendlyByteBuf buf) {
        // No data
    }

    public static OpenConfigScreenMessage decode(FriendlyByteBuf buf) {
        return new OpenConfigScreenMessage();
    }

    public static void handle(OpenConfigScreenMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientMessageHandler.handleOpenConfigScreen(msg)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
