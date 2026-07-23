package com.velorise.emc;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Rendering and measurement for the compact client EMC HUD. */
public final class EmcHudOverlay {
    private EmcHudOverlay() {}

    public static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight,
                              String balance, String rate, String worldTier, String stack, boolean positiveRate) {
        int width = baseWidth(font, balance, rate, worldTier, stack);
        int height = baseHeight(worldTier, stack);
        float scale = EmcHudPreferences.scale();
        int scaledWidth = Math.round(width * scale);
        int scaledHeight = Math.round(height * scale);
        int x = EmcHudPreferences.pixelX(screenWidth, scaledWidth);
        int y = EmcHudPreferences.pixelY(screenHeight, scaledHeight);
        renderAt(graphics, font, x, y, balance, rate, worldTier, stack, positiveRate);
    }

    public static void renderAt(GuiGraphics graphics, Font font, int x, int y,
                                String balance, String rate, String worldTier, String stack, boolean positiveRate) {
        float scale = EmcHudPreferences.scale();
        int width = baseWidth(font, balance, rate, worldTier, stack);
        int height = baseHeight(worldTier, stack);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.drawString(font, balance, 5, 4, 0xFFFFD866, false);
        graphics.drawString(font, rate, 5, 14, positiveRate ? 0xFF71E38A : 0xFFFF7777, false);
        int currentY = 24;
        if (worldTier != null && !worldTier.isBlank()) {
            graphics.drawString(font, worldTier, 5, currentY, 0xFF55FFFF, false);
            currentY += 10;
        }
        if (stack != null && !stack.isBlank()) {
            graphics.drawString(font, stack, 5, currentY, 0xFFFFFFFF, false);
        }
        graphics.pose().popPose();
    }

    public static int scaledWidth(Font font, String balance, String rate, String worldTier, String stack) {
        return Math.round(baseWidth(font, balance, rate, worldTier, stack) * EmcHudPreferences.scale());
    }

    public static int scaledHeight(String worldTier, String stack) {
        return Math.round(baseHeight(worldTier, stack) * EmcHudPreferences.scale());
    }

    private static int baseWidth(Font font, String balance, String rate, String worldTier, String stack) {
        int width = Math.max(font.width(balance), font.width(rate));
        if (worldTier != null && !worldTier.isBlank()) width = Math.max(width, font.width(worldTier));
        if (stack != null && !stack.isBlank()) width = Math.max(width, font.width(stack));
        return width + 10;
    }

    private static int baseHeight(String worldTier, String stack) {
        int lines = 2;
        if (worldTier != null && !worldTier.isBlank()) lines++;
        if (stack != null && !stack.isBlank()) lines++;
        return 8 + lines * 10;
    }
}
