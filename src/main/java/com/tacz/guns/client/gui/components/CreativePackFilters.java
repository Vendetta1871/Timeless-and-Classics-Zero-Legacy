package com.tacz.guns.client.gui.components;

import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.*;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.legacy.util.gui.Button;
import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import com.tacz.guns.legacy.util.Components;
import java.util.stream.Collectors;

/** A screen-local content pack filter; the registered creative tabs keep their full contents. */
public class CreativePackFilters {
    private static final ResourceLocation TAB_TEXTURE = new ResourceLocation("textures/gui/recipe_book.png");
    private static final ResourceLocation ALL_PACKS_ICON = new ResourceLocation(GunMod.MOD_ID, "textures/gui/creative_pack_all.png");
    private static final int VISIBLE_PACKS = 3;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_SPACING = 29;
    // 1.12.2: the legacy Button shim draws its own tooltips only inside legacy screens, so the creative screen
    // draws these through renderTooltips
    private static final ITextComponent PREVIOUS_TOOLTIP = Components.translatable("gui.tacz.creative_pack_filter.previous");
    private static final ITextComponent NEXT_TOOLTIP = Components.translatable("gui.tacz.creative_pack_filter.next");

    private final GuiContainerCreative screen;
    private final Runnable resetScroll;
    private final Map<CreativeTabs, String> selections = new IdentityHashMap<>();
    private final List<PackFilterButton> buttons = new ArrayList<>();
    private final List<FilterOption> options = new ArrayList<>();
    private Button previousButton;
    private Button nextButton;
    private CreativeTabs currentTab;
    private long packInfoVersion = -1;
    private int packScroll;

    public CreativePackFilters(GuiContainerCreative screen, Runnable resetScroll) {
        this.screen = screen;
        this.resetScroll = resetScroll;
    }

    public void init(Consumer<Button> add, CreativeTabs tab) {
        buttons.clear();
        for (int i = 0; i <= VISIBLE_PACKS; i++) {
            PackFilterButton button = new PackFilterButton(screen.getGuiLeft() - 28,
                    screen.getGuiTop() + 11 + TAB_SPACING * i);
            buttons.add(button);
            add.accept(button);
        }
        previousButton = new Button(screen.getGuiLeft() - 24, screen.getGuiTop() - 3, 20, 12,
                Components.literal("▲"), button -> scrollPacks(-1));
        nextButton = new Button(screen.getGuiLeft() - 24, screen.getGuiTop() + 127, 20, 12,
                Components.literal("▼"), button -> scrollPacks(1));
        add.accept(previousButton);
        add.accept(nextButton);
        updateTab(tab, true);
    }

    public void updateTab(CreativeTabs tab, boolean force) {
        if (!RenderConfig.RENDER_CREATIVE_PACK_SIDEBAR.get()) {
            if (currentTab == tab) {
                showItems(getDisplayItems(tab));
            }
            selections.clear();
            hideWidgets();
            return;
        }
        if (!isTaczCategoryTab(tab)) {
            hideWidgets();
            return;
        }

        long reloadVersion = ClientAssetsManager.INSTANCE.getPackInfoReloadVersion();
        // 1.12.2: tabs have no cached display item collection to compare (CreativeTabs#displayAllRelevantItems
        // builds a new list on every call), so an unforced update only checks the tab and the pack info version
        if (!force && currentTab == tab && packInfoVersion == reloadVersion) {
            return;
        }
        Collection<ItemStack> displayItems = getDisplayItems(tab);
        if (currentTab != tab) {
            packScroll = 0;
        }
        currentTab = tab;
        packInfoVersion = reloadVersion;
        options.clear();

        List<ItemStack> allItems = displayItems.stream().map(ItemStack::copy).collect(Collectors.toList());
        options.add(new FilterOption(null, Components.translatable("gui.tacz.creative_pack_filter.all"),
                ItemStack.EMPTY, ALL_PACKS_ICON, allItems));
        Map<String, List<ItemStack>> groupedItems = groupItems(displayItems);
        groupedItems.forEach((namespace, items) -> {
            PackInfo info = ClientAssetsManager.INSTANCE.getPackInfo(namespace);
            ITextComponent name = info == null || info.getName() == null || info.getName().trim().isEmpty()
                    || "custom.tacz.error.no_name".equals(info.getName())
                    ? Components.literal(namespace) : Components.translatable(info.getName());
            ResourceLocation logo = info == null ? null : info.getLogo(namespace);
            if (logo != null && !resourceExists(logo)) {
                logo = null;
            }
            FilterOption option = new FilterOption(namespace, name, items.get(0).copy(), logo, items);
            if (GunMod.MOD_ID.equals(namespace)) {
                options.add(1, option);
            } else {
                options.add(option);
            }
        });
        String selected = selections.get(tab);
        if (selected != null && !groupedItems.containsKey(selected)) {
            selections.remove(tab);
        }
        packScroll = MathHelper.clamp(packScroll, 0, maxScroll());
        updateButtons();
        applyFilter();
    }

