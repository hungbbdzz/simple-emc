package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ResonantFurnaceScreen extends AbstractContainerScreen<ResonantFurnaceMenu> {
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.tryParse("minecraft:textures/gui/container/furnace.png");
    private static final ResourceLocation SMOKER_TEXTURE = ResourceLocation.tryParse("minecraft:textures/gui/container/smoker.png");
    private static final ResourceLocation BLAST_FURNACE_TEXTURE = ResourceLocation.tryParse("minecraft:textures/gui/container/blast_furnace.png");
    private static final ResourceLocation RECIPE_BUTTON_TEXTURE = ResourceLocation.tryParse("minecraft:textures/gui/recipe_button.png");

    private Button recipeButton;

    public ResonantFurnaceScreen(ResonantFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    private ResourceLocation getGuiTexture() {
        if (menu == null) return FURNACE_TEXTURE;
        return switch (menu.getKind()) {
            case SMOKER -> SMOKER_TEXTURE;
            case BLAST_FURNACE -> BLAST_FURNACE_TEXTURE;
            default -> FURNACE_TEXTURE;
        };
    }

    @Override
    protected void init() {
        super.init();
        recipeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.setScreen(new OpenRecipeInventoryScreen(minecraft.player));
            }
        }).bounds(leftPos - 28, topPos + 29, 20, 18).build());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = getGuiTexture();
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        FurnaceGuiRenderCompat.renderProgress(graphics, texture, menu, leftPos, topPos);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, VanillaUiStyle.TEXT, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (recipeButton != null) {
            int v = recipeButton.isHoveredOrFocused() ? 18 : 0;
            graphics.blit(RECIPE_BUTTON_TEXTURE, recipeButton.getX(), recipeButton.getY(), 0, v, 20, 18);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
}
