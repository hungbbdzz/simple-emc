package com.velorise.simpleemc.compat.jei;

import com.velorise.simpleemc.SimpleEMC;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class SimpleEMCJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(SimpleEMC.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            new ArcaneEMCTransferHandler(),
            RecipeTypes.CRAFTING
        );
    }
}
