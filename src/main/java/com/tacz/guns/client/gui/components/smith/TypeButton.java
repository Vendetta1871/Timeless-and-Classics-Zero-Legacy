package com.tacz.guns.client.gui.components.smith;

import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.gui.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.tacz.guns.legacy.util.Components;

public class TypeButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public TypeButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 24, 25, Button.empty(), onPress);
        this.stack = stack;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.enableDepthTest();

        int vOffset = isHoveredOrFocused() ? 204 + this.height : 204;
        if (isSelected) {
            gui.blit(TEXTURE, this.getX(), this.getY(), 0, vOffset, this.width, this.height, 256, 256);
        } else {
            gui.blit(TEXTURE, this.getX(), this.getY(), 26, vOffset, this.width, this.height, 256, 256);
        }

        gui.renderItem(this.stack, this.getX() + 4, this.getY() + 5);
    }

    @Override
    public void onPress() {
        this.isSelected = true;
        this.onPress.onPress(this);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
