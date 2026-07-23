package com.velorise.emc.compat.jei;

import com.velorise.emc.AlchemicalAnvilItem;
import com.velorise.emc.VeloriseEMC;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AlchemicalAnvilCategory implements IRecipeCategory<AlchemicalAnvilRecipe> {
    public static final RecipeType<AlchemicalAnvilRecipe>[] TYPES = createTypes();

    @SuppressWarnings("unchecked")
    private static RecipeType<AlchemicalAnvilRecipe>[] createTypes() {
        RecipeType<AlchemicalAnvilRecipe>[] result = new RecipeType[4];
        for (int i = 0; i < 4; i++) {
            result[i] = RecipeType.create(VeloriseEMC.MODID, "alchemical_anvil_" + (i + 1), AlchemicalAnvilRecipe.class);
        }
        return result;
    }

    public static RecipeType<AlchemicalAnvilRecipe> typeForLevel(int level) {
        return TYPES[Math.max(1, Math.min(4, level)) - 1];
    }

    private final int minimumTier;
    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public AlchemicalAnvilCategory(IGuiHelper guiHelper, int minimumTier) {
        this.minimumTier = Math.max(1, Math.min(4, minimumTier));
        this.background = guiHelper.createDrawable(
            ResourceLocation.tryParse("minecraft:textures/gui/container/anvil.png"), 20, 16, 136, 54);
        this.icon = guiHelper.createDrawableItemStack(AlchemicalAnvilItem.createStack(this.minimumTier));
        this.title = Component.literal("Alchemical Anvil " + AlchemicalAnvilItem.roman(this.minimumTier));
    }

    @Override public RecipeType<AlchemicalAnvilRecipe> getRecipeType() { return typeForLevel(minimumTier); }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlchemicalAnvilRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 7, 31).addIngredients(recipe.leftInput());
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 31).addIngredients(recipe.rightInput());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 31).addItemStack(recipe.output());
    }

    @Override
    public void draw(AlchemicalAnvilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.fill(39, 3, 136, 22, 0xFF373737);
        guiGraphics.fill(40, 4, 135, 21, 0xFF282828);
        Font font = Minecraft.getInstance().font;
        String tierRoman = AlchemicalAnvilItem.roman(Math.max(1, recipe.requiredAnvilLevel()));
        Component text = Component.literal("§7Anvil Tier: §e" + tierRoman);
        guiGraphics.drawString(font, text, 42, 9, 0xFFFFFFFF, false);
    }
}
