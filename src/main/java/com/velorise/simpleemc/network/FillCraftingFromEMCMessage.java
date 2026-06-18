package com.velorise.simpleemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sent by the client (JEI transfer handler) to the server.
 * Each (craftSlots[i], itemRLs[i]) pair means:
 *   "Please place 1 of itemRLs[i] into crafting grid slot craftSlots[i]."
 * The server checks inventory first, then EMC, in that order.
 */
public class FillCraftingFromEMCMessage {
    private final List<Integer> craftSlots;
    private final List<ResourceLocation> itemRLs;

    public FillCraftingFromEMCMessage(List<Integer> craftSlots, List<ResourceLocation> itemRLs) {
        this.craftSlots = craftSlots;
        this.itemRLs = itemRLs;
    }

    public List<Integer> getCraftSlots() { return craftSlots; }
    public List<ResourceLocation> getItemRLs() { return itemRLs; }

    public static void encode(FillCraftingFromEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.craftSlots.size());
        for (int i = 0; i < msg.craftSlots.size(); i++) {
            buf.writeInt(msg.craftSlots.get(i));
            buf.writeResourceLocation(msg.itemRLs.get(i));
        }
    }

    public static FillCraftingFromEMCMessage decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Integer> slots = new ArrayList<>(size);
        List<ResourceLocation> rls = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(buf.readInt());
            rls.add(buf.readResourceLocation());
        }
        return new FillCraftingFromEMCMessage(slots, rls);
    }

    public static void handle(FillCraftingFromEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ServerMessageHandler.handleFillCrafting(msg, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
