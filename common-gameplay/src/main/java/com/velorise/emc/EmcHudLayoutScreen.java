package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minimal drag-only editor: grab the HUD, then click empty space to save. */
public final class EmcHudLayoutScreen extends Screen {
    private static final String BALANCE = "EMC: 3,799,931";
    private static final String RATE = "+24 EMC/s";
    private static final String WORLD_TIER = "World Tier: I (Overworld)";
    private static final String TARGET_BLOCK = "Target block: 8,192 EMC";
    private final Screen parent;
    private boolean dragging;
    private int hudX;
    private int hudY;
    private int grabX;
    private int grabY;

    public EmcHudLayoutScreen(Screen parent) {
        super(Component.literal("Move EMC HUD"));
        this.parent = parent;
    }

    private String worldTierStr() {
        return EmcHudPreferences.showWorldTier() ? WORLD_TIER : null;
    }

    private String targetBlockStr() {
        return EmcHudPreferences.showLookedAtBlockEmc() ? TARGET_BLOCK : null;
    }

    @Override
    protected void init() {
        hudX = EmcHudPreferences.pixelX(width, EmcHudOverlay.scaledWidth(font, BALANCE, RATE, worldTierStr(), targetBlockStr()));
        hudY = EmcHudPreferences.pixelY(height, EmcHudOverlay.scaledHeight(worldTierStr(), targetBlockStr()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x99000000);
        EmcHudOverlay.renderAt(graphics, font, hudX, hudY, BALANCE, RATE, worldTierStr(), targetBlockStr(), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int hudWidth = EmcHudOverlay.scaledWidth(font, BALANCE, RATE, worldTierStr(), targetBlockStr());
        int hudHeight = EmcHudOverlay.scaledHeight(worldTierStr(), targetBlockStr());
        if (mouseX >= hudX && mouseX < hudX + hudWidth && mouseY >= hudY && mouseY < hudY + hudHeight) {
            dragging = true;
            grabX = (int)mouseX - hudX;
            grabY = (int)mouseY - hudY;
            return true;
        }
        saveAndClose(hudWidth, hudHeight);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return false;
        int hudWidth = EmcHudOverlay.scaledWidth(font, BALANCE, RATE, worldTierStr(), targetBlockStr());
        int hudHeight = EmcHudOverlay.scaledHeight(worldTierStr(), targetBlockStr());
        hudX = Math.max(0, Math.min(width - hudWidth, (int)mouseX - grabX));
        hudY = Math.max(0, Math.min(height - hudHeight, (int)mouseY - grabY));
        EmcHudPreferences.setPixelPosition(hudX, hudY, width, height, hudWidth, hudHeight);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return true;
    }

    @Override
    public void onClose() {
        saveAndClose(EmcHudOverlay.scaledWidth(font, BALANCE, RATE, worldTierStr(), targetBlockStr()),
            EmcHudOverlay.scaledHeight(worldTierStr(), targetBlockStr()));
    }

    private void saveAndClose(int hudWidth, int hudHeight) {
        EmcHudPreferences.setPixelPosition(hudX, hudY, width, height, hudWidth, hudHeight);
        EmcHudPreferences.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
