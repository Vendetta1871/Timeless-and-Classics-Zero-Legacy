package com.tacz.guns.legacy.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.20.1 {@code GuiGraphics} calls TACZ uses, drawn with 1.12.2 {@link Gui} helpers, {@link FontRenderer} and
 * {@code RenderItem}. Every call applies {@link #pose()} to the GL matrix for its duration, so code that translates or
 * scales the pose works unchanged. Create one per frame (or per screen render) with {@code new GuiGraphics()}.
 */
@SideOnly(Side.CLIENT)
public class GuiGraphics {
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final PoseStack pose = new PoseStack();

    public PoseStack pose() {
        return pose;
    }

    public int guiWidth() {
        return Window.get().getGuiScaledWidth();
    }

    public int guiHeight() {
        return Window.get().getGuiScaledHeight();
    }

    private void begin() {
        GlStateManager.pushMatrix();
        RenderSystem.multMatrix(pose.last().pose());
    }

    private void end() {
        GlStateManager.popMatrix();
    }

    public void setColor(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }

    public void flush() {
    }

    /**
     * {@code blit(texture, x, y, u, v, width, height, textureWidth, textureHeight)}.
     */
    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        minecraft.getTextureManager().bindTexture(texture);
        begin();
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
        end();
    }

    /**
     * {@code blit(texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight)}: a scaled region.
     */
    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        minecraft.getTextureManager().bindTexture(texture);
        begin();
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, textureWidth, textureHeight);
        end();
    }

    /**
     * {@code blit(texture, x, y, u, v, width, height)} on a 256x256 texture.
     */
    public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        blit(texture, x, y, (float) u, (float) v, width, height, 256, 256);
    }

    /**
     * 1.20's {@code GuiGraphics#blitNineSliced} with the same slice size on every side (256x256 texture).
     */
    public void blitNineSliced(ResourceLocation texture, int x, int y, int width, int height, int sliceSize, int uWidth, int vHeight, int uOffset, int vOffset) {
        blitNineSliced(texture, x, y, width, height, sliceSize, sliceSize, sliceSize, sliceSize, uWidth, vHeight, uOffset, vOffset);
    }

    public void blitNineSliced(ResourceLocation texture, int x, int y, int width, int height, int left, int top, int right, int bottom,
                               int uWidth, int vHeight, int uOffset, int vOffset) {
        left = Math.min(left, width / 2);
        right = Math.min(right, width / 2);
        top = Math.min(top, height / 2);
        bottom = Math.min(bottom, height / 2);
        if (width == uWidth && height == vHeight) {
            blit(texture, x, y, uOffset, vOffset, width, height);
        } else if (height == vHeight) {
            blit(texture, x, y, uOffset, vOffset, left, height);
            blitRepeating(texture, x + left, y, width - right - left, height, uOffset + left, vOffset, uWidth - right - left, vHeight);
            blit(texture, x + width - right, y, uOffset + uWidth - right, vOffset, right, height);
        } else if (width == uWidth) {
            blit(texture, x, y, uOffset, vOffset, width, top);
            blitRepeating(texture, x, y + top, width, height - bottom - top, uOffset, vOffset + top, uWidth, vHeight - bottom - top);
            blit(texture, x, y + height - bottom, uOffset, vOffset + vHeight - bottom, width, bottom);
        } else {
            blit(texture, x, y, uOffset, vOffset, left, top);
            blitRepeating(texture, x + left, y, width - right - left, top, uOffset + left, vOffset, uWidth - right - left, top);
            blit(texture, x + width - right, y, uOffset + uWidth - right, vOffset, right, top);
            blit(texture, x, y + height - bottom, uOffset, vOffset + vHeight - bottom, left, bottom);
            blitRepeating(texture, x + left, y + height - bottom, width - right - left, bottom, uOffset + left, vOffset + vHeight - bottom, uWidth - right - left, bottom);
            blit(texture, x + width - right, y + height - bottom, uOffset + uWidth - right, vOffset + vHeight - bottom, right, bottom);
            blitRepeating(texture, x, y + top, left, height - bottom - top, uOffset, vOffset + top, left, vHeight - bottom - top);
            blitRepeating(texture, x + left, y + top, width - right - left, height - bottom - top, uOffset + left, vOffset + top, uWidth - right - left, vHeight - bottom - top);
            blitRepeating(texture, x + width - right, y + top, left, height - bottom - top, uOffset + uWidth - right, vOffset + top, right, vHeight - bottom - top);
        }
    }

    private void blitRepeating(ResourceLocation texture, int x, int y, int width, int height, int uOffset, int vOffset, int sourceWidth, int sourceHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        for (int dx = 0; dx < width; dx += sourceWidth) {
            int w = Math.min(sourceWidth, width - dx);
            for (int dy = 0; dy < height; dy += sourceHeight) {
                int h = Math.min(sourceHeight, height - dy);
                blit(texture, x + dx, y + dy, uOffset, vOffset, w, h);
            }
        }
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        begin();
        Gui.drawRect(minX, minY, maxX, maxY, color);
        end();
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        begin();
        drawGradient(minX, minY, maxX, maxY, colorFrom, colorTo);
        end();
    }

    private static void drawGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        float a1 = (float) (startColor >> 24 & 255) / 255.0F;
        float r1 = (float) (startColor >> 16 & 255) / 255.0F;
        float g1 = (float) (startColor >> 8 & 255) / 255.0F;
        float b1 = (float) (startColor & 255) / 255.0F;
        float a2 = (float) (endColor >> 24 & 255) / 255.0F;
        float r2 = (float) (endColor >> 16 & 255) / 255.0F;
        float g2 = (float) (endColor >> 8 & 255) / 255.0F;
        float b2 = (float) (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(right, top, 0).color(r1, g1, b1, a1).endVertex();
        buffer.pos(left, top, 0).color(r1, g1, b1, a1).endVertex();
        buffer.pos(left, bottom, 0).color(r2, g2, b2, a2).endVertex();
        buffer.pos(right, bottom, 0).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public int drawString(FontRenderer font, String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, String text, int x, int y, int color, boolean dropShadow) {
        begin();
        int width = font.drawString(text, x, y, color, dropShadow);
        end();
        // font rendering leaves the color set
        GlStateManager.color(1, 1, 1, 1);
        return width;
    }

    public int drawString(FontRenderer font, String text, float x, float y, int color, boolean dropShadow) {
        begin();
        int width = font.drawString(text, x, y, color, dropShadow);
        end();
        GlStateManager.color(1, 1, 1, 1);
        return width;
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return drawString(font, text.getFormattedText(), x, y, color, true);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean dropShadow) {
        return drawString(font, text.getFormattedText(), x, y, color, dropShadow);
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        drawString(font, text, x - font.getStringWidth(text) / 2, y, color, true);
    }

    public void drawCenteredString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        drawCenteredString(font, text.getFormattedText(), x, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        begin();
        RenderHelper.enableGUIStandardItemLighting();
        minecraft.getRenderItem().renderItemAndEffectIntoGUI(minecraft.player, stack, x, y);
        RenderHelper.disableStandardItemLighting();
        end();
    }

    public void renderFakeItem(ItemStack stack, int x, int y) {
        begin();
        RenderHelper.enableGUIStandardItemLighting();
        minecraft.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        end();
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y) {
        renderItemDecorations(font, stack, x, y, null);
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y, String text) {
        begin();
        minecraft.getRenderItem().renderItemOverlayIntoGUI(font, stack, x, y, text);
        end();
    }

    public void renderTooltip(FontRenderer font, ItemStack stack, int x, int y) {
        ITooltipFlag flag = minecraft.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL;
        List<String> lines = stack.getTooltip(minecraft.player, flag);
        drawHoveringText(font, lines, x, y);
    }

    public void renderTooltip(FontRenderer font, ITextComponent text, int x, int y) {
        List<String> lines = new ArrayList<>();
        lines.add(text.getFormattedText());
        drawHoveringText(font, lines, x, y);
    }

    public void renderComponentTooltip(FontRenderer font, List<? extends ITextComponent> lines, int x, int y) {
        List<String> text = new ArrayList<>(lines.size());
        for (ITextComponent line : lines) {
            text.add(line.getFormattedText());
        }
        drawHoveringText(font, text, x, y);
    }

    private void drawHoveringText(FontRenderer font, List<String> lines, int x, int y) {
        begin();
        GuiUtils.drawHoveringText(lines, x, y, guiWidth(), guiHeight(), -1, font);
        end();
    }

    /**
     * Scissor in GUI coordinates (not affected by the pose, as upstream).
     */
    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        Window window = Window.get();
        double scale = window.getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (minX * scale), (int) (window.getHeight() - maxY * scale),
                Math.max(0, (int) ((maxX - minX) * scale)), Math.max(0, (int) ((maxY - minY) * scale)));
    }

    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
