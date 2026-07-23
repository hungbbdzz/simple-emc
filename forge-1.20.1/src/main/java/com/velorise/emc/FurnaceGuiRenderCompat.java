package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.20.1 furnace progress renderer using the legacy texture sheet. */
public final class FurnaceGuiRenderCompat {
    private FurnaceGuiRenderCompat() {}

    public static void renderProgress(GuiGraphics graphics, ResourceLocation background,
                                      ResonantFurnaceMenu menu, int left, int top) {
        if (menu.isLit()) {
            int flame = menu.getLitProgress();
            if (flame > 0) {
                graphics.blit(background, left + 56, top + 36 + 12 - flame,
                    176, 12 - flame, 14, flame + 1);
            }
        }

        int progress = menu.getBurnProgress();
        if (progress > 0) {
            graphics.blit(background, left + 79, top + 34, 176, 14, progress + 1, 16);
        }
    }
}
