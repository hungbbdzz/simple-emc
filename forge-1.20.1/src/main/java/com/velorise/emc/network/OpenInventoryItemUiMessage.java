package com.velorise.emc.network;

import com.velorise.emc.InventoryUiFallback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenInventoryItemUiMessage(int inventorySlot) {
    public static void encode(OpenInventoryItemUiMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.inventorySlot);
    }
    public static OpenInventoryItemUiMessage decode(FriendlyByteBuf buffer) {
        return new OpenInventoryItemUiMessage(buffer.readVarInt());
    }
    public static void handle(OpenInventoryItemUiMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            int slot = message.inventorySlot;
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;
            var stack = player.getInventory().getItem(slot);
            if (InventoryUiFallback.canOpen(stack)) InventoryUiFallback.open(player, stack);
        });
        context.setPacketHandled(true);
    }
}