    /**
     * 1.12.2: {@code BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)} has the tacz namespace and the tab type is
     * {@code CATEGORY}. 1.12.2 has no creative tab registry; TACZ's tabs ({@code ModCreativeTabs}) are labelled
     * {@code tacz_<name>}, and the vanilla search / hotbar / inventory tabs never are.
     */
    private static boolean isTaczCategoryTab(@Nullable CreativeTabs tab) {
        return tab != null && tab.getTabLabel().startsWith(GunMod.MOD_ID + "_");
    }

    /**
     * 1.12.2: {@code CreativeModeTab#getDisplayItems()}.
     */
    private static Collection<ItemStack> getDisplayItems(CreativeTabs tab) {
        NonNullList<ItemStack> items = NonNullList.create();
        tab.displayAllRelevantItems(items);
        return items;
    }

    /**
     * 1.12.2: {@code ResourceManager#getResource(location).isPresent()}; the 1.12.2 resource manager throws when the
     * resource is missing.
     */
    private static boolean resourceExists(ResourceLocation location) {
        try (IResource ignored = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void hideWidgets() {
        currentTab = null;
        buttons.forEach(button -> button.setOption(null));
        previousButton.visible = nextButton.visible = false;
        previousButton.enabled = nextButton.enabled = false;
    }

    /** Keeps every stack, including multiple NBT variants of the same registered item. */
    static Map<String, List<ItemStack>> groupItems(Collection<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                groups.computeIfAbsent(getContentId(copy).getNamespace(), key -> new ArrayList<>()).add(copy);
            }
        }
        return groups;
    }

    static ResourceLocation getContentId(ItemStack stack) {
        if (stack.getItem() instanceof IGun gun) {
            return gun.getGunId(stack);
        }
        if (stack.getItem() instanceof IAmmo ammo) {
            return ammo.getAmmoId(stack);
        }
        if (stack.getItem() instanceof IAttachment attachment) {
            return attachment.getAttachmentId(stack);
        }
        if (stack.getItem() instanceof IBlock block) {
            return block.getBlockId(stack);
        }
        if (stack.getItem() instanceof IAmmoBox box) {
            ResourceLocation ammoId = box.getAmmoId(stack);
            if (!DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
                return ammoId;
            }
        }
        return stack.getItem().getRegistryName();
    }

    private void select(FilterOption option) {
        if (option.namespace() == null) {
            selections.remove(currentTab);
        } else {
            selections.put(currentTab, option.namespace());
        }
        applyFilter();
    }

    private void applyFilter() {
        String selected = selections.get(currentTab);
        FilterOption option = options.stream().filter(entry -> Objects.equals(entry.namespace(), selected))
                .findFirst().orElse(options.get(0));
        showItems(option.items());
    }

    private void showItems(Collection<ItemStack> items) {
        GuiContainerCreative.ContainerCreative menu = (GuiContainerCreative.ContainerCreative) screen.inventorySlots;
        menu.itemList.clear();
        items.forEach(stack -> menu.itemList.add(stack.copy()));
        resetScroll.run();
        menu.scrollTo(0);
    }

    private int maxScroll() {
        return Math.max(0, options.size() - 1 - VISIBLE_PACKS);
    }

    private void scrollPacks(int delta) {
        packScroll = MathHelper.clamp(packScroll + delta, 0, maxScroll());
        updateButtons();
    }

