package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 1.20.1 {@code Button} / {@code AbstractWidget} on top of {@link GuiButton}: subclasses override
 * {@link #renderWidget} and {@link #onPress()}, and use {@code getX() / getY() / width / height / isHoveredOrFocused()}.
 * The widget is not put into the screen's {@code buttonList}: {@link Screen} / {@link AbstractContainerScreen} draw it
 * through {@link Renderable} and deliver clicks through {@link GuiEventListener}. Without an override of
 * {@code renderWidget} the vanilla 1.12.2 button is drawn.
 * <p>
 * Deviation: no narration, the 1.20.1 {@code DEFAULT_NARRATION} constructor argument is dropped.
 */
@SideOnly(Side.CLIENT)
public class Button extends GuiButton implements Renderable, GuiEventListener {
    @FunctionalInterface
    public interface OnPress {
        void onPress(Button button);
    }

    protected final OnPress onPress;
    private ITextComponent message;
    private List<ITextComponent> tooltip;

    public Button(int x, int y, int width, int height, ITextComponent message, OnPress onPress) {
        super(0, x, y, width, height, message.getFormattedText());
        this.message = message;
        this.onPress = onPress;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public ITextComponent getMessage() {
        return message;
    }

    public void setMessage(ITextComponent message) {
        this.message = message;
        this.displayString = message.getFormattedText();
    }

    public boolean isHoveredOrFocused() {
        return this.hovered;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public boolean isActive() {
        return this.enabled && this.visible;
    }

    public void setTooltip(List<ITextComponent> tooltip) {
        this.tooltip = tooltip;
    }

    public void setTooltip(ITextComponent tooltip) {
        this.tooltip = java.util.Collections.singletonList(tooltip);
    }

    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Default look: the vanilla 1.12.2 button.
     */
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTick);
    }

    /**
     * Hover tooltip set by {@link #setTooltip}, drawn by the screen over all widgets.
     */
    void renderTooltip(GuiGraphics graphics, FontRenderer font, int mouseX, int mouseY) {
        if (this.visible && this.hovered && tooltip != null) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.visible && this.enabled && mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height) {
            this.playPressSound(Minecraft.getMinecraft().getSoundHandler());
            this.onPress();
            return true;
        }
        return false;
    }

    /**
     * {@code AbstractWidget#renderScrollingString(graphics, font, margin, color)}: the message centered in the widget, or
     * scrolling back and forth when it does not fit.
     */
    protected void renderScrollingString(GuiGraphics graphics, FontRenderer font, int margin, int color) {
        int minX = this.x + margin;
        int maxX = this.x + this.width - margin;
        renderScrollingString(graphics, font, this.getMessage(), minX, this.y, maxX, this.y + this.height, color);
    }

    protected static void renderScrollingString(GuiGraphics graphics, FontRenderer font, ITextComponent text, int minX, int minY, int maxX, int maxY, int color) {
        String string = text.getFormattedText();
        int textWidth = font.getStringWidth(string);
        int textY = (minY + maxY - font.FONT_HEIGHT) / 2 + 1;
        int available = maxX - minX;
        if (textWidth > available) {
            int overflow = textWidth - available;
            double seconds = (double) Minecraft.getSystemTime() / 1000.0;
            double period = Math.max((double) overflow * 0.5, 3.0);
            double wave = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * seconds / period)) / 2.0 + 0.5;
            double offset = wave * overflow;
            graphics.enableScissor(minX, minY, maxX, maxY);
            graphics.drawString(font, string, minX - (int) offset, textY, color, true);
            graphics.disableScissor();
        } else {
            int center = Math.max(minX + textWidth / 2, Math.min((minX + maxX) / 2, maxX - textWidth / 2));
            graphics.drawCenteredString(font, string, center, textY, color);
        }
    }

    /**
     * {@code Component.empty()}
     */
    public static ITextComponent empty() {
        return new TextComponentString("");
    }
}
