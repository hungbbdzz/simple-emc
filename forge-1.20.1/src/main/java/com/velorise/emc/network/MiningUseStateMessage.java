package com.velorise.emc.network;

import com.velorise.emc.ImbuedEquipmentUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client input heartbeat for context-activated Imbued mining tools.
 *
 * <p>The server still validates the held stack. A short expiry prevents a tool from
 * remaining powered if the client closes a screen, changes item, or disconnects mid-input.</p>
 */
public record MiningUseStateMessage(boolean active) {
    public static void encode(MiningUseStateMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
    }

    public static MiningUseStateMessage decode(FriendlyByteBuf buffer) {
        return new MiningUseStateMessage(buffer.readBoolean());
    }

    public static void handle(MiningUseStateMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            if (message.active && ImbuedEquipmentUtil.usesMiningActivation(mainHand)) {
                long now = player.level().getGameTime();
                if (now >= ImbuedEquipmentUtil.getActionCooldownTick(mainHand)) {
                    ImbuedEquipmentUtil.setActive(mainHand, true);
                    ImbuedEquipmentUtil.setMiningActiveUntil(mainHand, now + 30L);
                }
                return;
            }

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (!ImbuedEquipmentUtil.usesMiningActivation(stack)) continue;
                ImbuedEquipmentUtil.setActive(stack, false);
                ImbuedEquipmentUtil.setMiningActiveUntil(stack, 0L);
            }
        });
        context.setPacketHandled(true);
    }
}
