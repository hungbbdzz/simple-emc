package com.velorise.emc.network;

import com.velorise.emc.EMCRegistry;
import com.velorise.emc.EmcTransactionService;
import com.velorise.emc.ImbuedEquipmentUtil;
import com.velorise.emc.common.UtilityItemProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ElytraBoostMessage() {
    public static void encode(ElytraBoostMessage message, FriendlyByteBuf buffer) {}
    public static ElytraBoostMessage decode(FriendlyByteBuf buffer) { return new ElytraBoostMessage(); }

    public static void handle(ElytraBoostMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.isFallFlying()) return;
            ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!ImbuedEquipmentUtil.isElytra(elytra)) return;
            long now = player.level().getGameTime();
            if (now < ImbuedEquipmentUtil.getActionCooldownTick(elytra)) return;

            long cost = EMCRegistry.getUnitEMC(new ItemStack(Items.FIREWORK_ROCKET));
            if (cost <= 0L || !EmcTransactionService.spend(player, cost)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§cNot enough EMC for Elytra Boost. Need §e" + cost + " EMC"), true);
                return;
            }

            int mode = ImbuedEquipmentUtil.getMode(elytra);
            double impulse = UtilityItemProfile.elytraBoostImpulse(mode);
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(player.getDeltaMovement().add(
                look.x * impulse, look.y * impulse * 0.65D + 0.05D, look.z * impulse));
            player.hurtMarked = true;
            ImbuedEquipmentUtil.setActionCooldownTick(elytra, now + UtilityItemProfile.elytraBoostCooldownTicks());
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 0.8F, 1.0F + mode * 0.08F);
        });
        context.setPacketHandled(true);
    }
}
