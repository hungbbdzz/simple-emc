package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlchemicalHourglassScreen extends AbstractContainerScreen<AlchemicalHourglassMenu> {
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.tryParse("minecraft:textures/gui/container/furnace.png");
    private Button enabledButton;

    public AlchemicalHourglassScreen(AlchemicalHourglassMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelY = 72;
    }

    @Override protected void init() {
        super.init();
        enabledButton = addRenderableWidget(Button.builder(Component.empty(), b -> sendButton(0))
            .bounds(leftPos + 98, topPos + 44, 40, 16).build());
        refreshButtons();
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override protected void containerTick() { super.containerTick(); refreshButtons(); }

    private void refreshButtons() {
        if (enabledButton == null) return;
        enabledButton.setMessage(Component.translatable(menu.isEnabled()
            ? "screen.velorise_emc.machine.enabled_short" : "screen.velorise_emc.machine.disabled_short"));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaUiStyle.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaUiStyle.drawSlot(graphics, leftPos + 145, topPos + 22);
        // The fuel is consumed immediately, so the vanilla flame can occupy the empty fuel slot.
        graphics.blit(FURNACE_TEXTURE, leftPos + 146, topPos + 23, 176, 0, 14, 14);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                VanillaUiStyle.drawSlot(graphics, leftPos + 8 + col * 18, topPos + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            VanillaUiStyle.drawSlot(graphics, leftPos + 8 + col * 18, topPos + 142);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VanillaUiStyle.TEXT, false);
        String tier = "Core " + AlchemicalAnvilItem.roman(menu.getCoreTierLevel());
        graphics.drawString(font, Component.literal("Tier: " + tier + " | +" + menu.getBonusTicks() + " ticks/s"), 8, 20, VanillaUiStyle.MUTED_TEXT, false);
        graphics.drawString(font, Component.literal("Targets: " + menu.getTargetCount() + " | Radius: " + menu.getRadius()), 8, 34, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal(String.format("%,d / %,d EMC", menu.getStoredEmc(),
            com.velorise.emc.common.HourglassProfile.maxEmc())), 8, 48, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("Fuel"), 145, 11, VanillaUiStyle.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, VanillaUiStyle.TEXT, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
