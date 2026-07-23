package com.velorise.emc;

import com.velorise.emc.common.CustomUtilityProfile;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** Unbreakable anvil with no Too Expensive cap; EMC pays only missing XP levels. */
public class AlchemicalAnvilMenu extends AnvilMenu {
    private final Player player;
    private final int anvilLevel;

    public AlchemicalAnvilMenu(int containerId, Inventory inventory, ContainerLevelAccess access, int anvilLevel) {
        super(containerId, inventory, access);
        this.player = inventory.player;
        this.anvilLevel = Math.max(1, Math.min(4, anvilLevel));
        player.getPersistentData().putBoolean(VanillaMachineRegistry.ANVIL_FLAG, true);
    }

    public int anvilLevel() {
        return anvilLevel;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
            level.getBlockState(pos).getBlock() instanceof AlchemicalAnvilBlock
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            true);
    }

    @Override
    public boolean mayPickup(Player player, boolean hasStack) {
        if (!hasStack) return false;
        int levelCost = getCost();
        if (levelCost <= 0) return false;
        int missing = Math.max(0, levelCost - player.experienceLevel);
        if (missing == 0) return true;
        // The client does not own authoritative EMC state. Allow the click locally;
        // the server repeats this check and either spends EMC or rejects the take.
        if (player.level().isClientSide) return true;
        return player instanceof ServerPlayer serverPlayer
            && EmcTransactionService.canSpend(serverPlayer, missing * CustomUtilityProfile.alchemicalAnvilEmcPerMissingLevel());
    }

    @Override
    protected void onTake(Player player, net.minecraft.world.item.ItemStack stack) {
        int levelCost = getCost();
        int paidWithLevels = Math.min(levelCost, player.experienceLevel);
        int missing = Math.max(0, levelCost - paidWithLevels);
        if (missing > 0) {
            if (!(player instanceof ServerPlayer serverPlayer)
                || !EmcTransactionService.spend(serverPlayer, missing * CustomUtilityProfile.alchemicalAnvilEmcPerMissingLevel())) return;
        }
        if (!player.getAbilities().instabuild && paidWithLevels > 0) player.giveExperienceLevels(-paidWithLevels);
        inputSlots.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
        if (repairItemCountCost > 0) {
            net.minecraft.world.item.ItemStack material = inputSlots.getItem(1);
            if (!material.isEmpty() && material.getCount() > repairItemCountCost) {
                material.shrink(repairItemCountCost);
                inputSlots.setItem(1, material);
            } else inputSlots.setItem(1, net.minecraft.world.item.ItemStack.EMPTY);
        } else inputSlots.setItem(1, net.minecraft.world.item.ItemStack.EMPTY);
        access.execute((level, pos) -> level.levelEvent(1030, pos, 0));
        setMaximumCost(0);
    }

    @Override public void createResult() {
        super.createResult();
        if (!resultSlots.getItem(0).isEmpty() && getCost() >= 40) setMaximumCost(Math.max(1, getCost()));
    }
}
