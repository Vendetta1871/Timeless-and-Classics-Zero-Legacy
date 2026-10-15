package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.gui.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import com.tacz.guns.legacy.util.Components;

public class InventoryAttachmentSlot extends Button implements IStackTooltip {
    private final int slotIndex;
    private final InventoryPlayer inventory;

    public InventoryAttachmentSlot(int pX, int pY, int slotIndex, InventoryPlayer inventory, Button.OnPress onPress) {
        super(pX, pY, 18, 18, Button.empty(), onPress);
        this.slotIndex = slotIndex;
        this.inventory = inventory;
    }

    @Override
    public void renderTooltip(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused() && 0 <= this.slotIndex && this.slotIndex < this.inventory.getSizeInventory()) {
            ItemStack item = this.inventory.getStackInSlot(slotIndex);
            consumer.accept(item);
        }
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        int x = getX(), y = getY();
        if (isHoveredOrFocused()) {
            graphics.blit(GunRefitScreen.SLOT_TEXTURE, x, y, 0, 0, width, height, 18, 18);
        } else {
            graphics.blit(GunRefitScreen.SLOT_TEXTURE, x + 1, y + 1, 1, 1, width - 2, height - 2, 18, 18);
        }
        graphics.renderItem(inventory.getStackInSlot(slotIndex), x + 1, y + 1);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
