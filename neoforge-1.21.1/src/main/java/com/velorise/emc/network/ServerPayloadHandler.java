package com.velorise.emc.network;

import com.velorise.emc.AdvancementHooks;
import com.velorise.emc.EMCRegistry;
import com.velorise.emc.PlayerEMC;
import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.ArcaneTransmutationMenu;
import com.velorise.emc.TransmutationMenu;
import com.velorise.emc.TransmutationTaxService;
import com.velorise.emc.common.EmcPolicyOverride;
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
                    PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
                    long unitCost = EMCRegistry.getEMC(item);
                    if (unitCost <= 0L || !EMCRegistry.canMaterialize(item)
                        || !data.getLearnedItems().contains(item)) return;

                    long now = TransmutationTaxService.nowMillis();
                    int maxStack = item.getDefaultInstance().getMaxStackSize();
                    int requestedAmount = payload.withdrawStack()
                        ? TransmutationTaxService.maxAffordableCount(data, unitCost, maxStack, now)
                        : (TransmutationTaxService.quote(data, unitCost, 1, now).totalCost() <= data.getEmc() ? 1 : 0);
                    if (requestedAmount <= 0) return;

                    ItemStack stack = new ItemStack(item, requestedAmount);
                    int countBefore = stack.getCount();
                    if (payload.targetGrid() && player.containerMenu instanceof ArcaneTransmutationMenu arcaneMenu) {
                        arcaneMenu.addCraftingStack(stack);
                    } else {
                        player.getInventory().add(stack);
                    }

                    int added = countBefore - stack.getCount();
                    if (added <= 0) return;

                    long fundBefore = data.getEmc();
                    TransmutationTaxService.ChargeResult charged =
                        TransmutationTaxService.charge(data, unitCost, added, now);
                    if (charged.success()) {
                        AdvancementHooks.onMaterialize(player, item, added, data);
                        AdvancementHooks.onSpent(player, data, fundBefore);
                        player.setData(VeloriseEMC.PLAYER_EMC.get(), data);
                        VeloriseEMC.syncPlayerData(player);
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

            PlayerEMC emcData = player.getData(VeloriseEMC.PLAYER_EMC.get());
            Inventory inventory = player.getInventory();
            boolean emcChanged = false;
            boolean[] filledSlots = new boolean[9];
            long now = TransmutationTaxService.nowMillis();

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

            // ── Step 2: Fill each requested crafting slot; truncate malformed list pairs ──
            int requestCount = Math.min(payload.craftSlots().size(), payload.itemRLs().size());
            for (int i = 0; i < requestCount; i++) {
                int craftSlot = payload.craftSlots().get(i);
                if (craftSlot < 0 || craftSlot >= 9 || filledSlots[craftSlot]) continue;
                filledSlots[craftSlot] = true;

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

            // Sync EMC if changed and notify clients of crafting grid update
            if (emcChanged) VeloriseEMC.syncPlayerData(player);
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
                    java.util.Map<Item, EmcPolicyOverride> policyOverrides = new java.util.HashMap<>();
                    payload.policyOverrides().forEach((rl, policy) -> {
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

                    boolean modeChanged = EMCRegistry.getConfiguredValueMode() != payload.valueMode();
                    EMCRegistry.setConfiguredValueMode(payload.valueMode());

                    // Rebuild profile defaults first so edited values are compared against the selected mode.
                    if (modeChanged && !serverOverrides.isEmpty()) {
                        EMCRegistry.reloadAndRecalculate(
                            player.getServer().getRecipeManager(),
                            player.getServer().registryAccess()
                        );
                    }

                    if (payload.replaceAll()) EMCRegistry.replaceCustomEMC(serverOverrides);
                    else if (!serverOverrides.isEmpty()) EMCRegistry.saveCustomEMC(serverOverrides);
                    EMCRegistry.replaceItemPolicies(policyOverrides);

                    EMCRegistry.reloadAndRecalculate(
                        player.getServer().getRecipeManager(),
                        player.getServer().registryAccess()
                    );

                    java.util.Map<ResourceLocation, Long> fullOverrides = EMCRegistry.getFullCustomOverrides();
                    java.util.Map<ResourceLocation, EmcPolicyOverride> fullPolicies = EMCRegistry.getFullPolicyOverrides();

                    for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            onlinePlayer,
                            new SyncCustomEMCPayload(fullOverrides, EMCRegistry.getConfiguredValueMode(), fullPolicies)
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
        });
    }

    public static void handleRepairEquipment(final RepairEquipmentPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                com.velorise.emc.ManualRepairService.repair(player, payload.allEquipped());
            }
        });
    }

    public static void handleAquaticBreath(final AquaticBreathPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                com.velorise.emc.VanillaItemUpgradeEvents.activateAquaticBreathing(player);
            }
        });
    }


    public static void handleElytraBoost(final ElytraBoostPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isFallFlying()) return;
            ItemStack elytra = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            if (!com.velorise.emc.ImbuedEquipmentUtil.isElytra(elytra)) return;
            long now = player.level().getGameTime();
            if (now < com.velorise.emc.ImbuedEquipmentUtil.getActionCooldownTick(elytra)) return;

            long cost = com.velorise.emc.EMCRegistry.getUnitEMC(new ItemStack(Items.FIREWORK_ROCKET));
            if (cost <= 0L || !com.velorise.emc.EmcTransactionService.spend(player, cost)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§cNot enough EMC for Elytra Boost. Need §e" + cost + " EMC"), true);
                return;
            }

            int mode = com.velorise.emc.ImbuedEquipmentUtil.getMode(elytra);
            double impulse = com.velorise.emc.common.UtilityItemProfile.elytraBoostImpulse(mode);
            net.minecraft.world.phys.Vec3 look = player.getLookAngle();
            player.setDeltaMovement(player.getDeltaMovement().add(
                look.x * impulse, look.y * impulse * 0.65D + 0.05D, look.z * impulse));
            player.hurtMarked = true;
            com.velorise.emc.ImbuedEquipmentUtil.setActionCooldownTick(elytra, now + com.velorise.emc.common.UtilityItemProfile.elytraBoostCooldownTicks());
            player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F + mode * 0.08F);
        });
    }

    public static void handleOpenInventoryItemUi(final OpenInventoryItemUiPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            int slot = payload.inventorySlot();
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;
            ItemStack stack = player.getInventory().getItem(slot);
            if (com.velorise.emc.InventoryUiFallback.canOpen(stack)) {
                com.velorise.emc.InventoryUiFallback.open(player, stack);
            }
        });
    }

    public static void handleMiningUseState(final MiningUseStatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ItemStack mainHand = player.getMainHandItem();
            if (payload.active() && com.velorise.emc.ImbuedEquipmentUtil.usesMiningActivation(mainHand)) {
                long now = player.level().getGameTime();
                if (now >= com.velorise.emc.ImbuedEquipmentUtil.getActionCooldownTick(mainHand)) {
                    com.velorise.emc.ImbuedEquipmentUtil.setActive(mainHand, true);
                    com.velorise.emc.ImbuedEquipmentUtil.setMiningActiveUntil(mainHand, now + 30L);
                }
                return;
            }

            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (!com.velorise.emc.ImbuedEquipmentUtil.usesMiningActivation(stack)) continue;
                com.velorise.emc.ImbuedEquipmentUtil.setActive(stack, false);
                com.velorise.emc.ImbuedEquipmentUtil.setMiningActiveUntil(stack, 0L);
            }
        });
    }

    public static void handleChangeCharge(final ChangeChargePayload payload, final net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
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
                    int next = com.velorise.emc.common.InteractionRuleProfile.stepLevel(current, 1, maxMode, payload.decrease());
                    if (next != current) {
                        com.velorise.emc.ImbuedEquipmentUtil.setMode(stack, next);
                        showMode(player, "Mode", next, maxMode);
                    }
                    return;
                }
                if (stack.getItem() instanceof com.velorise.emc.AlchemicalChargeable chargeable) {
                    int maxCharge = chargeable.getMaxCharge(stack);
                    int current = chargeable.getCharge(stack);
                    int next = com.velorise.emc.common.InteractionRuleProfile.stepLevel(
                        current, 0, maxCharge, payload.decrease());
                    if (next != current) {
                        chargeable.setCharge(stack, next);
                        showMode(player, "Charge Level", next, maxCharge);
                    }
                    return;
                }
            }

        });
    }

    private static void showMode(ServerPlayer player, String label, int value, int max) {
        float pitch = 0.5F + 0.125F * value;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, pitch);
        String size = switch (value) { case 2 -> "3×3"; case 3 -> "5×5"; case 4 -> "7×7"; default -> "1×1"; };
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "§b" + label + ": §e" + size + " §7(" + value + "/" + max + ")"), true);
    }

    public static void handleUpdateHubSettings(final UpdateHubSettingsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
            data.setSourceMode(com.velorise.emc.common.EmcSourceMode.byId(payload.sourceMode()));
            data.setSafetyThreshold(Math.max(0L, payload.safetyThreshold()));
            data.setHudEnabled(payload.hudEnabled());
            player.setData(VeloriseEMC.PLAYER_EMC.get(), data);
            VeloriseEMC.syncPlayerData(player);
        });
    }

}
