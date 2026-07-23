package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Small shared drawing helpers for restrained Vanilla-style container screens. */
public final class VanillaUiStyle {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int PANEL_LIGHT = 0xFFFFFFFF;
    public static final int PANEL_DARK = 0xFF555555;
    public static final int INSET = 0xFF8B8B8B;
    public static final int INSET_LIGHT = 0xFFE0E0E0;
    // Vanilla container panels are light gray. Dark, shadowless labels remain
    // readable on both the panel and its slightly darker inset regions.
    public static final int TEXT = 0xFF202020;
    public static final int MUTED_TEXT = 0xFF4F4F4F;
    public static final int DISABLED_TEXT = 0xFF777777;
    public static final int INPUT_TEXT = 0xFFE0E0E0;
    public static final int BAR_TRACK = 0xFF666666;
    public static final int BAR_FILL = 0xFFB0B0B0;
    public static final int BAR_EMC = 0xFFD0A83C;
    public static final int STATUS_OK = 0xFF2E7D32;
    public static final int STATUS_WARN = 0xFF9A6700;
    public static final int STATUS_ERROR = 0xFFA03030;
    public static final int ROW = 0xFFB8B8B8;
    public static final int ROW_HOVER = 0xFFD0D0D0;
    public static final int ROW_SELECTED = 0xFFE0E0E0;
    public static final int ITEM_UNAFFORDABLE_OVERLAY = 0x70503030;
    public static final int ITEM_HOVER_OVERLAY = 0x50FFFFFF;
    public static final int ROW_ALTERNATE_OVERLAY = 0x20707070;

    private VanillaUiStyle() {}

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 1, PANEL_LIGHT);
        graphics.fill(x, y, x + 1, y + height, PANEL_LIGHT);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_DARK);
    }

    public static void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, INSET);
        graphics.fill(x, y, x + width, y + 1, PANEL_DARK);
        graphics.fill(x, y, x + 1, y + height, PANEL_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, INSET_LIGHT);
        graphics.fill(x, y + height - 1, x + width, y + height, INSET_LIGHT);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y) {
        drawInset(graphics, x - 1, y - 1, 18, 18);
    }

    public static void drawBar(GuiGraphics graphics, int x, int y, int width, int filled, boolean complete) {
        // 1px Black Outline
        graphics.fill(x, y, x + width, y + 5, 0xFF000000);
        // Dark Inner Track
        graphics.fill(x + 1, y + 1, x + width - 1, y + 4, 0xFF2A2A2A);
        int clamped = Math.max(0, Math.min(width - 2, filled));
        if (clamped > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + clamped, y + 4, complete ? 0xFFFFFFFF : 0xFF9E9E9E);
        }
    }

    public static void drawCenteredText(GuiGraphics graphics, Font font, Component text,
                                        int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }
}
