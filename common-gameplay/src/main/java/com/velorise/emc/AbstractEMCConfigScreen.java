package com.velorise.emc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velorise.emc.common.EmcItemPolicy;
import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcPolicyProfile;
import com.velorise.emc.common.EmcValueMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loader-neutral server EMC settings screen. It intentionally uses standard
 * Minecraft widgets and the restrained gray container palette used by every
 * Velorise EMC block screen.
 */
public abstract class AbstractEMCConfigScreen extends Screen {
    protected enum Page {
        OVERVIEW("Overview"),
        RULES("Item Rules"),
        VALUES("EMC Values"),
        BLACKLIST("Blacklist"),
        REVIEW("Review");

        final String label;
        Page(String label) { this.label = label; }
    }

    protected enum Filter {
        ALL("All"),
        RESTRICTED("Restricted"),
        CUSTOM("Custom"),
        HARD_LOCKED("Hard Locked");

        final String label;
        Filter(String label) { this.label = label; }
        Filter next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static final int PANEL_WIDTH = 326;
    private static final int PANEL_HEIGHT = 252;
    private static final int LIST_ROWS = 4;
    private static final int ROW_HEIGHT = 28;
    private static final int LIST_Y = 69;
    private static final int SCROLLBAR_X = 306;
    private static final int SCROLLBAR_HEIGHT = LIST_ROWS * ROW_HEIGHT;
    private static final Path SETTINGS_TRANSFER_FILE = Path.of("config", "velorise_emc", "server_settings_export.json");
    private static final Gson SETTINGS_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int UI_TEXT = VanillaUiStyle.TEXT;
    private static final int UI_MUTED = VanillaUiStyle.MUTED_TEXT;

    protected final Screen parentScreen;
    protected final EmcValueMode initialValueMode;
    protected EmcValueMode selectedValueMode;

    protected final Map<Item, Long> originalCustomEmc;
    protected final Map<Item, Long> customEmc;
    protected final Map<Item, EmcPolicyOverride> originalPolicies;
    protected final Map<Item, EmcPolicyOverride> policies;

    protected Page page = Page.OVERVIEW;
    protected Filter filter = Filter.ALL;
    protected int left;
    protected int top;
    protected int currentPage;
    protected Item selectedItem;
    protected String searchText = "";

    protected final List<Item> filteredItems = new ArrayList<>();
    protected EditBox searchBox;
    protected EditBox emcBox;
    protected Button previousButton;
    protected Button nextButton;
    protected Button materializeButton;
    protected Button resetRuleButton;
    protected Button blockAllButton;
    protected Button resetBlacklistButton;
    protected Button applyEmcButton;
    protected Button clearEmcButton;
    protected Button saveButton;
    protected boolean canEditServer = true;
    private boolean draggingScrollbar;

    protected AbstractEMCConfigScreen(Screen parentScreen) {
        super(Component.literal("Velorise EMC Server Settings"));
        this.parentScreen = parentScreen;
        this.initialValueMode = EMCRegistry.getEffectiveValueMode();
        this.selectedValueMode = this.initialValueMode;
        this.originalCustomEmc = new LinkedHashMap<>(EMCRegistry.getEffectiveCustomOverrides());
        this.customEmc = new LinkedHashMap<>(this.originalCustomEmc);
        this.originalPolicies = new LinkedHashMap<>(EMCRegistry.getEffectivePolicyOverrides());
        this.originalPolicies.keySet().removeIf(AbstractEMCConfigScreen::isVeloriseEmcItem);
        this.policies = new LinkedHashMap<>(this.originalPolicies);
    }

    protected abstract void sendServerUpdate(
        Map<ResourceLocation, Long> emcOverrides,
        EmcValueMode valueMode,
        Map<ResourceLocation, EmcPolicyOverride> policyOverrides
    );

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        Minecraft minecraft = Minecraft.getInstance();
        this.canEditServer = minecraft.player == null || minecraft.player.hasPermissions(2);
        buildPage();
    }

