package com.velorise.simpleemc.network;

import com.velorise.simpleemc.EMCRegistry;
import com.velorise.simpleemc.PlayerEMC;
import com.velorise.simpleemc.SimpleEMC;
import com.velorise.simpleemc.ArcaneTransmutationMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    /** Handle item withdrawal from EMC panel (single item, optionally to crafting grid). */
    public static void handle(final RequestWithdrawPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ResourceLocation rl = payload.itemRL();
                Item item = BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != Items.AIR) {
                    PlayerEMC data = player.getData(SimpleEMC.PLAYER_EMC.get());
                    long cost = EMCRegistry.getEMC(item);
                    if (cost > 0 && data.getLearnedItems().contains(item) && data.getEmc() >= cost) {
                        int maxStack = item.getDefaultInstance().getMaxStackSize();
                        int requestedAmount = payload.withdrawStack() ? (int) Math.min(maxStack, data.getEmc() / cost) : 1;
                        if (requestedAmount > 0) {
                            ItemStack stack = new ItemStack(item, requestedAmount);
                            int countBefore = stack.getCount();
                            if (payload.targetGrid() && player.containerMenu instanceof ArcaneTransmutationMenu arcaneMenu) {
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
                }
            }
        });
    }

    /**
     * Handle JEI recipe transfer fill request.
     * Step 1: Clear ALL crafting slots and return items to inventory (allow recipe switching).
     * Step 2: For each (craftSlot, itemRL) pair, take from inventory first, then EMC.
     */
    public static void handleFillCrafting(final FillCraftingFromEMCPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ArcaneTransmutationMenu arcaneMenu)) return;

            PlayerEMC emcData = player.getData(SimpleEMC.PLAYER_EMC.get());
            Inventory inventory = player.getInventory();
            boolean emcChanged = false;

            // ── Step 1: Clear the entire 3x3 crafting grid → return items to inventory ──
            for (int i = 0; i < 9; i++) {
                ItemStack craftStack = arcaneMenu.getCraftSlots().getItem(i);
                if (!craftStack.isEmpty()) {
                    if (!inventory.add(craftStack)) {
                        // Inventory full: drop on ground
                        player.drop(craftStack, false);
                    }
                    arcaneMenu.getCraftSlots().setItem(i, ItemStack.EMPTY);
                }
            }

            // ── Step 2: Fill each requested crafting slot ──
            for (int i = 0; i < payload.craftSlots().size(); i++) {
                int craftSlot = payload.craftSlots().get(i);
                if (craftSlot < 0 || craftSlot >= 9) continue;

                Item item = BuiltInRegistries.ITEM.get(payload.itemRLs().get(i));
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

            // Sync EMC if changed and notify clients of crafting grid update
            if (emcChanged) SimpleEMC.syncPlayerData(player);
            arcaneMenu.slotsChanged(arcaneMenu.getCraftSlots());
            player.containerMenu.broadcastChanges();
        });
    }

    public static void handleRequestUpdateEMC(final RequestUpdateEMCPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.hasPermissions(2)) {
                    java.util.Map<Item, Long> serverOverrides = new java.util.HashMap<>();
                    payload.overrides().forEach((rl, val) -> {
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
                    
                    java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
                    
                    for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            onlinePlayer,
                            new SyncCustomEMCPayload(fullOverrides)
                        );
                    }
                }
            }
        });
    }
}

