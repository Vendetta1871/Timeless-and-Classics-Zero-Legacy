package com.tacz.guns.client.gui;

import com.tacz.guns.inventory.AmmoStorageEntry;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.FontUtil;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageAmmoStorageClick;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2: extends {@link GuiContainer} directly instead of the {@code legacy.util.gui.AbstractContainerScreen} shim,
 * because this menu has real player inventory slots (drawing, clicking, dragging and the carried stack are vanilla
 * {@link GuiContainer} behaviour that the shim leaves out). The upstream methods keep their names and are called from
 * the {@link GuiContainer} hooks: {@code renderBg} from {@link #drawGuiContainerBackgroundLayer}, {@code renderLabels}
 * from {@link #drawGuiContainerForegroundLayer}, {@code render} is {@link #drawScreen}.
 */
@SideOnly(Side.CLIENT)
public class AmmoStorageScreen extends GuiContainer {
    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int CONTAINER_ROWS = AmmoStorageMenu.STORAGE_ROWS;
    private final AmmoStorageMenu menu;
    private final InventoryPlayer playerInventory;
    private final ITextComponent title;
    private final int imageWidth;
    private final int imageHeight;
    private final int inventoryLabelY;
    private int virtualClickButton = -1;

    public AmmoStorageScreen(AmmoStorageMenu menu, InventoryPlayer inventory, ITextComponent title) {
        super(menu);
        this.menu = menu;
        this.playerInventory = inventory;
        this.title = title;
        imageWidth = 176;
        imageHeight = 114 + CONTAINER_ROWS * 18 + AmmoStorageMenu.STATUS_HEIGHT;
        inventoryLabelY = imageHeight - 94;
        this.xSize = imageWidth;
        this.ySize = imageHeight;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        renderBg(new GuiGraphics(), partialTicks, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        int leftPos = guiLeft;
        int topPos = guiTop;
        int storageHeight = CONTAINER_ROWS * 18 + 17 + AmmoStorageMenu.STATUS_HEIGHT;
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos, 0, 0, imageWidth, 17);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + 17, imageWidth, AmmoStorageMenu.STATUS_HEIGHT,
                0.0F, 7.0F, imageWidth, 1, 256, 256);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + 17 + AmmoStorageMenu.STATUS_HEIGHT,
                0, 17, imageWidth, CONTAINER_ROWS * 18);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + storageHeight, 0, 126, imageWidth, 96);
        renderCapacity(graphics);
        List<AmmoStorageEntry> entries = menu.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            AmmoStorageEntry entry = entries.get(i);
            int x = leftPos + AmmoStorageMenu.INVENTORY_X + i % AmmoStorageMenu.STORAGE_COLUMNS * 18;
            int y = topPos + AmmoStorageMenu.STORAGE_Y + i / AmmoStorageMenu.STORAGE_COLUMNS * 18;
            graphics.renderItem(entry.sample(), x, y);
            renderSizeLabel(graphics, x, y, formatCount(entry.count()));
        }
        int hovered = getVirtualCell(mouseX, mouseY);
        if (hovered >= 0) {
            int x = leftPos + AmmoStorageMenu.INVENTORY_X + hovered % AmmoStorageMenu.STORAGE_COLUMNS * 18;
            int y = topPos + AmmoStorageMenu.STORAGE_Y + hovered / AmmoStorageMenu.STORAGE_COLUMNS * 18;
            renderSlotHighlight(x, y);
        }
    }

    /**
     * {@code AbstractContainerScreen#renderLabels}: the title and the player inventory label.
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(title.getFormattedText(), 8, 6, 0x404040);
        fontRenderer.drawString(playerInventory.getDisplayName().getFormattedText(), 8, inventoryLabelY, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderTooltip(new GuiGraphics(), mouseX, mouseY);
    }

    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isPointInRegion(8, 17, 160, 21, mouseX, mouseY)) {
            List<ITextComponent> lines = new ArrayList<>();
            lines.add(Components.translatable("gui.tacz.ammo_storage.capacity_detail", menu.getUsedStacks(), menu.getCapacityStacks()));
            lines.add(Components.translatable("gui.tacz.ammo_storage.free_capacity", Math.max(0, menu.getCapacityStacks() - menu.getUsedStacks())));
            graphics.renderComponentTooltip(fontRenderer, lines, mouseX, mouseY);
            return;
        }
        int cell = getVirtualCell(mouseX, mouseY);
        if (cell >= 0 && cell < menu.getEntries().size() && playerInventory.getItemStack().isEmpty()) {
            AmmoStorageEntry entry = menu.getEntries().get(cell);
            ItemStack sample = entry.sample();
            List<String> tooltip = new ArrayList<>(getItemToolTip(sample));
            tooltip.add(Components.translatable("gui.tacz.ammo_storage.count", entry.count()).withStyle(TextFormatting.GRAY).getFormattedText());
            tooltip.add(Components.translatable("gui.tacz.ammo_storage.remaining", entry.remainingCapacity())
                    .withStyle(entry.remainingCapacity() == 0 ? TextFormatting.RED : TextFormatting.GRAY).getFormattedText());
            // GuiScreen#renderToolTip with extra lines: announce the stack to the tooltip events (tooltip images)
            FontRenderer font = sample.getItem().getFontRenderer(sample);
            GuiUtils.preItemToolTip(sample);
            drawHoveringText(tooltip, mouseX, mouseY, font == null ? fontRenderer : font);
            GuiUtils.postItemToolTip();
            return;
        }
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        int cell = getVirtualCell(mouseX, mouseY);
        if (cell >= 0 && (button == 0 || button == 1)) {
            int entryId = cell < menu.getEntries().size() ? menu.getEntries().get(cell).id() : -1;
            AmmoStorageMenu.Action action = isShiftKeyDown() ? AmmoStorageMenu.Action.QUICK_MOVE
                    : button == 1 ? AmmoStorageMenu.Action.PICKUP_HALF : AmmoStorageMenu.Action.PICKUP;
            NetworkHandler.CHANNEL.sendToServer(new ClientMessageAmmoStorageClick(menu.windowId, entryId, action));
            virtualClickButton = button;
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (virtualClickButton >= 0) {
            return;
        }
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (button >= 0 && button == virtualClickButton) {
            virtualClickButton = -1;
            return;
        }
        super.mouseReleased(mouseX, mouseY, button);
    }

    private int getVirtualCell(double mouseX, double mouseY) {
        for (int cell = 0; cell < AmmoStorageMenu.STORAGE_SLOT_COUNT; cell++) {
            int x = AmmoStorageMenu.INVENTORY_X + cell % AmmoStorageMenu.STORAGE_COLUMNS * 18;
            int y = AmmoStorageMenu.STORAGE_Y + cell / AmmoStorageMenu.STORAGE_COLUMNS * 18;
            if (isPointInRegion(x, y, 16, 16, (int) mouseX, (int) mouseY)) {
                return cell;
            }
        }
        return -1;
    }

    /**
     * {@code AbstractContainerScreen#renderSlotHighlight}, as {@link GuiContainer#drawScreen} draws it.
     */
    private void renderSlotHighlight(int x, int y) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.colorMask(true, true, true, false);
        drawGradientRect(x, y, x + 16, y + 16, 0x80FFFFFF, 0x80FFFFFF);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableDepth();
    }

    private void renderCapacity(GuiGraphics graphics) {
        int leftPos = guiLeft;
        int topPos = guiTop;
        ITextComponent capacity = Components.translatable("gui.tacz.ammo_storage.capacity", menu.getUsedStacks(), menu.getCapacityStacks());
        ITextComponent types = Components.translatable("gui.tacz.ammo_storage.types", menu.getEntries().size(), AmmoStorageMenu.STORAGE_SLOT_COUNT);
        graphics.drawString(fontRenderer, capacity, leftPos + 8, topPos + 18, 0x404040, false);
        graphics.drawString(fontRenderer, types, leftPos + imageWidth - 8 - FontUtil.width(fontRenderer, types), topPos + 18,
                menu.getEntries().size() == AmmoStorageMenu.STORAGE_SLOT_COUNT ? 0xAA3030 : 0x404040, false);
        int x = leftPos + 8;
        int y = topPos + 31;
        double fullness = menu.getCapacityStacks() <= 0 ? 0
                : Math.min(1, (double) menu.getUsedStacks() / menu.getCapacityStacks());
        int color = fullness >= 1 ? 0xFFC45A48 : fullness >= 0.75 ? 0xFFD7A744 : 0xFF65A85C;
        graphics.fill(x, y, x + 160, y + 6, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 159, y + 5, 0xFF777777);
        graphics.fill(x + 1, y + 1, x + 1 + (int) Math.round(158 * fullness), y + 5, color);
        GlStateManager.color(1, 1, 1, 1);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000_000) {
            return formatCompact(count, 1_000_000_000, "b");
        }
        if (count >= 1_000_000) {
            return formatCompact(count, 1_000_000, "m");
        }
        if (count >= 1_000) {
            return formatCompact(count, 1_000, "k");
        }
        return Long.toString(count);
    }

    private String formatCompact(long count, int unit, String suffix) {
        long whole = count / unit;
        long decimal = count % unit / (unit / 10);
        return decimal == 0 || whole >= 10 ? whole + suffix : whole + "." + decimal + suffix;
    }

    private void renderSizeLabel(GuiGraphics graphics, int x, int y, String text) {
        float scale = 0.666F;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.pose().scale(scale, scale, 1);
        // 1.12.2: the item renderer leaves lighting and depth in a state that hides plain text
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        graphics.drawString(fontRenderer, text, (int) ((x + 17 - fontRenderer.getStringWidth(text) * scale) / scale),
                (int) ((y + 12) / scale), 0xFFFFFFFF, true);
        GlStateManager.enableDepth();
        graphics.pose().popPose();
    }
}