    protected void buildPage() {
        this.clearWidgets();
        addNavigation();

        switch (this.page) {
            case OVERVIEW -> addOverviewWidgets();
            case RULES -> addRulesWidgets();
            case VALUES -> addValuesWidgets();
            case BLACKLIST -> addBlacklistWidgets();
            case REVIEW -> addReviewWidgets();
        }

        this.addRenderableWidget(Button.builder(Component.literal("Export JSON"), button -> exportSettings())
            .bounds(this.left + 12, this.top + 224, 72, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Import JSON"), button -> importSettings())
            .bounds(this.left + 88, this.top + 224, 72, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
            .bounds(this.left + 170, this.top + 224, 68, 20).build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save & Apply"), button -> saveAndApply())
            .bounds(this.left + 242, this.top + 224, 76, 20).build());
        this.saveButton.active = this.canEditServer;

        refreshFilteredItems();
        updateSelectionButtons();
    }

    private void addNavigation() {
        int x = this.left + 8;
        int y = this.top + 18;
        int[] widths = {56, 62, 66, 58, 54};
        Page[] pages = Page.values();
        for (int i = 0; i < pages.length; i++) {
            Page target = pages[i];
            String text = target == this.page ? "[" + target.label + "]" : target.label;
            this.addRenderableWidget(Button.builder(Component.literal(text), button -> {
                this.page = target;
                this.currentPage = 0;
                this.selectedItem = null;
                buildPage();
            }).bounds(x, y, widths[i], 20).build());
            x += widths[i] + 2;
        }
    }

    private void addOverviewWidgets() {
        this.addRenderableWidget(Button.builder(modeButtonText(EmcValueMode.DEFAULT), button -> {
            this.selectedValueMode = EmcValueMode.DEFAULT;
            buildPage();
        }).bounds(this.left + 22, this.top + 58, 135, 24).build());

        this.addRenderableWidget(Button.builder(modeButtonText(EmcValueMode.BALANCED), button -> {
            this.selectedValueMode = EmcValueMode.BALANCED;
            buildPage();
        }).bounds(this.left + 169, this.top + 58, 135, 24).build());

        this.addRenderableWidget(Button.builder(Component.literal("Manage Item Rules"), button -> openPage(Page.RULES))
            .bounds(this.left + 22, this.top + 172, 135, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Custom EMC Values"), button -> openPage(Page.VALUES))
            .bounds(this.left + 169, this.top + 172, 135, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Blacklist"), button -> openPage(Page.BLACKLIST))
            .bounds(this.left + 22, this.top + 196, 135, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Review Changes"), button -> openPage(Page.REVIEW))
            .bounds(this.left + 169, this.top + 196, 135, 20).build());
    }

    private Component modeButtonText(EmcValueMode mode) {
        return Component.literal((this.selectedValueMode == mode ? "[x] " : "[ ] ")
            + (mode == EmcValueMode.DEFAULT ? "Default" : "Balanced"));
    }

    private void addRulesWidgets() {
        addSearchAndFilter();
        addPager();
        this.materializeButton = this.addRenderableWidget(Button.builder(Component.literal("Materialize: Inherit"), button -> cycleMaterialization())
            .bounds(this.left + 12, this.top + 193, 150, 20).build());
        this.resetRuleButton = this.addRenderableWidget(Button.builder(Component.literal("Reset Rule"), button -> resetSelectedPolicy())
            .bounds(this.left + 166, this.top + 193, 76, 20).build());
        this.blockAllButton = this.addRenderableWidget(Button.builder(Component.literal("Block All"), button -> blockSelectedAll())
            .bounds(this.left + 246, this.top + 193, 72, 20).build());
    }

    private void addValuesWidgets() {
        addSearchAndFilter();
        addPager();
        this.emcBox = new EditBox(this.font, this.left + 12, this.top + 194, 92, 18, Component.literal("Custom EMC"));
        this.emcBox.setMaxLength(18);
        this.emcBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        this.addRenderableWidget(this.emcBox);
        this.applyEmcButton = this.addRenderableWidget(Button.builder(Component.literal("Set Override"), button -> applyEmcOverride())
            .bounds(this.left + 108, this.top + 193, 92, 20).build());
        this.clearEmcButton = this.addRenderableWidget(Button.builder(Component.literal("Clear Override"), button -> clearEmcOverride())
            .bounds(this.left + 204, this.top + 193, 114, 20).build());
    }

    private void addBlacklistWidgets() {
        addSearchAndFilter();
        addPager();
        this.blockAllButton = this.addRenderableWidget(Button.builder(Component.literal("Block Everything"), button -> blockSelectedAll())
            .bounds(this.left + 46, this.top + 193, 122, 20).build());
        this.resetBlacklistButton = this.addRenderableWidget(Button.builder(Component.literal("Remove Server Block"), button -> resetSelectedPolicy())
            .bounds(this.left + 172, this.top + 193, 122, 20).build());
    }

    private void addReviewWidgets() {
        this.addRenderableWidget(Button.builder(Component.literal("Back to Overview"), button -> openPage(Page.OVERVIEW))
            .bounds(this.left + 12, this.top + 193, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset Draft"), button -> resetDraft())
            .bounds(this.left + 136, this.top + 193, 90, 20).build());
    }

    private void addSearchAndFilter() {
        this.searchBox = new EditBox(this.font, this.left + 12, this.top + 47, 184, 18, Component.literal("Search items"));
        this.searchBox.setHint(Component.literal("Search name or ID..."));
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            this.currentPage = 0;
            refreshFilteredItems();
        });
        this.addRenderableWidget(this.searchBox);
        this.addRenderableWidget(Button.builder(Component.literal("Filter: " + this.filter.label), button -> {
            this.filter = this.filter.next();
            this.currentPage = 0;
            buildPage();
        }).bounds(this.left + 200, this.top + 46, 118, 20).build());
    }

    private void addPager() {
        // The list itself owns scrolling. Page buttons used to occupy the final item row.
        this.previousButton = null;
        this.nextButton = null;
    }

    private void openPage(Page target) {
        this.page = target;
        this.currentPage = 0;
        this.selectedItem = null;
        buildPage();
    }

    protected void refreshFilteredItems() {
        this.filteredItems.clear();
        if (this.page == Page.OVERVIEW || this.page == Page.REVIEW) return;
        String query = this.searchText == null ? "" : this.searchText.trim().toLowerCase();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null || item == Items.AIR) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            String name = item.getDescription().getString().toLowerCase();
            if (!query.isEmpty() && !name.contains(query) && !id.toString().toLowerCase().contains(query)) continue;
            if (!matchesFilter(item)) continue;
            this.filteredItems.add(item);
        }
        this.filteredItems.sort(Comparator.comparing(item -> item.getDescription().getString().toLowerCase()));
        int maxScroll = Math.max(0, this.filteredItems.size() - LIST_ROWS);
        if (this.currentPage > maxScroll) this.currentPage = maxScroll;
        updatePager();
    }

    private boolean matchesFilter(Item item) {
        return switch (this.filter) {
            case ALL -> true;
            case RESTRICTED -> !previewPolicy(item).materializable();
            case CUSTOM -> this.policies.containsKey(item) || this.customEmc.containsKey(item);
            case HARD_LOCKED -> EMCRegistry.isHardBlocked(item) && !isVeloriseEmcItem(item);
        };
    }

    protected EmcItemPolicy previewPolicy(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        long emc = effectiveEmc(item);
        return EmcPolicyProfile.resolve(
            this.selectedValueMode,
            id == null ? "" : id.toString(),
            EMCRegistry.isHardBlocked(item),
            emc > 0L,
            this.policies.getOrDefault(item, EmcPolicyOverride.inherit())
        );
    }

    protected long effectiveEmc(Item item) {
        Long custom = this.customEmc.get(item);
        return custom != null ? custom : EMCRegistry.getEMC(item);
    }

    private void updatePager() {
        this.currentPage = Math.max(0, Math.min(maxScroll(), this.currentPage));
    }

    protected void updateSelectionButtons() {
        boolean selected = this.selectedItem != null;
        boolean ownItem = selected && isVeloriseEmcItem(this.selectedItem);
        boolean hard = selected && !ownItem && EMCRegistry.isHardBlocked(this.selectedItem);
        EmcPolicyOverride override = selected
            ? this.policies.getOrDefault(this.selectedItem, EmcPolicyOverride.inherit())
            : EmcPolicyOverride.inherit();

        if (this.materializeButton != null) {
            this.materializeButton.active = selected && !hard && !ownItem;
            String state = switch (override.materializableState()) {
                case EmcPolicyOverride.ALLOW -> "Allowed";
                case EmcPolicyOverride.DENY -> "Blocked";
                default -> "Inherit";
            };
            this.materializeButton.setMessage(Component.literal("Materialize: " + state));
        }
        if (this.resetRuleButton != null) this.resetRuleButton.active = selected && !hard && !override.isInherited();
        if (this.blockAllButton != null) this.blockAllButton.active = selected && !hard && !ownItem;
        if (this.resetBlacklistButton != null) this.resetBlacklistButton.active = selected && !hard && !override.isInherited();
        if (this.applyEmcButton != null) this.applyEmcButton.active = selected && !hard;
        if (this.clearEmcButton != null) this.clearEmcButton.active = selected && this.customEmc.containsKey(this.selectedItem);

        if (this.emcBox != null) {
            this.emcBox.setEditable(selected && !hard);
            if (selected && !this.emcBox.isFocused()) this.emcBox.setValue(String.valueOf(effectiveEmc(this.selectedItem)));
            if (!selected) this.emcBox.setValue("");
        }
    }

    private void cycleMaterialization() {
        if (this.selectedItem == null || EMCRegistry.isHardBlocked(this.selectedItem) || isVeloriseEmcItem(this.selectedItem)) return;
        EmcPolicyOverride current = this.policies.getOrDefault(this.selectedItem, EmcPolicyOverride.inherit());
        int next = switch (current.materializableState()) {
            case EmcPolicyOverride.INHERIT -> EmcPolicyOverride.DENY;
            case EmcPolicyOverride.DENY -> EmcPolicyOverride.ALLOW;
            default -> EmcPolicyOverride.INHERIT;
        };
        EmcPolicyOverride updated = current.withMaterializable(next);
        putPolicy(this.selectedItem, updated);
        updateSelectionButtons();
        refreshFilteredItems();
    }

    private void blockSelectedAll() {
        if (this.selectedItem == null || EMCRegistry.isHardBlocked(this.selectedItem) || isVeloriseEmcItem(this.selectedItem)) return;
        this.policies.put(this.selectedItem, EmcPolicyOverride.blockAll());
        updateSelectionButtons();
        refreshFilteredItems();
    }

    private void resetSelectedPolicy() {
        if (this.selectedItem == null || EMCRegistry.isHardBlocked(this.selectedItem)) return;
        this.policies.remove(this.selectedItem);
        updateSelectionButtons();
        refreshFilteredItems();
    }

    private void putPolicy(Item item, EmcPolicyOverride override) {
        if (isVeloriseEmcItem(item) || override == null || override.isInherited()) this.policies.remove(item);
        else this.policies.put(item, override);
    }

    private void applyEmcOverride() {
        if (this.selectedItem == null || EMCRegistry.isHardBlocked(this.selectedItem) || this.emcBox == null) return;
        try {
            long value = Long.parseLong(this.emcBox.getValue());
            if (value > 0L) this.customEmc.put(this.selectedItem, value);
        } catch (NumberFormatException ignored) {
            return;
        }
        updateSelectionButtons();
        refreshFilteredItems();
    }

    private void clearEmcOverride() {
        if (this.selectedItem == null) return;
        this.customEmc.remove(this.selectedItem);
        if (this.emcBox != null) this.emcBox.setValue(String.valueOf(EMCRegistry.getEMC(this.selectedItem)));
        updateSelectionButtons();
        refreshFilteredItems();
    }

    private void resetDraft() {
        this.selectedValueMode = this.initialValueMode;
        this.customEmc.clear();
        this.customEmc.putAll(this.originalCustomEmc);
        this.policies.clear();
        this.policies.putAll(this.originalPolicies);
        this.page = Page.OVERVIEW;
        this.selectedItem = null;
        this.currentPage = 0;
        buildPage();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRenderCompat.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        VanillaUiStyle.drawPanel(graphics, this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT);
        VanillaUiStyle.drawInset(graphics, this.left + 6, this.top + 42, PANEL_WIDTH - 12, 176);

        VanillaUiStyle.drawCenteredText(graphics, this.font, this.title, this.width / 2, this.top + 7, UI_TEXT);

        switch (this.page) {
            case OVERVIEW -> renderOverview(graphics);
            case RULES, VALUES, BLACKLIST -> renderItemPage(graphics, mouseX, mouseY);
            case REVIEW -> renderReview(graphics);
        }

        // Do not call Screen#render here. On newer clients that method may run the
        // menu-background blur shader after our custom panel has already been drawn,
        // which leaves only vanilla widgets sharp. Render the registered widgets
        // directly so the whole settings panel stays in the same render pass.
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        renderHoveredTooltip(graphics, mouseX, mouseY);
    }

    private void renderOverview(GuiGraphics graphics) {
        int x = this.left + 14;
        int y = this.top + 91;
        EmcSummary summary = calculateSummary();

        String description = this.selectedValueMode == EmcValueMode.DEFAULT
            ? "Default profile. All normal EMC items open."
            : "Balanced profile. Progression items restricted.";
        VanillaUiStyle.drawCenteredText(graphics, this.font, Component.literal(description), this.width / 2, y, UI_MUTED);

        VanillaUiStyle.drawInset(graphics, x, y + 14, PANEL_WIDTH - 28, 61);
        graphics.drawString(this.font, Component.literal("Economy Mode: " + this.selectedValueMode.displayName()), x + 8, y + 22, UI_TEXT, false);
        graphics.drawString(this.font, Component.literal("Items with EMC: " + summary.emcItems), x + 8, y + 35, UI_TEXT, false);
        graphics.drawString(this.font, Component.literal("Materializable: " + summary.materializable), x + 155, y + 35, UI_TEXT, false);
        graphics.drawString(this.font, Component.literal("Restricted: " + summary.restricted), x + 8, y + 48, UI_TEXT, false);
        graphics.drawString(this.font, Component.literal("Custom rules: " + this.policies.size()), x + 155, y + 48, UI_TEXT, false);
        graphics.drawString(this.font, Component.literal("Custom EMC: " + this.customEmc.size()), x + 8, y + 61, UI_TEXT, false);
        if (!this.canEditServer) {
            graphics.drawString(this.font, "Operator permission is required to apply server settings.",
                x + 8, y + 74, VanillaUiStyle.STATUS_WARN, false);
        }
    }

    private void renderItemPage(GuiGraphics graphics, int mouseX, int mouseY) {
        int start = this.currentPage;
        int listX = this.left + 12;
        int listY = this.top + LIST_Y;
        int rowWidth = PANEL_WIDTH - 40;

        for (int row = 0; row < LIST_ROWS; row++) {
            int index = start + row;
            int y = listY + row * ROW_HEIGHT;
            if (index >= this.filteredItems.size()) break;
            Item item = this.filteredItems.get(index);
            boolean hovered = mouseX >= listX && mouseX < listX + rowWidth && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            boolean selected = item == this.selectedItem;
            int fill = selected ? 0xFF3D5A80 : hovered ? 0xFF5A606D : 0xFF424752;
            graphics.fill(listX, y, listX + rowWidth, y + ROW_HEIGHT - 2, fill);
            graphics.renderFakeItem(new ItemStack(item), listX + 2, y + 5);

            String name = item.getDescription().getString();
            if (name.length() > 14) name = name.substring(0, 12) + "...";
            graphics.drawString(this.font, name, listX + 22, y + 3, 0xFFFFFFFF, false);
            ResourceLocation rowId = BuiltInRegistries.ITEM.getKey(item);
            String registryName = rowId == null ? "unknown" : rowId.toString();
            if (registryName.length() > 17) registryName = registryName.substring(0, 15) + "...";
            graphics.drawString(this.font, registryName, listX + 22, y + 15, 0xFFC6C6C6, false);

            long emc = effectiveEmc(item);
            String value = emc > 0L ? String.format("%,d", emc) : "No EMC";
            graphics.drawString(this.font, value, listX + 138, y + 9, 0xFFFFFFFF, false);

            EmcItemPolicy policy = previewPolicy(item);
            String status;
            int statusColor;
            if (EMCRegistry.isHardBlocked(item)) {
                status = "Hard Locked";
                statusColor = 0xFFFF5555;
            } else if (!policy.materializable()) {
                status = "Blocked";
                statusColor = 0xFFFFAA00;
            } else {
                status = "Allowed";
                statusColor = 0xFF55FF55;
            }
            graphics.drawString(this.font, status, listX + 204, y + 9, statusColor, false);
        }
        renderScrollbar(graphics);
    }

    private void renderReview(GuiGraphics graphics) {
        int x = this.left + 16;
        int y = this.top + 54;
        int emcChanges = countMapChanges(this.originalCustomEmc, this.customEmc);
        int policyChanges = countMapChanges(this.originalPolicies, this.policies);
        EmcSummary summary = calculateSummary();

        graphics.drawString(this.font, "Pending server changes", x, y, UI_TEXT, false);
        y += 18;
        graphics.drawString(this.font, "Mode: " + this.initialValueMode.displayName() + " -> " + this.selectedValueMode.displayName(), x, y, UI_TEXT, false);
        y += 14;
        graphics.drawString(this.font, "Custom EMC changes: " + emcChanges, x, y, UI_TEXT, false);
        y += 14;
        graphics.drawString(this.font, "Item rule changes: " + policyChanges, x, y, UI_TEXT, false);
        y += 14;
        graphics.drawString(this.font, "Restricted after apply: " + summary.restricted, x, y, UI_TEXT, false);
        y += 14;
        graphics.drawString(this.font, "Materializable after apply: " + summary.materializable, x, y, UI_TEXT, false);
        y += 22;
        graphics.drawString(this.font, "Custom EMC changes values only.", x, y, UI_MUTED, false);
        y += 12;
        graphics.drawString(this.font, "Blacklist and item rules override vanilla profile defaults.", x, y, UI_MUTED, false);
        y += 12;
        graphics.drawString(this.font, "Open Transmutation menus will close when applied.", x, y, VanillaUiStyle.STATUS_WARN, false);
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.page == Page.OVERVIEW || this.page == Page.REVIEW) return;
        int listX = this.left + 12;
        int listY = this.top + LIST_Y;
        int rowWidth = PANEL_WIDTH - 40;
        if (mouseX < listX || mouseX >= listX + rowWidth || mouseY < listY || mouseY >= listY + LIST_ROWS * ROW_HEIGHT) return;
        int row = (mouseY - listY) / ROW_HEIGHT;
        int index = this.currentPage + row;
        if (index < 0 || index >= this.filteredItems.size()) return;

        Item item = this.filteredItems.get(index);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        EmcItemPolicy policy = previewPolicy(item);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(item.getDescription());
        if (id != null) tooltip.add(Component.literal("§7" + id));
        tooltip.add(Component.literal("§7EMC: §e" + String.format("%,d", effectiveEmc(item))));
        tooltip.add(Component.literal("§7Burnable: " + yesNo(policy.burnable())));
        tooltip.add(Component.literal("§7Learnable: " + yesNo(policy.learnable())));
        tooltip.add(Component.literal("§7Materializable: " + yesNo(policy.materializable())));
        tooltip.add(Component.literal("§7Source: §f" + policy.source()));
        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private String yesNo(boolean value) {
        return value ? "§aYes" : "§cNo";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.page != Page.OVERVIEW && this.page != Page.REVIEW) {
            if (mouseX >= this.left + SCROLLBAR_X && mouseX < this.left + SCROLLBAR_X + 8
                && mouseY >= this.top + LIST_Y && mouseY < this.top + LIST_Y + SCROLLBAR_HEIGHT) {
                this.draggingScrollbar = true;
                scrollFromMouse(mouseY);
                return true;
            }
            int listX = this.left + 12;
            int listY = this.top + LIST_Y;
            int rowWidth = PANEL_WIDTH - 40;
            if (mouseX >= listX && mouseX < listX + rowWidth
                && mouseY >= listY && mouseY < listY + LIST_ROWS * ROW_HEIGHT) {
                int row = ((int) mouseY - listY) / ROW_HEIGHT;
                int index = this.currentPage + row;
                if (index >= 0 && index < this.filteredItems.size()) {
                    this.selectedItem = this.filteredItems.get(index);
                    if (this.emcBox != null) this.emcBox.setValue(String.valueOf(effectiveEmc(this.selectedItem)));
                    updateSelectionButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            scrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Minecraft 1.20 mouse wheel signature. */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scrollList(mouseX, mouseY, delta);
    }

    /** Minecraft 1.21 mouse wheel signature. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrollList(mouseX, mouseY, scrollY);
    }

    private boolean scrollList(double mouseX, double mouseY, double delta) {
        if (this.page == Page.OVERVIEW || this.page == Page.REVIEW) return false;
        if (mouseX < this.left + 12 || mouseX >= this.left + PANEL_WIDTH - 12
            || mouseY < this.top + LIST_Y || mouseY >= this.top + LIST_Y + SCROLLBAR_HEIGHT) return false;
        this.currentPage = Math.max(0, Math.min(maxScroll(), this.currentPage - (int) Math.signum(delta)));
        return true;
    }

    private int maxScroll() {
        return Math.max(0, this.filteredItems.size() - LIST_ROWS);
    }

    private void scrollFromMouse(double mouseY) {
        int max = maxScroll();
        if (max <= 0) {
            this.currentPage = 0;
            return;
        }
        int thumbHeight = scrollbarThumbHeight();
        double local = mouseY - (this.top + LIST_Y) - thumbHeight / 2.0D;
        double ratio = Math.max(0.0D, Math.min(1.0D, local / (SCROLLBAR_HEIGHT - thumbHeight)));
        this.currentPage = (int) Math.round(ratio * max);
    }

    private int scrollbarThumbHeight() {
        if (this.filteredItems.isEmpty()) return SCROLLBAR_HEIGHT;
        return Math.max(16, SCROLLBAR_HEIGHT * Math.min(LIST_ROWS, this.filteredItems.size()) / this.filteredItems.size());
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int x = this.left + SCROLLBAR_X;
        int y = this.top + LIST_Y;
        graphics.fill(x, y, x + 8, y + SCROLLBAR_HEIGHT, 0xFF202329);
        int thumbHeight = scrollbarThumbHeight();
        int max = maxScroll();
        int travel = SCROLLBAR_HEIGHT - thumbHeight;
        int thumbY = y + (max <= 0 ? 0 : (int) Math.round((double) this.currentPage * travel / max));
        graphics.fill(x + 1, thumbY, x + 7, thumbY + thumbHeight, 0xFFE6E6E6);
    }

    private EmcSummary calculateSummary() {
        int emcItems = 0;
        int materializable = 0;
        int restricted = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null || item == Items.AIR || effectiveEmc(item) <= 0L) continue;
            emcItems++;
            if (previewPolicy(item).materializable()) materializable++;
            else restricted++;
        }
        return new EmcSummary(emcItems, materializable, restricted);
    }

    private static <K, V> int countMapChanges(Map<K, V> before, Map<K, V> after) {
        int changes = 0;
        java.util.HashSet<K> keys = new java.util.HashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (K key : keys) {
            if (!java.util.Objects.equals(before.get(key), after.get(key))) changes++;
        }
        return changes;
    }

    private void exportSettings() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("format", 1);
            root.addProperty("valueMode", this.selectedValueMode.name());
            JsonObject emc = new JsonObject();
            this.customEmc.forEach((item, value) -> {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null && value != null && value > 0L) emc.addProperty(id.toString(), value);
            });
            root.add("customEmc", emc);
            JsonObject rules = new JsonObject();
            this.policies.forEach((item, policy) -> {
                if (isVeloriseEmcItem(item) || policy == null || policy.isInherited()) return;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) return;
                JsonObject rule = new JsonObject();
                rule.addProperty("burnable", policy.burnableState());
                rule.addProperty("learnable", policy.learnableState());
                rule.addProperty("materializable", policy.materializableState());
                rules.add(id.toString(), rule);
            });
            root.add("policies", rules);
            Files.createDirectories(SETTINGS_TRANSFER_FILE.getParent());
            Files.writeString(SETTINGS_TRANSFER_FILE, SETTINGS_GSON.toJson(root), StandardCharsets.UTF_8);
            showTransferMessage("Exported settings to " + SETTINGS_TRANSFER_FILE);
        } catch (Exception exception) {
            showTransferMessage("Could not export settings: " + exception.getMessage());
        }
    }

    private void importSettings() {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(SETTINGS_TRANSFER_FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("valueMode")) {
                this.selectedValueMode = EmcValueMode.valueOf(root.get("valueMode").getAsString().toUpperCase(java.util.Locale.ROOT));
            }
            this.customEmc.clear();
            JsonObject emc = root.has("customEmc") ? root.getAsJsonObject("customEmc") : new JsonObject();
            for (Map.Entry<String, JsonElement> entry : emc.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.get(id);
                long value = entry.getValue().getAsLong();
                if (item != Items.AIR && value > 0L) this.customEmc.put(item, value);
            }
            this.policies.clear();
            JsonObject rules = root.has("policies") ? root.getAsJsonObject("policies") : new JsonObject();
            for (Map.Entry<String, JsonElement> entry : rules.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR || isVeloriseEmcItem(item)) continue;
                JsonObject rule = entry.getValue().getAsJsonObject();
                EmcPolicyOverride policy = new EmcPolicyOverride(
                    jsonInt(rule, "burnable", EmcPolicyOverride.INHERIT),
                    jsonInt(rule, "learnable", EmcPolicyOverride.INHERIT),
                    jsonInt(rule, "materializable", EmcPolicyOverride.INHERIT));
                if (!policy.isInherited()) this.policies.put(item, policy);
            }
            this.currentPage = 0;
            this.selectedItem = null;
            buildPage();
            showTransferMessage("Imported settings from " + SETTINGS_TRANSFER_FILE);
        } catch (Exception exception) {
            showTransferMessage("Could not import settings: " + exception.getMessage());
        }
    }

    private static int jsonInt(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private void showTransferMessage(String message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
    }

    private record EmcSummary(int emcItems, int materializable, int restricted) {}

    protected void saveAndApply() {
        Minecraft currentMinecraft = Minecraft.getInstance();
        if (currentMinecraft.level != null && !this.canEditServer) return;

        Map<ResourceLocation, Long> emcPayload = new HashMap<>();
        this.customEmc.forEach((item, value) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && value != null && value > 0L) emcPayload.put(id, value);
        });
        Map<ResourceLocation, EmcPolicyOverride> policyPayload = new HashMap<>();
        this.policies.forEach((item, policy) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && policy != null && !policy.isInherited()
                && !EMCRegistry.isHardBlocked(item) && !isVeloriseEmcItem(item)) {
                policyPayload.put(id, policy);
            }
        });

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.getConnection() != null) {
            sendServerUpdate(emcPayload, this.selectedValueMode, policyPayload);
        } else {
            Map<Item, Long> localEmc = new HashMap<>(this.customEmc);
            Map<Item, EmcPolicyOverride> localPolicies = new HashMap<>(this.policies);
            EMCRegistry.setConfiguredValueMode(this.selectedValueMode);
            EMCRegistry.replaceCustomEMC(localEmc);
            EMCRegistry.replaceItemPolicies(localPolicies);
            EMCRegistry.loadCustomEMC();
            EMCRegistry.loadItemPolicies();
        }
        this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    private static boolean isVeloriseEmcItem(Item item) {
        if (item == null || item == Items.AIR) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && "velorise_emc".equals(id.getNamespace());
    }
}