    private void updateButtons() {
        buttons.get(0).setOption(options.get(0));
        for (int i = 1; i < buttons.size(); i++) {
            int optionIndex = packScroll + i;
            buttons.get(i).setOption(optionIndex < options.size() ? options.get(optionIndex) : null);
        }
        previousButton.visible = nextButton.visible = maxScroll() > 0;
        previousButton.enabled = packScroll > 0;
        nextButton.enabled = packScroll < maxScroll();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!RenderConfig.RENDER_CREATIVE_PACK_SIDEBAR.get() || currentTab == null || delta == 0) {
            return false;
        }
        int x = screen.getGuiLeft() - 28;
        int y = screen.getGuiTop() + 11;
        if (mouseX >= x && mouseX < x + 32 && mouseY >= y && mouseY < y + TAB_SPACING * VISIBLE_PACKS + TAB_HEIGHT) {
            scrollPacks(delta > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    /**
     * 1.12.2: the hover tooltips that upstream sets with {@code setTooltip(Tooltip.create(..))}. The creative screen
     * is not a legacy widget screen, so it calls this after drawing everything else.
     */
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        ITextComponent tooltip = null;
        for (PackFilterButton button : buttons) {
            if (button.visible && button.option != null && button.isMouseOver()) {
                tooltip = button.option.name();
            }
        }
        if (previousButton.visible && previousButton.isMouseOver()) {
            tooltip = PREVIOUS_TOOLTIP;
        }
        if (nextButton.visible && nextButton.isMouseOver()) {
            tooltip = NEXT_TOOLTIP;
        }
        if (tooltip != null) {
            graphics.renderTooltip(Minecraft.getMinecraft().fontRenderer, tooltip, mouseX, mouseY);
        }
    }

    /**
     * 1.20.1 {@code GuiGraphics#blitNineSliced} (256x256 texture): the corners and edges keep their size, the center
     * is tiled.
     */
    private static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                       int sliceSize, int uWidth, int vHeight, int uOffset, int vOffset) {
        int left = Math.min(sliceSize, width / 2);
        int right = Math.min(sliceSize, width - left);
        int top = Math.min(sliceSize, height / 2);
        int bottom = Math.min(sliceSize, height - top);
        int centerWidth = width - left - right;
        int centerHeight = height - top - bottom;
        int uCenterWidth = uWidth - 2 * sliceSize;
        int vCenterHeight = vHeight - 2 * sliceSize;
        int[] xs = {x, x + left, x + width - right};
        int[] widths = {left, centerWidth, right};
        int[] us = {uOffset, uOffset + sliceSize, uOffset + uWidth - right};
        int[] uSizes = {left, uCenterWidth, right};
        int[] ys = {y, y + top, y + height - bottom};
        int[] heights = {top, centerHeight, bottom};
        int[] vs = {vOffset, vOffset + sliceSize, vOffset + vHeight - bottom};
        int[] vSizes = {top, vCenterHeight, bottom};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (widths[column] <= 0 || heights[row] <= 0 || uSizes[column] <= 0 || vSizes[row] <= 0) {
                    continue;
                }
                // tile the region with the source size, clipping the last tile
                for (int dy = 0; dy < heights[row]; dy += vSizes[row]) {
                    int h = Math.min(vSizes[row], heights[row] - dy);
                    for (int dx = 0; dx < widths[column]; dx += uSizes[column]) {
                        int w = Math.min(uSizes[column], widths[column] - dx);
                        graphics.blit(texture, xs[column] + dx, ys[row] + dy, us[column], vs[row], w, h);
                    }
                }
            }
        }
    }

    private static final class FilterOption {
        @Nullable
        private final String namespace;
        private final ITextComponent name;
        private final ItemStack icon;
        @Nullable
        private final ResourceLocation logo;
        private final List<ItemStack> items;

        private FilterOption(@Nullable String namespace, ITextComponent name, ItemStack icon,
                             @Nullable ResourceLocation logo, List<ItemStack> items) {
            this.namespace = namespace;
            this.name = name;
            this.icon = icon;
            this.logo = logo;
            this.items = items;
        }

        @Nullable
        public String namespace() {
            return namespace;
        }

        public ITextComponent name() {
            return name;
        }

        public ItemStack icon() {
            return icon;
        }

        @Nullable
        public ResourceLocation logo() {
            return logo;
        }

        public List<ItemStack> items() {
            return items;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FilterOption)) return false;
            FilterOption that = (FilterOption) o;
            return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name)
                    && Objects.equals(icon, that.icon) && Objects.equals(logo, that.logo) && Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, name, icon, logo, items);
        }

        @Override
        public String toString() {
            return "FilterOption[" + "namespace=" + namespace + ", " + "name=" + name + ", " + "icon=" + icon + ", "
                    + "logo=" + logo + ", " + "items=" + items + "]";
        }
    }

    private class PackFilterButton extends Button {
        @Nullable
        private FilterOption option;

        private PackFilterButton(int x, int y) {
            super(x, y, 32, TAB_HEIGHT, Button.empty(), button -> {
                FilterOption selected = ((PackFilterButton) button).option;
                if (selected != null) {
                    select(selected);
                }
            });
        }

        private void setOption(@Nullable FilterOption option) {
            this.option = option;
            visible = enabled = option != null;
            setMessage(option == null ? Button.empty() : option.name());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (option == null) {
                return;
            }
            boolean selected = Objects.equals(option.namespace(), selections.get(currentTab));
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            // Native left-side tabs keep highlights above/left and shadows below.
            // Preserve the corners and the selected tab's join to the panel; only tile the center.
            blitNineSliced(graphics, TAB_TEXTURE, getX(), getY(), selected ? 31 : 28, TAB_HEIGHT,
                    3, selected ? 35 : 30, TAB_HEIGHT, selected ? 188 : 153, 2);
            if (option.logo() != null) {
                // UVs cover the full image, independently of its pixel dimensions.
                graphics.blit(option.logo(), getX() + 8, getY() + 5, 0, 0, 16, 16, 16, 16);
            } else {
                graphics.renderItem(option.icon(), getX() + 8, getY() + 5);
            }
            // 1.12.2: no keyboard focus navigation between widgets, so upstream's focus outline
            // (isFocused() && last input was the keyboard) is never drawn
        }
    }
}
