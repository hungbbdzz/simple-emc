package com.velorise.emc;

import com.velorise.emc.common.WorldProgressionObjective;
import com.velorise.emc.common.WorldProgressionProfile;
import com.velorise.emc.common.WorldProgressionState;
import com.velorise.emc.common.WorldTierStage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only overview of the server-global World Tier hunt and its multiplayer scaling. */
public final class WorldProgressScreen extends Screen {
    private static final int PANEL_WIDTH = 244;
    private static final int PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 22;
    private static final int VIEWPORT_HEIGHT = 132;
    private final Screen parent;
    private WorldTierStage selectedStage;
    private int scrollOffset = 0;

    public WorldProgressScreen(Screen parent) {
        super(Component.literal("World Progress"));
        this.parent = parent;
        WorldTierStage active = ClientWorldProgress.current().activeStage();
        this.selectedStage = active == null ? WorldTierStage.DEEP_WORLD_CONQUEST : active;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int buttonWidth = 49;
        for (WorldTierStage stage : WorldTierStage.values()) {
            this.addRenderableWidget(Button.builder(Component.literal(stageButtonLabel(stage)), button -> {
                selectedStage = stage;
                scrollOffset = 0;
                updateStageButtons();
            }).bounds(left + 10 + stage.index() * 55, top + 31, buttonWidth, 16).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(left + PANEL_WIDTH - 58, top + PANEL_HEIGHT - 20, 48, 16).build());
        updateStageButtons();
    }

    private void updateStageButtons() {
        for (var child : children()) {
            if (!(child instanceof Button button)) continue;
            String text = button.getMessage().getString();
            for (WorldTierStage stage : WorldTierStage.values()) {
                String label = stageButtonLabel(stage);
                String selected = "[" + label + "]";
                if (text.equals(label) || text.equals(selected)) {
                    button.setMessage(Component.literal(stage == selectedStage ? selected : label));
                }
            }
        }
    }

    private static String stageButtonLabel(WorldTierStage stage) {
        if (stage == WorldTierStage.OVERWORLD_HUNT) return "T0→I";
        if (stage == WorldTierStage.NETHER_CONQUEST) return "I→II";
        if (stage == WorldTierStage.END_CONQUEST) return "II→III";
        return "III→IV";
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scrollList(mouseX, mouseY, delta);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrollList(mouseX, mouseY, scrollY);
    }

    private boolean scrollList(double mouseX, double mouseY, double delta) {
        int max = maxScroll();
        if (max <= 0) {
            scrollOffset = 0;
            return false;
        }
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(delta) * 18));
        return true;
    }

    private int maxScroll() {
        int count = 0;
        for (WorldProgressionObjective obj : WorldProgressionObjective.values()) {
            if (obj.stage() == selectedStage) count++;
        }
        return Math.max(0, count * ROW_HEIGHT - VIEWPORT_HEIGHT + 6);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        VanillaUiStyle.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        VanillaUiStyle.drawInset(graphics, left + 7, top + 51, PANEL_WIDTH - 14, 137);

        WorldProgressionState state = ClientWorldProgress.current();
        String tierText = state.worldTier() == 0 ? "World Tier 0" : "World Tier " + roman(state.worldTier());
        VanillaUiStyle.drawCenteredText(graphics, font, Component.literal(tierText), width / 2, top + 9,
            VanillaUiStyle.TEXT);
        VanillaUiStyle.drawCenteredText(graphics, font, Component.literal(selectedStage.displayName()), width / 2,
            top + 20, VanillaUiStyle.MUTED_TEXT);

        int contributors = state.contributorCount(selectedStage);
        double scale = WorldProgressionProfile.scale(contributors);
        String meta = "Contributors: " + contributors + "  |  Scale: ×" + formatScale(scale);
        graphics.drawString(font, meta, left + 10, top + 193, VanillaUiStyle.MUTED_TEXT, false);

        int maxScr = maxScroll();
        if (scrollOffset > maxScr) scrollOffset = maxScr;

        // Render objectives inside scissor clip (expanded right boundary to avoid clipping text)
        graphics.enableScissor(left + 8, top + 52, left + PANEL_WIDTH - 8, top + 187);

        int row = 0;
        for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
            if (objective.stage() != selectedStage) continue;
            int y = top + 54 + (row * ROW_HEIGHT) - scrollOffset;

            if (y + ROW_HEIGHT >= top + 50 && y <= top + 188) {
                long current = state.count(objective);
                long required = state.required(objective);
                boolean complete = required <= 0L || current >= required || current >= (Long.MAX_VALUE / 2);

                int textColor = complete ? 0xFFFFFFFF : VanillaUiStyle.TEXT;
                graphics.drawString(font, objective.displayName(), left + 12, y, textColor, false);

                String countText;
                if (required <= 0L) {
                    countText = "Disabled";
                } else if (complete) {
                    countText = "✓ " + formatCount(required) + " / " + formatCount(required);
                } else {
                    countText = formatCount(current) + " / " + formatCount(required);
                }

                int rightMargin = maxScr > 0 ? 22 : 14;
                graphics.drawString(font, countText, left + PANEL_WIDTH - rightMargin - font.width(countText), y + 4,
                    complete ? 0xFFFFFFFF : VanillaUiStyle.MUTED_TEXT, false);

                long barCurrent = complete ? required : current;
                int fill = required <= 0L ? 102 : (int) Math.min(102L, barCurrent * 102L / Math.max(1L, required));
                VanillaUiStyle.drawBar(graphics, left + 12, y + 10, 104, fill, complete);
            }
            row++;
        }
        graphics.disableScissor();

        // Draw scrollbar if content overflows (Reversed colors: dark track 0xFF181A1F, light handle 0xFFE0E0E0)
        if (maxScr > 0) {
            int sbX = left + PANEL_WIDTH - 12;
            int sbY = top + 53;
            int sbH = 133;
            graphics.fill(sbX, sbY, sbX + 3, sbY + sbH, 0xFF181A1F);
            int thumbH = Math.max(16, sbH * VIEWPORT_HEIGHT / (VIEWPORT_HEIGHT + maxScr));
            int thumbY = sbY + (int) ((double) scrollOffset / maxScr * (sbH - thumbH));
            graphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0xFFE0E0E0);
        }

        String reward = selectedStage.unlockedWorldTier() <= state.worldTier()
            ? "Core " + roman(selectedStage.unlockedWorldTier()) + " unlocked"
            : "Unlocks Core " + roman(selectedStage.unlockedWorldTier());
        graphics.drawString(font, reward, left + 10, top + PANEL_HEIGHT - 17,
            selectedStage.unlockedWorldTier() <= state.worldTier() ? 0xFFFFFFFF : VanillaUiStyle.TEXT, false);

        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static String formatScale(double value) {
        return Math.abs(value - Math.rint(value)) < 0.001D
            ? Long.toString(Math.round(value))
            : String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatCount(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, value));
    }

    private static String roman(int tier) {
        return switch (tier) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "I"; };
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
