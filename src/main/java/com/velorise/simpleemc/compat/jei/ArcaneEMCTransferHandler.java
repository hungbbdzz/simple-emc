package com.velorise.simpleemc.compat.jei;

import com.velorise.simpleemc.ArcaneTransmutationMenu;
import com.velorise.simpleemc.EMCRegistry;
import com.velorise.simpleemc.PlayerEMC;
import com.velorise.simpleemc.SimpleEMC;
import com.velorise.simpleemc.capability.PlayerEMCCapability;
import com.velorise.simpleemc.network.FillCraftingFromEMCMessage;
import com.velorise.simpleemc.network.ModMessages;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArcaneEMCTransferHandler implements IRecipeTransferHandler<ArcaneTransmutationMenu, CraftingRecipe> {

    private static final int SLOT_X_OFFSET = 1;
    private static final int SLOT_Y_OFFSET = 1;
    private static final int SLOT_SIZE = 18;

    private static final class MissingItemsError implements IRecipeTransferError {
        private final List<Integer> missingSlotIndices;

        MissingItemsError(List<Integer> missingSlotIndices) {
            this.missingSlotIndices = missingSlotIndices;
        }

        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY,
                               IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
            for (int slotIdx : missingSlotIndices) {
                int col = slotIdx % 3;
                int row = slotIdx / 3;
                int x = recipeX + SLOT_X_OFFSET + col * SLOT_SIZE;
                int y = recipeY + SLOT_Y_OFFSET + row * SLOT_SIZE;
                guiGraphics.fill(x, y, x + 16, y + 16, 0x99FF0000);
            }
        }
    }

    @Override
    public Class<? extends ArcaneTransmutationMenu> getContainerClass() {
        return ArcaneTransmutationMenu.class;
    }

    @Override
    public Optional<MenuType<ArcaneTransmutationMenu>> getMenuType() {
        return Optional.of(SimpleEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(
            ArcaneTransmutationMenu container,
            CraftingRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {

        PlayerEMC emcData = player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).orElse(null);
        if (emcData == null) return null;

        Inventory inventory = player.getInventory();
        List<IRecipeSlotView> inputSlots = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);

        Map<Item, Integer> inventoryAvailable = buildInventoryMap(inventory);
        for (int i = 0; i < 9; i++) {
            ItemStack craftStack = container.getCraftSlots().getItem(i);
            if (!craftStack.isEmpty()) {
                inventoryAvailable.merge(craftStack.getItem(), craftStack.getCount(), Integer::sum);
            }
        }

        List<Integer> slotsToFill = new ArrayList<>();
        List<ResourceLocation> itemsToFill = new ArrayList<>();

        List<Integer> emcSlotIndices = new ArrayList<>();
        long totalEmcNeeded = 0;

        List<Integer> missingSlots = new ArrayList<>();

        for (int slotIdx = 0; slotIdx < inputSlots.size() && slotIdx < 9; slotIdx++) {
            IRecipeSlotView slotView = inputSlots.get(slotIdx);

            List<Item> candidates = slotView.getItemStacks()
                .map(ItemStack::getItem)
                .distinct()
                .toList();

            if (candidates.isEmpty()) continue;

            Item itemFromInventory = null;
            for (Item candidate : candidates) {
                int available = inventoryAvailable.getOrDefault(candidate, 0);
                if (available > 0) {
                    itemFromInventory = candidate;
                    inventoryAvailable.put(candidate, available - 1);
                    break;
                }
            }
            if (itemFromInventory != null) {
                slotsToFill.add(slotIdx);
                itemsToFill.add(BuiltInRegistries.ITEM.getKey(itemFromInventory));
                continue;
            }

            Item itemFromEMC = null;
            for (Item candidate : candidates) {
                long cost = EMCRegistry.getEMC(candidate);
                if (cost > 0 && emcData.getLearnedItems().contains(candidate)) {
                    itemFromEMC = candidate;
                    totalEmcNeeded += cost;
                    break;
                }
            }
            if (itemFromEMC != null) {
                slotsToFill.add(slotIdx);
                itemsToFill.add(BuiltInRegistries.ITEM.getKey(itemFromEMC));
                emcSlotIndices.add(slotIdx);
                continue;
            }

            missingSlots.add(slotIdx);
        }

        if (!missingSlots.isEmpty()) {
            return new MissingItemsError(missingSlots);
        }

        if (totalEmcNeeded > emcData.getEmc()) {
            return new MissingItemsError(emcSlotIndices);
        }

        if (doTransfer && !slotsToFill.isEmpty()) {
            ModMessages.CHANNEL.sendToServer(new FillCraftingFromEMCMessage(slotsToFill, itemsToFill));
        }

        return null;
    }

    private Map<Item, Integer> buildInventoryMap(Inventory inventory) {
        Map<Item, Integer> map = new HashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                map.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return map;
    }
}
