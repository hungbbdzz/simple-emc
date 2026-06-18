package com.velorise.simpleemc.network;

import com.velorise.simpleemc.EMCRegistry;
import com.velorise.simpleemc.PlayerEMC;
import com.velorise.simpleemc.SimpleEMC;
import com.velorise.simpleemc.ArcaneTransmutationMenu;
import com.velorise.simpleemc.capability.PlayerEMCCapability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class ServerMessageHandler {

    public static void handleWithdraw(RequestWithdrawMessage msg, ServerPlayer player) {
        if (player == null) return;
        ResourceLocation rl = msg.getItemRL();
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item != null && item != Items.AIR) {
            player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
                long cost = EMCRegistry.getEMC(item);
                if (cost > 0 && data.getLearnedItems().contains(item) && data.getEmc() >= cost) {
                    int maxStack = item.getDefaultInstance().getMaxStackSize();
                    int requestedAmount = msg.isWithdrawStack() ? (int) Math.min(maxStack, data.getEmc() / cost) : 1;
                    if (requestedAmount > 0) {
                        ItemStack stack = new ItemStack(item, requestedAmount);
                        int countBefore = stack.getCount();
                        if (msg.isTargetGrid() && player.containerMenu instanceof ArcaneTransmutationMenu arcaneMenu) {
                            arcaneMenu.addCraftingStack(stack);
                        } else {
                            player.getInventory().add(stack);
                        }
                        int added = countBefore - stack.getCount();
                        if (added > 0) {
                            data.removeEmc(cost * added);
                            SimpleEMC.syncPlayerData(player);
                        }
                    }
                }
            });
        }
    }

    public static void handleFillCrafting(FillCraftingFromEMCMessage msg, ServerPlayer player) {
        if (player == null) return;
        if (!(player.containerMenu instanceof ArcaneTransmutationMenu arcaneMenu)) return;

        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(emcData -> {
            Inventory inventory = player.getInventory();
            boolean emcChanged = false;

            // Step 1: Clear the entire 3x3 grid
            for (int i = 0; i < 9; i++) {
                ItemStack craftStack = arcaneMenu.getCraftSlots().getItem(i);
                if (!craftStack.isEmpty()) {
                    if (!inventory.add(craftStack)) {
                        player.drop(craftStack, false);
                    }
                    arcaneMenu.getCraftSlots().setItem(i, ItemStack.EMPTY);
                }
            }

            // Step 2: Fill each requested crafting slot
            for (int i = 0; i < msg.getCraftSlots().size(); i++) {
                int craftSlot = msg.getCraftSlots().get(i);
                if (craftSlot < 0 || craftSlot >= 9) continue;

                Item item = BuiltInRegistries.ITEM.get(msg.getItemRLs().get(i));
                if (item == null || item == Items.AIR) continue;

                // Try inventory first
                boolean tookFromInventory = false;
                for (int invIdx = 0; invIdx < inventory.getContainerSize(); invIdx++) {
                    ItemStack invStack = inventory.getItem(invIdx);
                    if (!invStack.isEmpty() && invStack.is(item)) {
                        ItemStack toPlace = invStack.split(1);
                        arcaneMenu.getCraftSlots().setItem(craftSlot, toPlace);
                        if (invStack.isEmpty()) inventory.setItem(invIdx, ItemStack.EMPTY);
                        tookFromInventory = true;
                        break;
                    }
                }
                if (tookFromInventory) continue;

                // Try EMC
                long cost = EMCRegistry.getEMC(item);
                if (cost > 0 && emcData.getLearnedItems().contains(item) && emcData.getEmc() >= cost) {
                    emcData.removeEmc(cost);
                    arcaneMenu.getCraftSlots().setItem(craftSlot, new ItemStack(item, 1));
                    emcChanged = true;
                }
            }

            if (emcChanged) SimpleEMC.syncPlayerData(player);
            arcaneMenu.slotsChanged(arcaneMenu.getCraftSlots());
            player.containerMenu.broadcastChanges();
        });
    }

    public static void handleUpdateEMC(RequestUpdateEMCMessage msg, ServerPlayer player) {
        if (player == null) return;
        if (player.hasPermissions(2)) {
            Map<Item, Long> serverOverrides = new HashMap<>();
            msg.getOverrides().forEach((rl, val) -> {
                Item item = BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != Items.AIR) {
                    serverOverrides.put(item, val);
                }
            });

            EMCRegistry.saveCustomEMC(serverOverrides);

            EMCRegistry.reloadAndRecalculate(
                player.getServer().getRecipeManager(),
                player.getServer().registryAccess()
            );

            Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();

            for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                ModMessages.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> onlinePlayer),
                    new SyncCustomEMCMessage(fullOverrides)
                );
            }
        }
    }
}
