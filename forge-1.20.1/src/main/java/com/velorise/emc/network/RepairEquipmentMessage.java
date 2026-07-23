package com.velorise.emc.network;

import com.velorise.emc.ManualRepairService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RepairEquipmentMessage(boolean allEquipped) {
    public static void encode(RepairEquipmentMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.allEquipped());
    }

    public static RepairEquipmentMessage decode(FriendlyByteBuf buffer) {
        return new RepairEquipmentMessage(buffer.readBoolean());
    }

    public static void handle(RepairEquipmentMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) ManualRepairService.repair(player, message.allEquipped());
        });
        context.setPacketHandled(true);
    }
}
