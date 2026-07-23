package com.velorise.emc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.21+ furnace progress renderer using the GUI sprite atlas. */
public final class FurnaceGuiRenderCompat {
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;

    private FurnaceGuiRenderCompat() {}

    public static void renderProgress(GuiGraphics graphics, ResourceLocation ignoredBackground,
                                      ResonantFurnaceMenu menu, int left, int top) {
        ResourceLocation litSprite = sprite(menu.getKind(), "lit_progress");
        ResourceLocation burnSprite = sprite(menu.getKind(), "burn_progress");

        if (menu.isLit()) {
            int flameHeight = menu.getLitProgress(FLAME_HEIGHT);
            if (flameHeight > 0) {
                int sourceY = FLAME_HEIGHT - flameHeight;
                graphics.blitSprite(litSprite, FLAME_WIDTH, FLAME_HEIGHT, 0, sourceY,
                    left + 56, top + 36 + sourceY, FLAME_WIDTH, flameHeight);
            }
        }

        int arrowWidth = menu.getProgressPixels(ARROW_WIDTH);
        if (arrowWidth > 0) {
            graphics.blitSprite(burnSprite, ARROW_WIDTH, ARROW_HEIGHT, 0, 0,
                left + 79, top + 34, arrowWidth, ARROW_HEIGHT);
        }
    }

    private static ResourceLocation sprite(AbstractResonantFurnaceBlockEntity.Kind kind, String name) {
        String folder = switch (kind) {
            case SMOKER -> "smoker";
            case BLAST_FURNACE -> "blast_furnace";
            default -> "furnace";
        };
        return ResourceLocation.tryParse("minecraft:container/" + folder + "/" + name);
    }
}
