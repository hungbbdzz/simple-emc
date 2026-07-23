package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ResonanceJukeboxScreen extends AbstractContainerScreen<ResonanceJukeboxMenu> {
    private Button playButton;
    private Button repeatButton;
    private Button shuffleButton;

    public ResonanceJukeboxScreen(ResonanceJukeboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 194;
        imageHeight = 226;
        inventoryLabelX = 12;
        inventoryLabelY = 128;
        titleLabelX = 8;
        titleLabelY = 4;
    }

    @Override
    protected void init() {
        super.init();
        playButton = addRenderableWidget(Button.builder(Component.literal("Play"), button -> press(0))
            .bounds(leftPos + 8, topPos + 18, 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("◀"), button -> press(1))
            .bounds(leftPos + 58, topPos + 18, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), button -> press(2))
            .bounds(leftPos + 84, topPos + 18, 24, 20).build());
        repeatButton = addRenderableWidget(Button.builder(Component.literal("Repeat"), button -> press(3))
            .bounds(leftPos + 110, topPos + 18, 76, 20).build());
        shuffleButton = addRenderableWidget(Button.builder(Component.literal("Shuffle"), button -> press(4))
            .bounds(leftPos + 8, topPos + 40, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("−"), button -> press(30))
            .bounds(leftPos + 90, topPos + 40, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> press(31))
            .bounds(leftPos + 116, topPos + 40, 24, 20).build());

        refreshButtons();
    }

    private void press(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (playButton == null) return;
        playButton.setMessage(Component.translatable(menu.isPlaying()
            ? "screen.velorise_emc.jukebox.stop" : "screen.velorise_emc.jukebox.play"));
        repeatButton.setMessage(Component.translatable("screen.velorise_emc.jukebox.repeat."
            + menu.getRepeatMode().name().toLowerCase()));
        shuffleButton.setMessage(Component.translatable(menu.isShuffle()
            ? "screen.velorise_emc.jukebox.shuffle_on" : "screen.velorise_emc.jukebox.shuffle_off"));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaUiStyle.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaUiStyle.drawInset(graphics, leftPos + 4, topPos + 14, 186, 48);
        VanillaUiStyle.drawInset(graphics, leftPos + 4, topPos + 72, 186, 48);
        VanillaUiStyle.drawInset(graphics, leftPos + 12, topPos + 138, 170, 86);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 10; col++) slotBox(graphics, 8 + col * 18, 76 + row * 20);
        }
        int selected = menu.getSelectedSlot();
        int selectedX = 8 + (selected % 10) * 18;
        int selectedY = 76 + (selected / 10) * 20;
        drawSelectedSlot(graphics, selectedX, selectedY);
        slotBox(graphics, 170, 40);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slotBox(graphics, 16 + col * 18, 142 + row * 18);
        }
        for (int col = 0; col < 9; col++) slotBox(graphics, 16 + col * 18, 200);

    }

    private void slotBox(GuiGraphics graphics, int x, int y) {
        VanillaUiStyle.drawSlot(graphics, leftPos + x, topPos + y);
    }

    private void drawSelectedSlot(GuiGraphics graphics, int x, int y) {
        // Fill only the 16x16 slot interior. The vanilla-style border remains intact
        // and the marker cannot overlap an adjacent playlist slot.
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF707070);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VanillaUiStyle.TEXT, false);
        // Keep the compact right-side fuel slot clear; only the percentage is shown
        // beside it so labels never draw over the playback buttons or slot frame.
        graphics.drawString(font, Component.literal(menu.getVolume() + "%"), 144, 47,
            VanillaUiStyle.MUTED_TEXT, false);

        ItemStack disc = menu.getSelectedDisc();
        Component name = disc.isEmpty()
            ? Component.translatable("screen.velorise_emc.jukebox.no_track")
            : disc.getHoverName();
        String trackName = font.plainSubstrByWidth(name.getString(), 96);
        graphics.drawString(font, Component.literal(trackName), 8, 63,
            disc.isEmpty() ? VanillaUiStyle.DISABLED_TEXT : VanillaUiStyle.TEXT, false);
        String emc = EmcTextFormat.compact(menu.getStoredEmc()) + " EMC";
        graphics.drawString(font, Component.literal(emc), 182 - font.width(emc), 63,
            VanillaUiStyle.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, VanillaUiStyle.TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
