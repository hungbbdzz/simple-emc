package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact three-row paged UI for both Deepbound storage variants. */
public class DeepboundStorageScreen extends AbstractContainerScreen<DeepboundStorageMenu> {
    private Button previousButton;
    private Button nextButton;

    public DeepboundStorageScreen(DeepboundStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 204;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 108;
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> sendPageButton(0))
            .bounds(leftPos + 8, topPos + 20, 22, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> sendPageButton(1))
            .bounds(leftPos + imageWidth - 30, topPos + 20, 22, 20).build());
        refreshButtons();
    }

    private void sendPageButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (previousButton == null || nextButton == null) return;
        int page = menu.getCurrentPage();
        int pages = menu.getPageCount();
        previousButton.active = page > 0;
        nextButton.active = page + 1 < pages;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaUiStyle.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaUiStyle.drawInset(graphics, leftPos + 4, topPos + 42, imageWidth - 8, 61);
        VanillaUiStyle.drawInset(graphics, leftPos + 4, topPos + 116, imageWidth - 8, imageHeight - 120);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) slotBox(graphics, 8 + column * 18, 46 + row * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) slotBox(graphics, 8 + column * 18, 120 + row * 18);
        }
        for (int column = 0; column < 9; column++) slotBox(graphics, 8 + column * 18, 178);
    }

    private void slotBox(GuiGraphics graphics, int x, int y) {
        VanillaUiStyle.drawSlot(graphics, leftPos + x, topPos + y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VanillaUiStyle.TEXT, false);
        Component pageText = Component.translatable("screen.velorise_emc.deepbound.page",
            menu.getCurrentPage() + 1, menu.getPageCount());
        int pageX = (imageWidth - font.width(pageText)) / 2;
        graphics.drawString(font, pageText, pageX, 26, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.velorise_emc.deepbound.core",
            menu.getCoreTierLevel(), menu.getAccessibleSlotCount()), 8, 94, VanillaUiStyle.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX,
            inventoryLabelY, VanillaUiStyle.TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
