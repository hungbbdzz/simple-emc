package com.velorise.simpleemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent by the client (JEI transfer handler) to the server.
 * Each (craftSlots[i], itemRLs[i]) pair means:
 *   "Please place 1 of itemRLs[i] into crafting grid slot craftSlots[i]."
 * The server checks inventory first, then EMC, in that order.
 */
public record FillCraftingFromEMCPayload(
    List<Integer> craftSlots,
    List<ResourceLocation> itemRLs
) implements CustomPacketPayload {

    public static final Type<FillCraftingFromEMCPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("simpleemc", "fill_crafting_emc"));

    public static final StreamCodec<FriendlyByteBuf, FillCraftingFromEMCPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeInt(payload.craftSlots().size());
            for (int i = 0; i < payload.craftSlots().size(); i++) {
                buf.writeInt(payload.craftSlots().get(i));
                buf.writeResourceLocation(payload.itemRLs().get(i));
            }
        },
        buf -> {
            int size = buf.readInt();
            List<Integer> slots = new ArrayList<>(size);
            List<ResourceLocation> rls = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                slots.add(buf.readInt());
                rls.add(buf.readResourceLocation());
            }
            return new FillCraftingFromEMCPayload(slots, rls);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
