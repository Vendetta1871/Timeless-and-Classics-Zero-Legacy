package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 Forge {@code ForgeSlider} on top of Forge 1.12.2's {@link GuiSlider}; {@link #applyValue()} is called whenever
 * the value changes. Deviation: {@code stepSize} and {@code precision} are ignored (continuous value).
 */
@SideOnly(Side.CLIENT)
public class ForgeSlider extends GuiSlider implements Renderable, GuiEventListener {
    private static class Holder implements ISlider {
        ForgeSlider slider;

        @Override
        public void onChangeSliderValue(GuiSlider slider) {
            if (this.slider != null) {
                this.slider.applyValue();
            }
        }
    }

    public ForgeSlider(int x, int y, int width, int height, ITextComponent prefix, ITextComponent suffix, double minValue, double maxValue,
                       double currentValue, double stepSize, int precision, boolean drawString) {
        this(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, drawString, new Holder());
    }

    private ForgeSlider(int x, int y, int width, int height, ITextComponent prefix, ITextComponent suffix, double minValue, double maxValue,
                        double currentValue, boolean drawString, Holder holder) {
        super(0, x, y, width, height, prefix.getFormattedText(), suffix.getFormattedText(), minValue, maxValue, currentValue, false, drawString, holder);
        holder.slider = this;
    }

    public double getValue() {
        return super.getValue();
    }

    protected void applyValue() {
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // GuiSlider moves the handle while it is dragged from within drawButton
        graphics.setColor(1, 1, 1, 1);
        super.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return button == 0 && super.mousePressed(Minecraft.getMinecraft(), (int) mouseX, (int) mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased((int) mouseX, (int) mouseY);
        return false;
    }
}
