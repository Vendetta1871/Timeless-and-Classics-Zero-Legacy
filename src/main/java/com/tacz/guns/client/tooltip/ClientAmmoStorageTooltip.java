package com.tacz.guns.client.tooltip;

import com.tacz.guns.inventory.tooltip.AmmoStorageTooltip;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.ClientTooltipComponent;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.FontUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

public class ClientAmmoStorageTooltip implements ClientTooltipComponent {
    private static final int ENTRY_HEIGHT = 28;
    private final AmmoStorageTooltip tooltip;

    public ClientAmmoStorageTooltip(AmmoStorageTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int getHeight() {
        int height = tooltip.getEntries().size() * ENTRY_HEIGHT;
        return tooltip.getHidden() > 0 ? height + 10 : height;
    }

    @Override
    public int getWidth(FontRenderer font) {
        int width = 0;
        for (AmmoStorageTooltip.Entry entry : tooltip.getEntries()) {
            width = Math.max(width, FontUtil.width(font, entry.getAmmoName()));
            width = Math.max(width, FontUtil.width(font, getCount(entry)));
        }
        if (tooltip.getHidden() > 0) {
            width = Math.max(width, FontUtil.width(font, getHidden()));
        }
        return width + 22;
    }

    @Override
    public void renderText(FontRenderer font, int x, int y, GuiGraphics graphics) {
        int yOffset = y;
        for (AmmoStorageTooltip.Entry entry : tooltip.getEntries()) {
            graphics.drawString(font, entry.getAmmoName(), x + 20, yOffset + 4, 0xffaa00, false);
            graphics.drawString(font, getCount(entry), x + 20, yOffset + 15, 0x666666, false);
            yOffset += ENTRY_HEIGHT;
        }
        if (tooltip.getHidden() > 0) {
            graphics.drawString(font, getHidden(), x, yOffset, 0x9e9e9e, false);
        }
    }

    @Override
    public void renderImage(FontRenderer font, int x, int y, GuiGraphics guiGraphics) {
        int yOffset = y;
        for (AmmoStorageTooltip.Entry entry : tooltip.getEntries()) {
            guiGraphics.renderItem(entry.ammo(), x, yOffset + 5);
            yOffset += ENTRY_HEIGHT;
        }
    }

    private static ITextComponent getCount(AmmoStorageTooltip.Entry entry) {
        // 1.12.2: TextComponentTranslation only knows %s, the string uses %d
        return Components.literal(I18n.format("tooltip.tacz.ammo_box.count", entry.count()));
    }

    private ITextComponent getHidden() {
        return Components.translatable("container.shulkerBox.more", tooltip.getHidden()).withStyle(TextFormatting.ITALIC);
    }
}
