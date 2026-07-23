package com.velorise.emc;

import com.velorise.emc.common.EmcSourceMode;
import com.velorise.emc.network.UpdateHubSettingsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class EmcHubScreen extends Screen {
    private EmcSourceMode sourceMode = EmcSourceMode.FUND_ONLY;
    private boolean hudEnabled = true;
    private boolean showLookedAtBlockEmc;
    private boolean showStackEmc;
    private boolean showWorldTier;
    private long emc;
    private long inventoryFuelBuffer;
    private int left;
    private int top;

    public EmcHubScreen() { super(Component.translatable("screen.velorise_emc.emc_hub")); }

    @Override
    protected void init() {
        left = (width - 260) / 2;
        top = (height - 236) / 2;
        showLookedAtBlockEmc = EmcHudPreferences.showLookedAtBlockEmc();
        showStackEmc = EmcHudPreferences.showStackEmc();
        showWorldTier = EmcHudPreferences.showWorldTier();
        PlayerEMC data = minecraft != null && minecraft.player != null
            ? minecraft.player.getData(VeloriseEMC.PLAYER_EMC.get()) : new PlayerEMC();
        emc = data.getEmc();
        sourceMode = data.getSourceMode();
        hudEnabled = data.isHudEnabled();
        inventoryFuelBuffer = data.getInventoryFuelBuffer();

        addRenderableWidget(Button.builder(hudLabel(), button -> {
            hudEnabled = !hudEnabled;
            button.setMessage(hudLabel());
        }).bounds(left + 122, top + 51, 122, 20).build());

        addRenderableWidget(Button.builder(worldTierLabel(), button -> {
            showWorldTier = !showWorldTier;
            EmcHudPreferences.setShowWorldTier(showWorldTier);
            EmcHudPreferences.save();
            button.setMessage(worldTierLabel());
        }).bounds(left + 122, top + 75, 122, 20).build());

        addRenderableWidget(Button.builder(stackEmcLabel(), button -> {
            showStackEmc = !showStackEmc;
            EmcHudPreferences.setShowStackEmc(showStackEmc);
            EmcHudPreferences.save();
            button.setMessage(stackEmcLabel());
        }).bounds(left + 122, top + 99, 122, 20).build());

        addRenderableWidget(Button.builder(heldItemLabel(), button -> {
            showLookedAtBlockEmc = !showLookedAtBlockEmc;
            EmcHudPreferences.setShowLookedAtBlockEmc(showLookedAtBlockEmc);
            EmcHudPreferences.save();
            button.setMessage(heldItemLabel());
        }).bounds(left + 122, top + 123, 122, 20).build());

        addRenderableWidget(Button.builder(scaleLabel(), button -> {
            EmcHudPreferences.cycleScale();
            EmcHudPreferences.save();
            button.setMessage(scaleLabel());
        }).bounds(left + 122, top + 147, 122, 20).build());

        addRenderableWidget(Button.builder(Component.literal("View Quests"), button -> {
            save();
            if (minecraft != null) minecraft.setScreen(new WorldProgressScreen(this));
        }).bounds(left + 20, top + 171, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Move HUD"), button -> {
            save();
            if (minecraft != null) minecraft.setScreen(new EmcHudLayoutScreen(this));
        }).bounds(left + 122, top + 171, 122, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
            .bounds(left + 20, top + 204, 102, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            save();
            onClose();
        }).bounds(left + 138, top + 204, 102, 20).build());
    }

    private Component hudLabel() {
        return Component.translatable(hudEnabled ? "screen.velorise_emc.hud_on" : "screen.velorise_emc.hud_off");
    }

    private Component worldTierLabel() {
        return Component.literal(showWorldTier ? "On" : "Off");
    }

    private Component stackEmcLabel() {
        return Component.literal(showStackEmc ? "On" : "Off");
    }

    private Component heldItemLabel() {
        return Component.literal(showLookedAtBlockEmc ? "On" : "Off");
    }

    private Component scaleLabel() {
        return Component.literal(EmcHudPreferences.scalePercent() + "%");
    }

    private void save() {
        PacketDistributor.sendToServer(new UpdateHubSettingsPayload(sourceMode.ordinal(), 0L, hudEnabled));
        EmcHudPreferences.setShowLookedAtBlockEmc(showLookedAtBlockEmc);
        EmcHudPreferences.setShowStackEmc(showStackEmc);
        EmcHudPreferences.setShowWorldTier(showWorldTier);
        EmcHudPreferences.save();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        VanillaUiStyle.drawPanel(graphics, left, top, 260, 236);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        VanillaUiStyle.drawCenteredText(graphics, font, title, width / 2, top + 8, VanillaUiStyle.TEXT);
        graphics.drawString(font, Component.translatable("screen.velorise_emc.balance", String.format("%,d", emc)), left + 20, top + 24, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.velorise_emc.inventory_buffer", String.format("%,d", inventoryFuelBuffer)), left + 20, top + 36, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.velorise_emc.hud"), left + 20, top + 57, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("World Tier"), left + 20, top + 81, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("Stack EMC"), left + 20, top + 105, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("Target block EMC"), left + 20, top + 129, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("HUD size"), left + 20, top + 153, VanillaUiStyle.TEXT, false);
        graphics.drawString(font, Component.literal("HUD position"), left + 20, top + 177, VanillaUiStyle.TEXT, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
