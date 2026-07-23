package com.velorise.emc.compat.jei;

import com.velorise.emc.AlchemicalAnvilItem;
import com.velorise.emc.CoreTierData;
import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.VanillaReworkEvents;
import com.velorise.emc.common.WirelessCoreTier;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JeiPlugin
public class VeloriseEMCJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(VeloriseEMC.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (int tier = 1; tier <= 4; tier++) registration.addRecipeCategories(new AlchemicalAnvilCategory(registration.getJeiHelpers().getGuiHelper(), tier));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<AlchemicalAnvilRecipe>[] recipesByTier = new List[4];
        for (int i = 0; i < 4; i++) recipesByTier[i] = new ArrayList<>();
        for (WirelessCoreTier tier : WirelessCoreTier.values()) addRuntimeUpgradeRecipes(recipesByTier[tier.level() - 1], tier);
        for (int tier = 1; tier <= 4; tier++) {
            registration.addRecipes(AlchemicalAnvilCategory.typeForLevel(tier), recipesByTier[tier - 1]);
        }
    }

    private static void addRuntimeUpgradeRecipes(List<AlchemicalAnvilRecipe> recipes, WirelessCoreTier tier) {
        ItemStack core = CoreTierData.createCoreStack(tier);
        Set<String> seen = new HashSet<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack base = item.getDefaultInstance();
            if (base.isEmpty() || CoreTierData.isCoreItem(base)
                || AlchemicalAnvilItem.isAnvil(base)) continue;

            // Build the exact input state expected by this tier. This is important for
            // ordinary tools/armor: Core II-IV require both the previous tier marker
            // and the imbued marker created by Core I. Merely writing tier NBT made
            // JEI omit almost every normal item from the higher-tier categories.
            ItemStack left = base.copy();
            for (int level = 1; level < tier.level(); level++) {
                ItemStack next = VanillaReworkEvents.createCoreUpgradePreview(
                    left, WirelessCoreTier.byLevel(level));
                // Integrated mod items already begin at Core I, so applying Core I to
                // their default stack legitimately returns empty. Continue to Core II.
                if (!next.isEmpty()) left = next;
            }

            ItemStack output = VanillaReworkEvents.createCoreUpgradePreview(left, tier);
            if (output.isEmpty()) continue;

            ResourceLocation leftId = BuiltInRegistries.ITEM.getKey(left.getItem());
            ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(output.getItem());
            String key = String.valueOf(leftId) + ">" + outputId + "@" + tier.level();
            if (!seen.add(key)) continue;

            recipes.add(new AlchemicalAnvilRecipe(
                Ingredient.of(left),
                Ingredient.of(core),
                output,
                tier.level()
            ));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (int tier = 1; tier <= 4; tier++) {
            registration.addRecipeCatalyst(AlchemicalAnvilItem.createStack(tier), AlchemicalAnvilCategory.typeForLevel(tier));
        }
        ItemStack furnace = new ItemStack(VeloriseEMC.RESONANT_FURNACE_ITEM.get());
        registration.addRecipeCatalyst(furnace, RecipeTypes.SMELTING);

        ItemStack smoker = new ItemStack(VeloriseEMC.RESONANT_SMOKER_ITEM.get());
        registration.addRecipeCatalyst(smoker, RecipeTypes.SMOKING);
        registration.addRecipeCatalyst(smoker, RecipeTypes.SMELTING);

        ItemStack blastFurnace = new ItemStack(VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get());
        registration.addRecipeCatalyst(blastFurnace, RecipeTypes.BLASTING);
        registration.addRecipeCatalyst(blastFurnace, RecipeTypes.SMELTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            new ArcaneEMCTransferHandler(),
            RecipeTypes.CRAFTING
        );
        registration.addRecipeTransferHandler(
            com.velorise.emc.ResonantFurnaceMenu.class,
            VeloriseEMC.RESONANT_FURNACE_MENU_TYPE.get(),
            RecipeTypes.SMELTING,
            0, 1, 3, 36
        );
        registration.addRecipeTransferHandler(
            com.velorise.emc.ResonantFurnaceMenu.class,
            VeloriseEMC.RESONANT_FURNACE_MENU_TYPE.get(),
            RecipeTypes.BLASTING,
            0, 1, 3, 36
        );
        registration.addRecipeTransferHandler(
            com.velorise.emc.ResonantFurnaceMenu.class,
            VeloriseEMC.RESONANT_FURNACE_MENU_TYPE.get(),
            RecipeTypes.SMOKING,
            0, 1, 3, 36
        );
    }
}
