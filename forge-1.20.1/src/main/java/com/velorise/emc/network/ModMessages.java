package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "13";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VeloriseEMC.MODID, "main"),
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

        CHANNEL.messageBuilder(ChangeChargeMessage.class, id++)
                .encoder(ChangeChargeMessage::encode)
                .decoder(ChangeChargeMessage::decode)
                .consumerMainThread(ChangeChargeMessage::handle)
                .add();

        CHANNEL.messageBuilder(UpdateHubSettingsMessage.class, id++)
                .encoder(UpdateHubSettingsMessage::encode)
                .decoder(UpdateHubSettingsMessage::decode)
                .consumerMainThread(UpdateHubSettingsMessage::handle)
                .add();

        CHANNEL.messageBuilder(JukeboxAudioMessage.class, id++)
                .encoder(JukeboxAudioMessage::encode)
                .decoder(JukeboxAudioMessage::decode)
                .consumerMainThread(JukeboxAudioMessage::handle)
                .add();

        CHANNEL.messageBuilder(ElytraBoostMessage.class, id++)
                .encoder(ElytraBoostMessage::encode)
                .decoder(ElytraBoostMessage::decode)
                .consumerMainThread(ElytraBoostMessage::handle)
                .add();

        CHANNEL.messageBuilder(RepairEquipmentMessage.class, id++)
                .encoder(RepairEquipmentMessage::encode)
                .decoder(RepairEquipmentMessage::decode)
                .consumerMainThread(RepairEquipmentMessage::handle)
                .add();

        CHANNEL.messageBuilder(AquaticBreathMessage.class, id++)
                .encoder(AquaticBreathMessage::encode)
                .decoder(AquaticBreathMessage::decode)
                .consumerMainThread(AquaticBreathMessage::handle)
                .add();

        CHANNEL.messageBuilder(OpenInventoryItemUiMessage.class, id++)
                .encoder(OpenInventoryItemUiMessage::encode)
                .decoder(OpenInventoryItemUiMessage::decode)
                .consumerMainThread(OpenInventoryItemUiMessage::handle)
                .add();

        CHANNEL.messageBuilder(MiningUseStateMessage.class, id++)
                .encoder(MiningUseStateMessage::encode)
                .decoder(MiningUseStateMessage::decode)
                .consumerMainThread(MiningUseStateMessage::handle)
                .add();


        CHANNEL.messageBuilder(SyncGameplayConfigMessage.class, id++)
                .encoder(SyncGameplayConfigMessage::encode)
                .decoder(SyncGameplayConfigMessage::decode)
                .consumerMainThread(SyncGameplayConfigMessage::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
