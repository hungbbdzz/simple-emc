package com.velorise.simpleemc.compat.jei;

import com.velorise.simpleemc.SimpleEMC;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class SimpleEMCJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SimpleEMC.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Custom handler: fills crafting grid from inventory first, then EMC for missing items.
        registration.addRecipeTransferHandler(
            new ArcaneEMCTransferHandler(),
            RecipeTypes.CRAFTING
        );
    }
}
