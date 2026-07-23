package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.capability.PlayerEMCCapability;
import com.velorise.emc.common.EmcSourceMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateHubSettingsMessage(int sourceMode, long safetyThreshold, boolean hudEnabled) {
    public static void encode(UpdateHubSettingsMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.sourceMode());
        buf.writeLong(msg.safetyThreshold());
        buf.writeBoolean(msg.hudEnabled());
    }

    public static UpdateHubSettingsMessage decode(FriendlyByteBuf buf) {
        return new UpdateHubSettingsMessage(buf.readVarInt(), buf.readLong(), buf.readBoolean());
    }

    public static void handle(UpdateHubSettingsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
                data.setSourceMode(EmcSourceMode.byId(msg.sourceMode()));
                data.setSafetyThreshold(Math.max(0L, msg.safetyThreshold()));
                data.setHudEnabled(msg.hudEnabled());
            });
            VeloriseEMC.syncPlayerData(player);
        });
        context.setPacketHandled(true);
    }
}
