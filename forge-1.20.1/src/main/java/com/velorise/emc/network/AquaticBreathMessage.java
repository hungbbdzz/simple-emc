package com.velorise.emc.network;

import com.velorise.emc.VanillaItemUpgradeEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AquaticBreathMessage() {
    public static void encode(AquaticBreathMessage message, FriendlyByteBuf buffer) {}
    public static AquaticBreathMessage decode(FriendlyByteBuf buffer) { return new AquaticBreathMessage(); }

    public static void handle(AquaticBreathMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) VanillaItemUpgradeEvents.activateAquaticBreathing(player);
        });
        context.setPacketHandled(true);
    }
}
