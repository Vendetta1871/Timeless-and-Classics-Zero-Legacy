package com.tacz.guns.client.tooltip;

import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.ClientTooltipComponent;
import com.tacz.guns.legacy.util.FontUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.util.Components;

public class ClientAmmoBoxTooltip implements ClientTooltipComponent {
    private final ItemStack ammo;
    private final ITextComponent count;
    private final ITextComponent ammoName;

    public ClientAmmoBoxTooltip(AmmoBoxTooltip tooltip) {
        this.ammo = tooltip.getAmmo();
        ItemStack ammoBox = tooltip.getAmmoBox();
        if (ammoBox.getItem() instanceof IAmmoBox box && box.isCreative(ammoBox)) {
            this.count = Components.literal("∞");
        } else {
            // 1.12.2: TextComponentTranslation only knows %s, the string uses %d
            this.count = Components.literal(I18n.format("tooltip.tacz.ammo_box.count", tooltip.getCount()));
        }
        this.ammoName = Components.literal(this.ammo.getDisplayName());
    }

    @Override
    public int getHeight() {
        return 28;
    }

    @Override
    public int getWidth(FontRenderer font) {
        return Math.max(FontUtil.width(font, ammoName), FontUtil.width(font, count)) + 22;
    }

    @Override
    public void renderText(FontRenderer font, int pX, int pY, GuiGraphics graphics) {
        graphics.drawString(font, ammoName, pX + 20, pY + 4, 0xffaa00, false);
        graphics.drawString(font, count, pX + 20, pY + 15, 0x666666, false);
    }

    @Override
    public void renderImage(FontRenderer pFont, int pX, int pY, GuiGraphics pGuiGraphics) {
        pGuiGraphics.renderItem(ammo, pX, pY + 5);
    }
}
