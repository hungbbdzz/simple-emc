package com.velorise.simpleemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestWithdrawMessage {
    private final ResourceLocation itemRL;
    private final boolean withdrawStack;
    private final boolean targetGrid;

    public RequestWithdrawMessage(ResourceLocation itemRL, boolean withdrawStack, boolean targetGrid) {
        this.itemRL = itemRL;
        this.withdrawStack = withdrawStack;
        this.targetGrid = targetGrid;
    }

    public ResourceLocation getItemRL() { return itemRL; }
    public boolean isWithdrawStack() { return withdrawStack; }
    public boolean isTargetGrid() { return targetGrid; }

    public static void encode(RequestWithdrawMessage msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.itemRL);
        buf.writeBoolean(msg.withdrawStack);
        buf.writeBoolean(msg.targetGrid);
    }

    public static RequestWithdrawMessage decode(FriendlyByteBuf buf) {
        return new RequestWithdrawMessage(buf.readResourceLocation(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(RequestWithdrawMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ServerMessageHandler.handleWithdraw(msg, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
