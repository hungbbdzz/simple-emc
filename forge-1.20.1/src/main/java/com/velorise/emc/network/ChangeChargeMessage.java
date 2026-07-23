package com.velorise.emc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChangeChargeMessage {
    private final boolean decrease;

    public ChangeChargeMessage(boolean decrease) {
        this.decrease = decrease;
    }

    public boolean isDecrease() { return decrease; }

    public static void encode(ChangeChargeMessage msg, FriendlyByteBuf buf) { buf.writeBoolean(msg.decrease); }
    public static ChangeChargeMessage decode(FriendlyByteBuf buf) { return new ChangeChargeMessage(buf.readBoolean()); }

    public static void handle(ChangeChargeMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof com.velorise.emc.EchoboundSwordItem) {
                    boolean cone = com.velorise.emc.EchoboundSwordItem.toggleMode(player);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, cone ? 0.8F : 0.5F);
                    return;
                }
                if (com.velorise.emc.ImbuedEquipmentUtil.supportsMode(stack)) {
                    int current = com.velorise.emc.ImbuedEquipmentUtil.getMode(stack);
                    int maxMode = com.velorise.emc.CoreTierData.maximumMode(stack);
                    int next = com.velorise.emc.common.InteractionRuleProfile.stepLevel(current, 1, maxMode, msg.isDecrease());
                    if (next != current) {
                        com.velorise.emc.ImbuedEquipmentUtil.setMode(stack, next);
                        showMode(player, "Mode", next, maxMode);
                    }
                    return;
                }
                if (stack.getItem() instanceof com.velorise.emc.AlchemicalChargeable chargeable) {
                    int minCharge = chargeable.getMinCharge(stack);
                    int maxCharge = chargeable.getMaxCharge(stack);
                    int current = Math.max(minCharge, chargeable.getCharge(stack));
                    int next = com.velorise.emc.common.InteractionRuleProfile.stepLevel(current, minCharge, maxCharge, msg.isDecrease());
                    if (next != current) {
                        chargeable.setCharge(stack, next);
                        showMode(player, "Charge Level", next, maxCharge);
                    }
                    return;
                }
            }

        });
        ctx.get().setPacketHandled(true);
    }

    private static void showMode(net.minecraft.server.level.ServerPlayer player, String label, int value, int max) {
        float pitch = 0.5F + 0.125F * value;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, pitch);
        String size = switch (value) { case 2 -> "3×3"; case 3 -> "5×5"; case 4 -> "7×7"; default -> "1×1"; };
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "§b" + label + ": §e" + size + " §7(" + value + "/" + max + ")"), true);
    }
}
