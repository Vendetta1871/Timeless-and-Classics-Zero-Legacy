package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code ImageButton} with the legacy constructor (a 256x256 texture, the hovered image is {@code yDiffTex}
 * below the normal one).
 */
@SideOnly(Side.CLIENT)
public class ImageButton extends Button {
    private final ResourceLocation resourceLocation;
    private final int xTexStart;
    private final int yTexStart;
    private final int yDiffTex;
    private final int textureWidth;
    private final int textureHeight;

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation texture, OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, texture, 256, 256, onPress);
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation texture,
                       int textureWidth, int textureHeight, OnPress onPress) {
        super(x, y, width, height, empty(), onPress);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.resourceLocation = texture;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int vOffset = this.yTexStart;
        if (this.isHoveredOrFocused()) {
            vOffset += this.yDiffTex;
        }
        graphics.blit(this.resourceLocation, this.x, this.y, (float) this.xTexStart, (float) vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
    }
}
