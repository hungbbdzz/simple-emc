package com.velorise.emc;

import com.velorise.emc.common.CoreAuraMachineProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CoreAuraMachineScreen extends AbstractContainerScreen<CoreAuraMachineMenu> {
    private Button enabledButton;

    public CoreAuraMachineScreen(CoreAuraMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 97;
    }

    @Override protected void init() {
        super.init();
        enabledButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(0))
            .bounds(leftPos + 136, topPos + 24, 32, 20).build());
        refresh();
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }

    private void refresh() {
        if (enabledButton != null) enabledButton.setMessage(Component.literal(menu.isEnabled() ? "ON" : "OFF"));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaUiStyle.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaUiStyle.drawInset(graphics, leftPos + 4, topPos + 48, 168, 47);
        VanillaUiStyle.drawInset(graphics, leftPos + 4, topPos + 102, 168, 84);
        VanillaUiStyle.drawSlot(graphics, leftPos + 7, topPos + 56);
        int width = (int) Math.min(136L, menu.getStoredEmc() * 136L / CoreAuraMachineProfile.maxEmc(menu.getKind()));
        VanillaUiStyle.drawBar(graphics, leftPos + 32, topPos + 74, 136, width, true);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("Core " + menu.getCoreTier() + "  |  Radius " + menu.getRange()),
            8, 14, VanillaUiStyle.MUTED_TEXT, false);
        graphics.drawString(font, Component.literal(menu.getCostPerSecond() + " EMC/s  |  " + menu.getCostPerPulse() + " EMC/pulse"),
            32, 56, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal(String.format("%,d EMC", menu.getStoredEmc())), 32, 68, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, statusText(), 8, 84, statusColor(), false);
        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, VanillaUiStyle.TEXT, false);
    }

    private Component statusText() {
        if (!menu.isEnabled()) return Component.translatable("screen.velorise_emc.status.disabled");
        if (menu.getStoredEmc() < menu.getCostPerPulse()) return Component.translatable("screen.velorise_emc.status.no_emc");
        return Component.translatable(menu.isRunning() ? "screen.velorise_emc.status.active" : "screen.velorise_emc.status.ready");
    }
    private int statusColor() {
        if (!menu.isEnabled()) return VanillaUiStyle.STATUS_WARN;
        if (menu.getStoredEmc() < menu.getCostPerPulse()) return VanillaUiStyle.STATUS_ERROR;
        return VanillaUiStyle.STATUS_OK;
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
