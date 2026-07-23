package com.velorise.emc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Shared low-opacity world backdrop without the heavy Vanilla blur pass. */
public final class ScreenRenderCompat {
    private ScreenRenderCompat() {}

    public static void renderBackground(Screen screen, GuiGraphics graphics,
                                        int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, 0x58000000);
    }
}
