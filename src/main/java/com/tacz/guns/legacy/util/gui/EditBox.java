package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 1.20.1 {@code EditBox} around a {@link GuiTextField}: {@code getValue / setValue / setHint / setResponder / setX /
 * setY}.
 */
@SideOnly(Side.CLIENT)
public class EditBox implements Renderable, GuiEventListener {
    private final GuiTextField field;
    private final FontRenderer font;
    @Nullable
    private ITextComponent hint;
    @Nullable
    private Consumer<String> responder;
    private String lastValue = "";

    public EditBox(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
        this.font = font;
        this.field = new GuiTextField(0, font, x, y, width, height);
    }

    public String getValue() {
        return field.getText();
    }

    public void setValue(String value) {
        field.setText(value);
        lastValue = value;
    }

    public void setHint(@Nullable ITextComponent hint) {
        this.hint = hint;
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder;
    }

    public void setX(int x) {
        field.x = x;
    }

    public void setY(int y) {
        field.y = y;
    }

    public int getX() {
        return field.x;
    }

    public int getY() {
        return field.y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        field.drawTextBox();
        if (field.getText().isEmpty() && !field.isFocused() && hint != null) {
            graphics.drawString(font, hint.getFormattedText(), field.x + 4, field.y + (field.height - 8) / 2, 0x707070, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = mouseX >= field.x && mouseX < field.x + field.width && mouseY >= field.y && mouseY < field.y + field.height;
        field.mouseClicked((int) mouseX, (int) mouseY, button);
        return inside;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        if (!field.isFocused()) {
            return false;
        }
        boolean handled = field.textboxKeyTyped(typedChar, keyCode);
        String value = field.getText();
        if (!value.equals(lastValue)) {
            lastValue = value;
            if (responder != null) {
                responder.accept(value);
            }
        }
        return handled;
    }

    @Override
    public void tick() {
        field.updateCursorCounter();
    }
}
