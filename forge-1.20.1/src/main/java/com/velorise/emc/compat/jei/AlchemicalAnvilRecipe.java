package com.velorise.emc.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record AlchemicalAnvilRecipe(
    Ingredient leftInput,
    Ingredient rightInput,
    ItemStack output,
    int requiredAnvilLevel
) {}
