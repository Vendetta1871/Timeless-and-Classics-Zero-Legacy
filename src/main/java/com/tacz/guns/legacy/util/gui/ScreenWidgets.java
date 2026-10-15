package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * The widget list of a screen ({@code Screen#renderables} / {@code children}) with event dispatch, shared by
 * {@link Screen} and {@link AbstractContainerScreen}. Widgets are not added to the 1.12.2 {@code buttonList}.
 */
@SideOnly(Side.CLIENT)
final class ScreenWidgets {
    final List<Renderable> renderables = new ArrayList<>();
    final List<GuiEventListener> children = new ArrayList<>();
    private GuiEventListener dragging;

    <T> T add(T widget) {
        if (widget instanceof Renderable) {
            renderables.add((Renderable) widget);
        }
        if (widget instanceof GuiEventListener) {
            children.add((GuiEventListener) widget);
        }
        return widget;
    }

    void clear() {
        renderables.clear();
        children.clear();
        dragging = null;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, FontRenderer font) {
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        // tooltips of hovered buttons go over every widget
        for (Renderable renderable : renderables) {
            if (renderable instanceof Button) {
                ((Button) renderable).renderTooltip(graphics, font, mouseX, mouseY);
            }
        }
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        // the last added widget is drawn on top
        for (int i = children.size() - 1; i >= 0; i--) {
            GuiEventListener child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button)) {
                dragging = child;
                return true;
            }
        }
        return false;
    }

    boolean mouseReleased(double mouseX, double mouseY, int button) {
        GuiEventListener target = dragging;
        dragging = null;
        boolean handled = false;
        for (GuiEventListener child : new ArrayList<>(children)) {
            handled |= child.mouseReleased(mouseX, mouseY, button);
        }
        return handled || target != null;
    }

    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return dragging != null && dragging.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    boolean keyTyped(char typedChar, int keyCode) {
        for (GuiEventListener child : children) {
            if (child.keyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    void tick() {
        for (GuiEventListener child : new ArrayList<>(children)) {
            child.tick();
        }
    }
}
