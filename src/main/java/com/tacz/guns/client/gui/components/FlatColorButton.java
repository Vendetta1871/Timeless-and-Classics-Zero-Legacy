package com.tacz.guns.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.gui.Button;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.Collections;
import java.util.List;
import com.tacz.guns.legacy.util.Components;
import com.google.common.collect.ImmutableList;

public class FlatColorButton extends Button {
    private boolean isSelect = false;
    private List<ITextComponent> tooltips;

    public FlatColorButton(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
    }

    public FlatColorButton setTooltips(String key) {
        tooltips = Collections.singletonList(Components.translatable(key));
        return this;
    }

    public FlatColorButton setTooltips(List<ITextComponent> tooltips) {
        this.tooltips = tooltips;
        return this;
    }

    public FlatColorButton setTooltips(ITextComponent... tooltips) {
        this.tooltips = ImmutableList.copyOf(tooltips);
        return this;
    }

    public void renderToolTip(GuiGraphics graphics, GuiScreen screen, int pMouseX, int pMouseY) {
        if (this.isHovered() && tooltips != null) {
            graphics.renderComponentTooltip(Minecraft.getMinecraft().fontRenderer, tooltips, pMouseX, pMouseY);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        if (isSelect) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xAF222222, 0xAF222222);
        } else {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xAF222222, 0xAF222222);
        }
        if (this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }
        this.renderScrollingString(graphics, font, 2, 0xF3EFE0);
        this.renderToolTip(graphics, minecraft.currentScreen, mouseX, mouseY);
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }
}
