package com.velorise.simpleemc.compat.jei;

import com.velorise.simpleemc.ArcaneTransmutationMenu;
import com.velorise.simpleemc.EMCRegistry;
import com.velorise.simpleemc.PlayerEMC;
import com.velorise.simpleemc.SimpleEMC;
import com.velorise.simpleemc.network.FillCraftingFromEMCPayload;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Custom JEI Recipe Transfer Handler for ArcaneTransmutationMenu.
 *
 * Features:
 * - Always clears the crafting grid first so switching recipes works cleanly.
 * - Items already in the grid are counted as "available in inventory" (they'll be returned server-side).
 * - Items not in inventory are pulled from EMC if the player has learned them and has enough balance.
 * - Missing/unaffordable slots are highlighted in RED in the JEI recipe overlay.
 */
public class ArcaneEMCTransferHandler implements IRecipeTransferHandler<ArcaneTransmutationMenu, RecipeHolder<CraftingRecipe>> {

    // Standard JEI crafting recipe category layout:
    // Input slots start at (1, 1) relative to recipe origin, 18px apart.
    private static final int SLOT_X_OFFSET = 1;
    private static final int SLOT_Y_OFFSET = 1;
    private static final int SLOT_SIZE = 18;

    /**
     * Error implementation that draws a red overlay on specific recipe input slots.
     * Used to highlight which slots are missing items or which need more EMC.
     */
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
                // Semi-transparent red overlay (same colour JEI uses for missing items)
                guiGraphics.fill(x, y, x + 16, y + 16, 0x99FF0000);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // IRecipeTransferHandler implementation
    // ─────────────────────────────────────────────────────────────

    @Override
    public Class<? extends ArcaneTransmutationMenu> getContainerClass() {
        return ArcaneTransmutationMenu.class;
    }

    @Override
    public Optional<MenuType<ArcaneTransmutationMenu>> getMenuType() {
        return Optional.of(SimpleEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    /**
     * @return null → success (green "+" button, transfer proceeds)
     *         non-null → error with optional red slot overlay
     */
    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(
            ArcaneTransmutationMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {

        PlayerEMC emcData = player.getData(SimpleEMC.PLAYER_EMC.get());
        Inventory inventory = player.getInventory();

        // JEI gives us INPUT slot views in crafting grid order (0–8 positions)
        List<IRecipeSlotView> inputSlots = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);

        // Build virtual inventory: real inventory + items currently in crafting grid
        // (the server will clear the grid first and return those items before filling)
        Map<Item, Integer> inventoryAvailable = buildInventoryMap(inventory);
        for (int i = 0; i < 9; i++) {
            ItemStack craftStack = container.getCraftSlots().getItem(i);
            if (!craftStack.isEmpty()) {
                inventoryAvailable.merge(craftStack.getItem(), craftStack.getCount(), Integer::sum);
            }
        }

        // Slots we'll ask the server to fill
        List<Integer> slotsToFill = new ArrayList<>();
        List<ResourceLocation> itemsToFill = new ArrayList<>();

        // Track EMC-sourced slots separately so we can highlight them if balance is insufficient
        List<Integer> emcSlotIndices = new ArrayList<>();
        long totalEmcNeeded = 0;

        // Truly missing slots (not in inventory AND not obtainable via EMC)
        List<Integer> missingSlots = new ArrayList<>();

        for (int slotIdx = 0; slotIdx < inputSlots.size() && slotIdx < 9; slotIdx++) {
            IRecipeSlotView slotView = inputSlots.get(slotIdx);

            // Unique Item types that can satisfy this recipe slot
            List<Item> candidates = slotView.getItemStacks()
                .map(ItemStack::getItem)
                .distinct()
                .toList();

            if (candidates.isEmpty()) continue; // empty slot in shaped recipe

            // ── Priority 1: take from virtual inventory (real inventory + returned crafting items) ──
            Item itemFromInventory = null;
            for (Item candidate : candidates) {
                int available = inventoryAvailable.getOrDefault(candidate, 0);
                if (available > 0) {
                    itemFromInventory = candidate;
                    inventoryAvailable.put(candidate, available - 1); // consume from simulation
                    break;
                }
            }
            if (itemFromInventory != null) {
                slotsToFill.add(slotIdx);
                itemsToFill.add(BuiltInRegistries.ITEM.getKey(itemFromInventory));
                continue;
            }

            // ── Priority 2: withdraw from EMC ──
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
                emcSlotIndices.add(slotIdx); // mark as EMC-sourced
                continue;
            }

            // ── Truly missing ──
            missingSlots.add(slotIdx);
        }

        // Return error with red overlay if items are truly missing
        if (!missingSlots.isEmpty()) {
            return new MissingItemsError(missingSlots);
        }

        // Return error with red overlay on EMC slots if not enough balance
        if (totalEmcNeeded > emcData.getEmc()) {
            return new MissingItemsError(emcSlotIndices);
        }

        // All slots can be satisfied → send packet to server
        if (doTransfer && !slotsToFill.isEmpty()) {
            PacketDistributor.sendToServer(new FillCraftingFromEMCPayload(slotsToFill, itemsToFill));
        }

        return null; // success
    }

    /** Builds an item→count map from the player's inventory for simulation. */
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
