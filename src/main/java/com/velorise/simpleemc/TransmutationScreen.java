package com.velorise.simpleemc;

import com.velorise.simpleemc.network.RequestWithdrawPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TransmutationScreen extends AbstractContainerScreen<TransmutationMenu> {
    private EditBox searchBox;
    private int currentPage = 0;
    private final List<Item> filteredItems = new ArrayList<>();
    private boolean showAffordableOnly = false;
    
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 3;
    private static final int ITEMS_PER_PAGE = GRID_COLS * GRID_ROWS;
    
    // Positions of grid relative to GUI top-left
    private static final int GRID_X = 8;
    private static final int GRID_Y = 46;
    private static final int SLOT_SIZE = 18;

    public TransmutationScreen(TransmutationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192;
    }

    @Override
    protected void init() {
        super.init();
        
        // Search bar with placeholder hint
        this.searchBox = new EditBox(this.font, this.leftPos + 24, this.topPos + 6, 108, 12, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search...").withStyle(s -> s.withColor(0xFF888888)));
        this.searchBox.setResponder(text -> {
            this.currentPage = 0;
            this.updateFilteredItems();
        });
        this.searchBox.setBordered(true);
        this.searchBox.setTextColor(0xFF404040);
        this.addRenderableWidget(this.searchBox);

        // Previous Page Button
        this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (currentPage > 0) {
                currentPage--;
            }
        }).bounds(this.leftPos + 6, this.topPos + 6, 16, 12).build());

        // Next Page Button
        this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if ((currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size()) {
                currentPage++;
            }
        }).bounds(this.leftPos + 136, this.topPos + 6, 16, 12).build());

        // Toggle affordable-only filter button (rightmost, like Crafting Table)
        this.addRenderableWidget(Button.builder(Component.literal(showAffordableOnly ? "\u2714" : "\u2715"), btn -> {
            showAffordableOnly = !showAffordableOnly;
            btn.setMessage(Component.literal(showAffordableOnly ? "\u2714" : "\u2715"));
            currentPage = 0;
            updateFilteredItems();
        }).bounds(this.leftPos + 154, this.topPos + 6, 16, 12).build());

        this.updateFilteredItems();
    }

    private void updateFilteredItems() {
        this.filteredItems.clear();
        if (this.minecraft == null || this.minecraft.player == null) return;

        PlayerEMC data = this.minecraft.player.getData(SimpleEMC.PLAYER_EMC.get());
        String query = this.searchBox.getValue().toLowerCase();

        List<Item> learned = new ArrayList<>(data.getLearnedItems());
        // Sort alphabetically (A-Z) by display name
        learned.sort((item1, item2) -> {
            String name1 = Component.translatable(item1.getDescriptionId()).getString();
            String name2 = Component.translatable(item2.getDescriptionId()).getString();
            return name1.compareToIgnoreCase(name2);
        });

        for (Item item : learned) {
            String name = Component.translatable(item.getDescriptionId()).getString().toLowerCase();
            if (!name.contains(query)) continue;
            if (showAffordableOnly && data.getEmc() < EMCRegistry.getEMC(item)) continue;
            filteredItems.add(item);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Redraw filtered items list to stay up-to-date with attachment data changes
        this.updateFilteredItems();

        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Basic Minecraft-style Light Gray Panel Background
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        // Outer dark border
        this.renderOutline(graphics, x, y, this.imageWidth, this.imageHeight, 0xFF000000);
        // Inner highlights (White at top-left, Dark Gray at bottom-right)
        graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 2, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, y + this.imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + this.imageHeight - 2, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF555555);
        graphics.fill(x + this.imageWidth - 2, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF555555);

        // Draw input slot background (located at x: 150, y: 22) - Vanilla style
        graphics.fill(x + 150, y + 22, x + 150 + 16, y + 22 + 16, 0xFF8B8B8B);
        this.renderOutline(graphics, x + 150 - 1, y + 22 - 1, 18, 18, 0xFF373737);
        graphics.fill(x + 150 - 1, y + 22 + 17 - 1, x + 150 + 17, y + 22 + 17, 0xFFFFFFFF);
        graphics.fill(x + 150 + 17 - 1, y + 22 - 1, x + 150 + 17, y + 22 + 17, 0xFFFFFFFF);

        // Draw grid outline and individual slot boxes - Vanilla style
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
                // Top-left dark shadow
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY, 0xFF373737);
                graphics.fill(slotX - 1, slotY - 1, slotX, slotY + 17, 0xFF373737);
                // Bottom-right white highlight
                graphics.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, 0xFFFFFFFF);
                graphics.fill(slotX + 16, slotY - 1, slotX + 17, slotY + 17, 0xFFFFFFFF);
            }
        }

        // Draw Player inventory slots background - Vanilla style
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 8 + col * 18;
                int slotY = y + 106 + row * 18;
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY, 0xFF373737);
                graphics.fill(slotX - 1, slotY - 1, slotX, slotY + 17, 0xFF373737);
                graphics.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, 0xFFFFFFFF);
                graphics.fill(slotX + 16, slotY - 1, slotX + 17, slotY + 17, 0xFFFFFFFF);
            }
        }

        // Draw Hotbar slots background - Vanilla style
        for (int col = 0; col < 9; col++) {
            int slotX = x + 8 + col * 18;
            int slotY = y + 166;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY, 0xFF373737);
            graphics.fill(slotX - 1, slotY - 1, slotX, slotY + 17, 0xFF373737);
            graphics.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, 0xFFFFFFFF);
            graphics.fill(slotX + 16, slotY - 1, slotX + 17, slotY + 17, 0xFFFFFFFF);
        }
    }

    private void renderOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);           // top
        graphics.fill(x, y + height - 1, x + width, y + height, color); // bottom
        graphics.fill(x, y, x + 1, y + height, color);           // left
        graphics.fill(x + width - 1, y, x + width, y + height, color);  // right
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        // Player's current EMC
        PlayerEMC data = this.minecraft.player.getData(SimpleEMC.PLAYER_EMC.get());
        String emcStr = "EMC: " + String.format("%,d", data.getEmc());
        graphics.drawString(this.font, emcStr, 8, 26, 0xFF404040, false);

        // Render furnace fire icon instead of "Đốt:" text
        net.minecraft.resources.ResourceLocation FURNACE_LOCATION = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");
        graphics.blit(FURNACE_LOCATION, 132, 24, 56, 36, 14, 14);

        // Render learned items in grid
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

            long cost = EMCRegistry.getEMC(item);
            boolean canAfford = data.getEmc() >= cost;

            int mX = mouseX - x;
            int mY = mouseY - y;
            boolean hovering = mX >= drawX && mX < drawX + 16 && mY >= drawY && mY < drawY + 16;

            if (!canAfford) {
                // Red tint overlay - dimmed to show unavailable
                graphics.fill(drawX, drawY, drawX + 16, drawY + 16, 0x99CC0000);
                // Red border around slot (4 lines: top, bottom, left, right)
                graphics.fill(drawX - 1, drawY - 1, drawX + 17, drawY,     0xFFFF3333); // top
                graphics.fill(drawX - 1, drawY + 16, drawX + 17, drawY + 17, 0xFFFF3333); // bottom
                graphics.fill(drawX - 1, drawY - 1, drawX,     drawY + 17, 0xFFFF3333); // left
                graphics.fill(drawX + 16, drawY - 1, drawX + 17, drawY + 17, 0xFFFF3333); // right
            } else if (hovering) {
                // White hover highlight when affordable
                graphics.fill(drawX, drawY, drawX + 16, drawY + 16, 0x80FFFFFF);
            }
        }

    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        // Check if hovering over grid items
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
                
                graphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchBox);
            return true;
        } else {
            this.setFocused(null);
        }

        // Check if click is on grid items
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
                long cost = EMCRegistry.getEMC(item);

                if (this.minecraft != null && this.minecraft.player != null) {
                    PlayerEMC data = this.minecraft.player.getData(SimpleEMC.PLAYER_EMC.get());
                    if (data.getEmc() >= cost) {
                        // Send withdraw packet to Server (right-click is button == 1)
                        boolean withdrawStack = (button == 1);
                        PacketDistributor.sendToServer(new RequestWithdrawPayload(BuiltInRegistries.ITEM.getKey(item), withdrawStack, false));
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.searchBox.isFocused()) {
            return false; // Prevent closing the inventory screen when typing 'E'
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
