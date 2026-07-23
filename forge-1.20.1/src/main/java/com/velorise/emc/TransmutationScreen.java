package com.velorise.emc;

import com.velorise.emc.capability.PlayerEMCCapability;
import com.velorise.emc.network.ModMessages;
import com.velorise.emc.network.RequestWithdrawMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class TransmutationScreen extends AbstractContainerScreen<TransmutationMenu> {
    private EditBox searchBox;
    private int currentPage = 0;
    private final List<Item> filteredItems = new ArrayList<>();
    private boolean showAffordableOnly = false;
    private Button worldTierQuestButton;

    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 4;
    private static final int ITEMS_PER_PAGE = GRID_COLS * GRID_ROWS;

    private static final int GRID_X = 8;
    private static final int GRID_Y = 26;
    private static final int SLOT_SIZE = 18;

    public TransmutationScreen(TransmutationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 202;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();

        // Extended search bar to fit top UI
        this.searchBox = new EditBox(this.font, this.leftPos + 22, this.topPos + 6, 142, 12, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search...").withStyle(style -> style.withColor(VanillaUiStyle.DISABLED_TEXT)));
        this.searchBox.setResponder(text -> {
            this.currentPage = 0;
            this.updateFilteredItems();
        });
        this.searchBox.setBordered(true);
        this.searchBox.setTextColor(VanillaUiStyle.INPUT_TEXT);
        this.addRenderableWidget(this.searchBox);

        // Previous Page Button
        this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (currentPage > 0) {
                currentPage--;
            }
        }).bounds(this.leftPos + 6, this.topPos + 6, 14, 12).build());

        // Next Page Button
        this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if ((currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size()) {
                currentPage++;
            }
        }).bounds(this.leftPos + 166, this.topPos + 6, 14, 12).build());

        // Toggle affordable-only filter button
        this.addRenderableWidget(Button.builder(Component.literal(showAffordableOnly ? "\u2714" : "\u2715"), btn -> {
            showAffordableOnly = !showAffordableOnly;
            btn.setMessage(Component.literal(showAffordableOnly ? "\u2714" : "\u2715"));
            currentPage = 0;
            updateFilteredItems();
        }).bounds(this.leftPos + 182, this.topPos + 6, 14, 12).build());

        // Book button positioned at top of right inset panel (x = 168, y = 30)
        this.worldTierQuestButton = this.addRenderableWidget(Button.builder(Component.empty(), btn -> openWorldTierQuests())
            .bounds(this.leftPos + 168, this.topPos + 30, 18, 18).build());

        this.updateFilteredItems();
    }

    private void updateFilteredItems() {
        this.filteredItems.clear();
        if (this.minecraft == null || this.minecraft.player == null) return;

        PlayerEMC data = this.minecraft.player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).orElse(null);
        if (data == null) return;

        String query = this.searchBox.getValue().toLowerCase();

        List<Item> learned = new ArrayList<>(data.getLearnedItems());
        learned.removeIf(item -> !EMCRegistry.canMaterialize(item));
        learned.sort((item1, item2) -> {
            String name1 = Component.translatable(item1.getDescriptionId()).getString();
            String name2 = Component.translatable(item2.getDescriptionId()).getString();
            return name1.compareToIgnoreCase(name2);
        });

        for (Item item : learned) {
            String name = Component.translatable(item.getDescriptionId()).getString().toLowerCase();
            if (!name.contains(query)) continue;
            long unitCost = EMCRegistry.getEMC(item);
            if (showAffordableOnly
                && data.getEmc() < TransmutationTaxService.quote(data, unitCost, 1).totalCost()) continue;
            filteredItems.add(item);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.updateFilteredItems();

        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.worldTierQuestButton != null) {
            graphics.renderFakeItem(new ItemStack(Items.KNOWLEDGE_BOOK), this.worldTierQuestButton.getX() + 1, this.worldTierQuestButton.getY() + 1);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.worldTierQuestButton != null && this.worldTierQuestButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, java.util.List.of(Component.literal("World Tier Quests & Hub")), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        VanillaUiStyle.drawPanel(graphics, x, y, this.imageWidth, this.imageHeight);
        VanillaUiStyle.drawInset(graphics, x + 6, y + 24, 148, 76);
        VanillaUiStyle.drawInset(graphics, x + 158, y + 24, 38, 76);
        VanillaUiStyle.drawInset(graphics, x + 18, y + 124, 166, 88);
        VanillaUiStyle.drawSlot(graphics, x + 169, y + 52);

        // Render custom pure transparent fire icon directly below burn slot (169, 52) at (170, 74)
        ResourceLocation BURN_FLAME = new ResourceLocation("velorise_emc", "textures/gui/burn_flame.png");
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.blit(BURN_FLAME, x + 170, y + 74, 0.0F, 0.0F, 14, 14, 14, 14);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                VanillaUiStyle.drawSlot(graphics,
                    x + GRID_X + col * SLOT_SIZE,
                    y + GRID_Y + row * SLOT_SIZE);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                VanillaUiStyle.drawSlot(graphics, x + 20 + col * 18, y + 130 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            VanillaUiStyle.drawSlot(graphics, x + 20 + col * 18, y + 186);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        PlayerEMC data = this.minecraft.player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).orElse(null);
        if (data == null) return;

        String emcStr = "EMC: " + EmcTextFormat.compact(data.getEmc());
        graphics.drawString(this.font, emcStr, 20, 108, VanillaUiStyle.TEXT, false);
        String taxStr = "Tax: +" + TransmutationTaxService.currentSurchargePerItem(data);
        graphics.drawString(this.font, taxStr, 182 - this.font.width(taxStr), 108, VanillaUiStyle.MUTED_TEXT, false);

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int x = this.leftPos;
        int y = this.topPos;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;

            Item item = filteredItems.get(idx);
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;

            int drawX = GRID_X + col * SLOT_SIZE;
            int drawY = GRID_Y + row * SLOT_SIZE;

            graphics.renderFakeItem(new ItemStack(item), drawX, drawY);

            long unitCost = EMCRegistry.getEMC(item);
            long totalCost = TransmutationTaxService.quote(data, unitCost, 1).totalCost();
            boolean canAfford = data.getEmc() >= totalCost;

            int mX = mouseX - x;
            int mY = mouseY - y;
            boolean hovering = mX >= drawX && mX < drawX + 16 && mY >= drawY && mY < drawY + 16;

            if (!canAfford) {
                graphics.fill(drawX, drawY, drawX + 16, drawY + 16, VanillaUiStyle.ITEM_UNAFFORDABLE_OVERLAY);
                graphics.fill(drawX, drawY + 15, drawX + 16, drawY + 16, VanillaUiStyle.STATUS_ERROR);
            } else if (hovering) {
                graphics.fill(drawX, drawY, drawX + 16, drawY + 16, VanillaUiStyle.ITEM_HOVER_OVERLAY);
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        int taxMouseX = mouseX - this.leftPos;
        int taxMouseY = mouseY - this.topPos;
        PlayerEMC taxData = this.minecraft == null || this.minecraft.player == null ? null
            : this.minecraft.player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).orElse(null);
        String taxLabel = taxData == null ? "" : "Tax: +" + TransmutationTaxService.currentSurchargePerItem(taxData);
        int taxLabelX = 182 - this.font.width(taxLabel);
        if (taxData != null && taxMouseX >= taxLabelX && taxMouseX < 182
            && taxMouseY >= 110 && taxMouseY < 123) {
            graphics.renderComponentTooltip(this.font, TransmutationTaxService.meterTooltip(taxData), mouseX, mouseY);
            return;
        }

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int x = this.leftPos;
        int y = this.topPos;
        int mX = mouseX - x;
        int mY = mouseY - y;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;

            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int slotX = GRID_X + col * SLOT_SIZE;
            int slotY = GRID_Y + row * SLOT_SIZE;

            if (mX >= slotX && mX < slotX + 16 && mY >= slotY && mY < slotY + 16) {
                Item item = filteredItems.get(idx);
                ItemStack stack = new ItemStack(item);
                List<Component> tooltipLines = this.getTooltipFromContainerItem(stack);
                PlayerEMC data = this.minecraft.player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).orElse(null);
                if (data != null) {
                    long unitCost = EMCRegistry.getEMC(item);
                    var quote = TransmutationTaxService.quote(data, unitCost, 1);
                    tooltipLines.add(Component.literal("§7Cost: §f" + String.format("%,d", quote.totalCost()) + " EMC"));
                }

                graphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
                break;
            }
        }
    }

    private void openWorldTierQuests() {
        if (this.minecraft == null) return;
        this.minecraft.setScreen(new WorldProgressScreen(this));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchBox);
            return true;
        } else {
            this.setFocused(null);
        }

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int x = this.leftPos;
        int y = this.topPos;
        int mX = (int) mouseX - x;
        int mY = (int) mouseY - y;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= filteredItems.size()) break;

            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int slotX = GRID_X + col * SLOT_SIZE;
            int slotY = GRID_Y + row * SLOT_SIZE;

            if (mX >= slotX && mX < slotX + 16 && mY >= slotY && mY < slotY + 16) {
                Item item = filteredItems.get(idx);
                long unitCost = EMCRegistry.getEMC(item);

                if (this.minecraft != null && this.minecraft.player != null) {
                    PlayerEMC data = this.minecraft.player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).orElse(null);
                    long totalCost = data == null ? Long.MAX_VALUE
                        : TransmutationTaxService.quote(data, unitCost, 1).totalCost();
                    if (data != null && data.getEmc() >= totalCost) {
                        boolean withdrawStack = (button == 1);
                        ModMessages.CHANNEL.sendToServer(new RequestWithdrawMessage(BuiltInRegistries.ITEM.getKey(item), withdrawStack, false));
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            this.onClose();
            return true;
        }
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.searchBox.isFocused() && this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true; // Prevent closing screen when typing 'E' in search box
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
