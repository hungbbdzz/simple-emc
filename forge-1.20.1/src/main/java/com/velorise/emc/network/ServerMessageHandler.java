package com.velorise.emc.network;

import com.velorise.emc.AdvancementHooks;
import com.velorise.emc.EMCRegistry;
import com.velorise.emc.PlayerEMC;
import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.ArcaneTransmutationMenu;
import com.velorise.emc.TransmutationMenu;
import com.velorise.emc.TransmutationTaxService;
import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.capability.PlayerEMCCapability;
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
                long unitCost = EMCRegistry.getEMC(item);
                if (unitCost <= 0L || !EMCRegistry.canMaterialize(item)
                    || !data.getLearnedItems().contains(item)) return;

                long now = TransmutationTaxService.nowMillis();
                int maxStack = item.getDefaultInstance().getMaxStackSize();
                int requestedAmount = msg.isWithdrawStack()
                    ? TransmutationTaxService.maxAffordableCount(data, unitCost, maxStack, now)
                    : (TransmutationTaxService.quote(data, unitCost, 1, now).totalCost() <= data.getEmc() ? 1 : 0);
                if (requestedAmount <= 0) return;

                ItemStack stack = new ItemStack(item, requestedAmount);
                int countBefore = stack.getCount();
                if (msg.isTargetGrid() && player.containerMenu instanceof ArcaneTransmutationMenu arcaneMenu) {
                    arcaneMenu.addCraftingStack(stack);
                } else {
                    player.getInventory().add(stack);
                }

                int added = countBefore - stack.getCount();
                if (added <= 0) return;

                // The affordable amount was quoted from the same data on the same server thread,
                // so charging the exact inserted count must succeed unless another handler breaks
                // the server-thread transaction invariant.
                long fundBefore = data.getEmc();
                TransmutationTaxService.ChargeResult charged =
                    TransmutationTaxService.charge(data, unitCost, added, now);
                if (charged.success()) {
                    AdvancementHooks.onMaterialize(player, item, added, data);
                    AdvancementHooks.onSpent(player, data, fundBefore);
                    VeloriseEMC.syncPlayerData(player);
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
            boolean[] filledSlots = new boolean[9];
            long now = TransmutationTaxService.nowMillis();

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

            // Step 2: Fill each requested crafting slot. Treat malformed list pairs as truncated.
            int requestCount = Math.min(msg.getCraftSlots().size(), msg.getItemRLs().size());
            for (int i = 0; i < requestCount; i++) {
                int craftSlot = msg.getCraftSlots().get(i);
                if (craftSlot < 0 || craftSlot >= 9 || filledSlots[craftSlot]) continue;
                filledSlots[craftSlot] = true;

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
                long unitCost = EMCRegistry.getEMC(item);
                if (unitCost > 0L && EMCRegistry.canMaterialize(item)
                    && emcData.getLearnedItems().contains(item)) {
                    long fundBefore = emcData.getEmc();
                    TransmutationTaxService.ChargeResult charged =
                        TransmutationTaxService.charge(emcData, unitCost, 1, now);
                    if (charged.success()) {
                        arcaneMenu.getCraftSlots().setItem(craftSlot, new ItemStack(item, 1));
                        AdvancementHooks.onMaterialize(player, item, 1, emcData);
                        AdvancementHooks.onSpent(player, emcData, fundBefore);
                        emcChanged = true;
                    }
                }
            }

            if (emcChanged) VeloriseEMC.syncPlayerData(player);
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
            Map<Item, EmcPolicyOverride> policyOverrides = new HashMap<>();
            msg.getPolicyOverrides().forEach((rl, policy) -> {
                Item item = BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != Items.AIR && policy != null && !EMCRegistry.isHardBlocked(item)) {
                    policyOverrides.put(item, policy);
                }
            });

            for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                if (onlinePlayer.containerMenu instanceof TransmutationMenu
                    || onlinePlayer.containerMenu instanceof ArcaneTransmutationMenu) {
                    onlinePlayer.closeContainer();
                }
            }

            boolean modeChanged = EMCRegistry.getConfiguredValueMode() != msg.getValueMode();
            EMCRegistry.setConfiguredValueMode(msg.getValueMode());

            // Rebuild profile defaults first so edited values are compared against the selected mode.
            if (modeChanged && !serverOverrides.isEmpty()) {
                EMCRegistry.reloadAndRecalculate(
                    player.getServer().getRecipeManager(),
                    player.getServer().registryAccess()
                );
            }

            if (msg.isReplaceAll()) EMCRegistry.replaceCustomEMC(serverOverrides);
            else if (!serverOverrides.isEmpty()) EMCRegistry.saveCustomEMC(serverOverrides);
            EMCRegistry.replaceItemPolicies(policyOverrides);

            EMCRegistry.reloadAndRecalculate(
                player.getServer().getRecipeManager(),
                player.getServer().registryAccess()
            );

            Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
            Map<ResourceLocation, EmcPolicyOverride> fullPolicies = EMCRegistry.getFullPolicyOverrides();

            for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                ModMessages.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> onlinePlayer),
                    new SyncCustomEMCMessage(fullOverrides, EMCRegistry.getConfiguredValueMode(), fullPolicies)
                );
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Velorise EMC settings applied: " + EMCRegistry.getConfiguredValueMode().displayName()
                    + " | " + fullPolicies.size() + " custom item rules"), false);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Operator permission is required to change Velorise EMC settings."), false);
        }
    }
}
