package com.velorise.simpleemc;

import com.velorise.simpleemc.network.ModMessages;
import com.velorise.simpleemc.network.RequestUpdateEMCMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EMCConfigScreen extends Screen {
    private final Screen parentScreen;
    private EditBox searchBox;
    private EditBox emcEditBox;
    private Button prevPageButton;
    private Button nextPageButton;
    private int currentPage = 0;
    private final List<Item> filteredItems = new ArrayList<>();
    private final Map<Item, Long> pendingEdits = new HashMap<>();
    private Item selectedItem = null;
    private boolean showMissingOnly = false;

    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 4;
    private static final int ITEMS_PER_PAGE = GRID_COLS * GRID_ROWS;

    private int guiLeft;
    private int guiTop;
    private final int xSize = 210;
    private final int ySize = 220;

    public EMCConfigScreen(Screen parentScreen) {
        super(Component.literal("EMC Config"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.searchBox = new EditBox(this.font, this.guiLeft + 12, this.guiTop + 20, 110, 14, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search..."));
        this.searchBox.setResponder(text -> {
            this.currentPage = 0;
            this.updateFilteredItems();
        });
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.literal(this.showMissingOnly ? "Show All" : "Missing Only"), btn -> {
            this.showMissingOnly = !this.showMissingOnly;
            btn.setMessage(Component.literal(this.showMissingOnly ? "Show All" : "Missing Only"));
            this.currentPage = 0;
            this.updateFilteredItems();
        }).bounds(this.guiLeft + 128, this.guiTop + 17, 70, 20).build());

        this.prevPageButton = Button.builder(Component.literal("<"), btn -> {
            if (this.currentPage > 0) {
                this.currentPage--;
            }
        }).bounds(this.guiLeft + 12, this.guiTop + 120, 20, 20).build();
        this.addRenderableWidget(this.prevPageButton);

        this.nextPageButton = Button.builder(Component.literal(">"), btn -> {
            if ((this.currentPage + 1) * ITEMS_PER_PAGE < this.filteredItems.size()) {
                this.currentPage++;
            }
        }).bounds(this.guiLeft + 178, this.guiTop + 120, 20, 20).build();
        this.addRenderableWidget(this.nextPageButton);

        this.emcEditBox = new EditBox(this.font, this.guiLeft + 80, this.guiTop + 155, 60, 14, Component.literal("EMC"));
        this.emcEditBox.setResponder(text -> {
            if (this.selectedItem != null) {
                try {
                    long val = text.isEmpty() ? 0L : Long.parseLong(text);
                    if (val >= 0) {
                        this.pendingEdits.put(this.selectedItem, val);
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        });
        this.emcEditBox.setVisible(false);
        this.addRenderableWidget(this.emcEditBox);

        Button resetBtn = Button.builder(Component.literal("Reset"), btn -> {
            if (this.selectedItem != null) {
                this.pendingEdits.put(this.selectedItem, 0L);
                this.emcEditBox.setValue("0");
            }
        }).bounds(this.guiLeft + 145, this.guiTop + 152, 45, 20).build();
        resetBtn.visible = false;
        this.addRenderableWidget(resetBtn);

        this.addRenderableWidget(Button.builder(Component.literal("Save & Apply"), btn -> {
            saveAndApply();
        }).bounds(this.guiLeft + 12, this.guiTop + 190, 85, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            this.minecraft.setScreen(this.parentScreen);
        }).bounds(this.guiLeft + 113, this.guiTop + 190, 85, 20).build());

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.getConnection() != null) {
            EMCRegistry.calculateAllRecipeEMC(
                mc.getConnection().getRecipeManager(),
                mc.level.registryAccess()
            );
        }

        this.updateFilteredItems();
    }

    private void updateFilteredItems() {
        this.filteredItems.clear();
        if (this.showMissingOnly && !EMCRegistry.hasRecipeCache()) {
            this.selectedItem = null;
            return;
        }
        String query = this.searchBox.getValue().toLowerCase();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == net.minecraft.world.item.Items.AIR) continue;
            
            if (this.showMissingOnly && EMCRegistry.hasEMC(item)) {
                continue;
            }
            
            String name = item.getDescription().getString().toLowerCase();
            String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
            if (name.contains(query) || id.contains(query)) {
                this.filteredItems.add(item);
            }
        }
    }

    private long getEffectiveEMC(Item item) {
        if (this.pendingEdits.containsKey(item)) {
            return this.pendingEdits.get(item);
        }
        return EMCRegistry.getEMC(item);
    }

    private int hoverItemIdx = -1;

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);

        int x = this.guiLeft;
        int y = this.guiTop;

        graphics.fill(x, y, x + this.xSize, y + this.ySize, 0xFFC6C6C6);
        graphics.fill(x, y, x + this.xSize, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + this.ySize, 0xFFFFFFFF);
        graphics.fill(x + this.xSize - 1, y, x + this.xSize, y + this.ySize, 0xFF555555);
        graphics.fill(x, y + this.ySize - 1, x + this.xSize, y + this.ySize, 0xFF555555);

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIndex + i;
            if (idx >= this.filteredItems.size()) break;
            int gridX = x + 12 + (i % GRID_COLS) * 20;
            int gridY = y + 42 + (i / GRID_COLS) * 20;
            graphics.fill(gridX, gridY, gridX + 18, gridY + 18, 0xFF8B8B8B);
            graphics.fill(gridX, gridY, gridX + 18, gridY + 1, 0xFF373737);
            graphics.fill(gridX, gridY, gridX + 1, gridY + 18, 0xFF373737);
            graphics.fill(gridX + 17, gridY, gridX + 18, gridY + 18, 0xFFFFFFFF);
            graphics.fill(gridX, gridY + 17, gridX + 18, gridY + 18, 0xFFFFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int x = this.guiLeft;
        int y = this.guiTop;
        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        boolean isNoticeActive = this.showMissingOnly && !EMCRegistry.hasRecipeCache();

        this.prevPageButton.visible = !isNoticeActive;
        this.nextPageButton.visible = !isNoticeActive;

        if (this.selectedItem != null && !isNoticeActive) {
            this.emcEditBox.setVisible(true);
            for (var widget : this.children()) {
                if (widget instanceof Button btn && btn.getMessage().getString().equals("Reset")) {
                    btn.visible = true;
                }
            }
        } else {
            this.emcEditBox.setVisible(false);
            for (var widget : this.children()) {
                if (widget instanceof Button btn && btn.getMessage().getString().equals("Reset")) {
                    btn.visible = false;
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(this.font, "EMC Config Editor", x + 12, y + 8, 0xFF404040, false);

        if (isNoticeActive) {
            int cx = x + this.xSize / 2;
            int cy = y + 90;
            graphics.fill(x + 10, y + 42, x + this.xSize - 10, y + 120, 0x7F000000);
            String line1 = "Recipe data not loaded yet.";
            String line2 = "Enter any world once to";
            String line3 = "generate the full EMC list.";
            graphics.drawString(this.font, line1, cx - this.font.width(line1) / 2, cy - 18, 0xFFFFFFAA, false);
            graphics.drawString(this.font, line2, cx - this.font.width(line2) / 2, cy - 6, 0xFFCCCCCC, false);
            graphics.drawString(this.font, line3, cx - this.font.width(line3) / 2, cy + 6, 0xFFCCCCCC, false);
            this.hoverItemIdx = -1;
        } else {
            this.hoverItemIdx = -1;
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int idx = startIndex + i;
                if (idx >= this.filteredItems.size()) break;

                Item item = this.filteredItems.get(idx);
                int gridX = x + 12 + (i % GRID_COLS) * 20;
                int gridY = y + 42 + (i / GRID_COLS) * 20;

                if (item == this.selectedItem) {
                    graphics.fill(gridX + 1, gridY + 1, gridX + 17, gridY + 17, 0x8070FF70);
                }

                graphics.renderFakeItem(new ItemStack(item), gridX + 1, gridY + 1);

                if (mouseX >= gridX && mouseX < gridX + 18 && mouseY >= gridY && mouseY < gridY + 18) {
                    this.hoverItemIdx = idx;
                    graphics.fill(gridX + 1, gridY + 1, gridX + 17, gridY + 17, 0x40FFFFFF);
                }
            }
        }

        if (!isNoticeActive) {
            int totalPages = (this.filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
            if (totalPages == 0) totalPages = 1;
            String pageStr = (this.currentPage + 1) + " / " + totalPages;
            int pageStrX = x + this.xSize / 2 - this.font.width(pageStr) / 2;
            graphics.drawString(this.font, pageStr, pageStrX, y + 126, 0xFF404040, false);
        }

        if (!isNoticeActive) {
            if (this.selectedItem != null) {
                graphics.renderFakeItem(new ItemStack(this.selectedItem), x + 12, y + 152);
                String itemName = this.selectedItem.getDescription().getString();
                if (itemName.length() > 22) itemName = itemName.substring(0, 19) + "...";
                graphics.drawString(this.font, itemName, x + 35, y + 148, 0xFF404040, false);
                graphics.drawString(this.font, "EMC:", x + 35, y + 159, 0xFF404040, false);
            } else {
                graphics.drawString(this.font, "Select an item to edit its EMC", x + 12, y + 155, 0xFF888888, false);
            }
        }

        if (this.hoverItemIdx != -1) {
            Item item = this.filteredItems.get(this.hoverItemIdx);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(item.getDescription());
            if (this.pendingEdits.containsKey(item)) {
                long pending = this.pendingEdits.get(item);
                tooltip.add(Component.literal("§eEMC: " + String.format("%,d", pending) + " §7(unsaved)"));
            } else if (EMCRegistry.hasEMC(item)) {
                tooltip.add(Component.literal("§eEMC: " + String.format("%,d", EMCRegistry.getEMC(item))));
            } else {
                tooltip.add(Component.literal("§cNo EMC – click to assign"));
            }
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.guiLeft;
        int y = this.guiTop;
        int startIndex = this.currentPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIndex + i;
            if (idx >= this.filteredItems.size()) break;

            int gridX = x + 12 + (i % GRID_COLS) * 20;
            int gridY = y + 42 + (i / GRID_COLS) * 20;

            if (mouseX >= gridX && mouseX < gridX + 18 && mouseY >= gridY && mouseY < gridY + 18) {
                Item item = this.filteredItems.get(idx);
                this.selectedItem = item;
                this.emcEditBox.setValue(String.valueOf(getEffectiveEMC(item)));
                this.emcEditBox.setFocused(true);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void saveAndApply() {
        if (!this.pendingEdits.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Map<ResourceLocation, Long> syncMap = new HashMap<>();
                this.pendingEdits.forEach((item, val) -> {
                    ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
                    syncMap.put(rl, val);
                });
                ModMessages.CHANNEL.sendToServer(new RequestUpdateEMCMessage(syncMap));
            } else {
                EMCRegistry.saveCustomEMC(this.pendingEdits);
                EMCRegistry.loadCustomEMC();
            }
        }
        this.minecraft.setScreen(this.parentScreen);
    }
}
