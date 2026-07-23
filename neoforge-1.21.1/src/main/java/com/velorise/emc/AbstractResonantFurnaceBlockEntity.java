package com.velorise.emc;

import com.velorise.emc.common.ResonantMachineProfile;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Core-scaled furnace implementation using fuel and speed multipliers. */
public abstract class AbstractResonantFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements ResonantMachine {
    public enum Kind { FURNACE, SMOKER, BLAST_FURNACE }

    private static final String TAG_CORE = "CoreTier";
    private static final String TAG_ENABLED = "MachineEnabled";

    private final Kind kind;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private int coreTier = 1;
    private boolean machineEnabled = true;

    protected AbstractResonantFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                                  RecipeType<? extends AbstractCookingRecipe> recipeType, Kind kind) {
        super(type, pos, state, recipeType);
        this.recipeType = recipeType;
        this.kind = kind;
    }

    @Override protected Component getDefaultName() {
        return Component.translatable(switch (kind) {
            case FURNACE -> "container.velorise_emc.resonant_furnace";
            case SMOKER -> "container.velorise_emc.resonant_smoker";
            case BLAST_FURNACE -> "container.velorise_emc.resonant_blast_furnace";
        });
    }

    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ResonantFurnaceMenu(id, inventory, this); }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) {
            return getBurnDuration(stack) > 0 || super.canPlaceItem(slot, stack);
        }
        return super.canPlaceItem(slot, stack);
    }

    @Override
    protected int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return Math.max(0, super.getBurnDuration(stack));
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<? extends AbstractCookingRecipe> getRecipe(Level level, AbstractResonantFurnaceBlockEntity furnace) {
        ItemStack input = furnace.getItem(SLOT_INPUT);
        if (input.isEmpty()) return null;
        var recipeInput = new net.minecraft.world.item.crafting.SingleRecipeInput(input);
        var rm = level.getRecipeManager();
        var match = rm.getRecipeFor((RecipeType<AbstractCookingRecipe>) furnace.recipeType, recipeInput, level);
        if (match.isPresent()) return match.get();
        return rm.getRecipeFor(RecipeType.SMELTING, recipeInput, level).orElse(null);
    }

    private static boolean canSmelt(Level level, AbstractResonantFurnaceBlockEntity furnace, RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (recipe == null || furnace.getItem(SLOT_INPUT).isEmpty()) return false;
        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;
        ItemStack output = furnace.getItem(SLOT_RESULT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private static void smeltItem(Level level, AbstractResonantFurnaceBlockEntity furnace, RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (!canSmelt(level, furnace, recipe)) return;
        ItemStack input = furnace.getItem(SLOT_INPUT);
        ItemStack result = recipe.value().getResultItem(level.registryAccess()).copy();
        ItemStack output = furnace.getItem(SLOT_RESULT);

        if (output.isEmpty()) {
            furnace.setItem(SLOT_RESULT, result);
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            output.grow(result.getCount());
        }

        input.shrink(1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractResonantFurnaceBlockEntity furnace) {
        if (level.isClientSide) return;

        boolean wasLit = furnace.isVisuallyLit();
        boolean dirty = false;

        if (furnace.getLitTime() > 0) {
            furnace.dataAccess.set(0, furnace.getLitTime() - 1);
        }

        RecipeHolder<? extends AbstractCookingRecipe> recipe = getRecipe(level, furnace);
        boolean canSmelt = canSmelt(level, furnace, recipe);

        if (!furnace.isVisuallyLit() && canSmelt) {
            ItemStack fuel = furnace.getItem(SLOT_FUEL);
            int burnTime = furnace.getBurnDuration(fuel);
            if (burnTime > 0) {
                furnace.dataAccess.set(0, burnTime);
                furnace.dataAccess.set(1, burnTime);
                if (furnace.getLitTime() > 0) {
                    dirty = true;
                    if (!fuel.isEmpty()) {
                        Item item = fuel.getItem();
                        fuel.shrink(1);
                        if (fuel.isEmpty()) {
                            ItemStack remainder = item.getCraftingRemainingItem() != null
                                ? new ItemStack(item.getCraftingRemainingItem()) : ItemStack.EMPTY;
                            furnace.setItem(SLOT_FUEL, remainder);
                        }
                    }
                }
            }
        }

        if (furnace.isVisuallyLit() && canSmelt) {
            int totalTime = recipe.value().getCookingTime();
            if (totalTime <= 0) totalTime = 200;
            furnace.dataAccess.set(3, totalTime);

            int speed = Math.max(1, furnace.getSpeedMultiplier());
            int progress = furnace.getCookingProgress() + speed;
            furnace.dataAccess.set(2, progress);

            if (progress >= totalTime) {
                furnace.dataAccess.set(2, 0);
                smeltItem(level, furnace, recipe);
                dirty = true;
            }
        } else {
            furnace.dataAccess.set(2, 0);
        }

        if (wasLit != furnace.isVisuallyLit()) {
            dirty = true;
            state = state.setValue(AbstractResonantFurnaceBlock.LIT, furnace.isVisuallyLit());
            level.setBlock(pos, state, 3);
        }

        if (dirty) {
            furnace.setChanged();
        }
    }

    public int getLitTime() { return dataAccess.get(0); }
    public int getLitDuration() { return dataAccess.get(1); }
    public boolean isVisuallyLit() { return getLitTime() > 0; }

    public int getOperationCost() { return 0; }
    public int getSpeedMultiplier() { return ResonantMachineProfile.furnaceSpeedMultiplier(coreTier); }

    public ItemStack createDropStack(Item item, BlockEntityType<?> type) {
        ItemStack result = new ItemStack(item);
        CoreTierData.set(result, WirelessCoreTier.byLevel(coreTier));
        CompoundTag tag = new CompoundTag();
        writeMachineData(tag);
        BlockItem.setBlockEntityData(result, type, tag);
        if (hasCustomName()) result.set(DataComponents.CUSTOM_NAME, getCustomName());
        return result;
    }
    protected void writeMachineData(CompoundTag tag) {
        tag.putInt(TAG_CORE, coreTier);
        tag.putBoolean(TAG_ENABLED, machineEnabled);
    }
    protected void readMachineData(CompoundTag tag) {
        coreTier = Math.max(1, Math.min(4, tag.contains(TAG_CORE) ? tag.getInt(TAG_CORE) : 1));
        machineEnabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); readMachineData(tag);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); writeMachineData(tag);
    }

    public int getCookingProgress() { return dataAccess.get(2); }
    public int getCookingTotalTime() { return dataAccess.get(3); }
    public int getCoreTierLevel() { return coreTier; }
    public void setCoreTier(WirelessCoreTier tier) { coreTier = tier == null ? 1 : tier.level(); setChanged(); }
    public Kind getKind() { return kind; }

    @Override public long getStoredEmc() { return 0L; }
    @Override public long getMaxStoredEmc() { return 0L; }
    @Override public long addEmc(long amount) { return 0L; }
    @Override public boolean isMachineEnabled() { return machineEnabled; }
    @Override public void setMachineEnabled(boolean enabled) { machineEnabled = enabled; setChanged(); }
}
