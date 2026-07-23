package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenInventoryItemUiPayload(int inventorySlot) implements CustomPacketPayload {
    public static final Type<OpenInventoryItemUiPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "open_inventory_item_ui"));
    public static final StreamCodec<FriendlyByteBuf, OpenInventoryItemUiPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeVarInt(payload.inventorySlot),
        buffer -> new OpenInventoryItemUiPayload(buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
