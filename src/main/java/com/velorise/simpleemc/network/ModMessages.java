package com.velorise.simpleemc.network;
 
import com.velorise.simpleemc.SimpleEMC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
 
public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
 
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SimpleEMC.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
 
    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncPlayerEMCMessage.class, id++)
                .encoder(SyncPlayerEMCMessage::encode)
                .decoder(SyncPlayerEMCMessage::decode)
                .consumerMainThread(SyncPlayerEMCMessage::handle)
                .add();
 
        CHANNEL.messageBuilder(RequestWithdrawMessage.class, id++)
                .encoder(RequestWithdrawMessage::encode)
                .decoder(RequestWithdrawMessage::decode)
                .consumerMainThread(RequestWithdrawMessage::handle)
                .add();
 
        CHANNEL.messageBuilder(SyncCustomEMCMessage.class, id++)
                .encoder(SyncCustomEMCMessage::encode)
                .decoder(SyncCustomEMCMessage::decode)
                .consumerMainThread(SyncCustomEMCMessage::handle)
                .add();
 
        CHANNEL.messageBuilder(RequestUpdateEMCMessage.class, id++)
                .encoder(RequestUpdateEMCMessage::encode)
                .decoder(RequestUpdateEMCMessage::decode)
                .consumerMainThread(RequestUpdateEMCMessage::handle)
                .add();
 
        CHANNEL.messageBuilder(OpenConfigScreenMessage.class, id++)
                .encoder(OpenConfigScreenMessage::encode)
                .decoder(OpenConfigScreenMessage::decode)
                .consumerMainThread(OpenConfigScreenMessage::handle)
                .add();
 
        CHANNEL.messageBuilder(FillCraftingFromEMCMessage.class, id++)
                .encoder(FillCraftingFromEMCMessage::encode)
                .decoder(FillCraftingFromEMCMessage::decode)
                .consumerMainThread(FillCraftingFromEMCMessage::handle)
                .add();
    }
 
    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
 
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
