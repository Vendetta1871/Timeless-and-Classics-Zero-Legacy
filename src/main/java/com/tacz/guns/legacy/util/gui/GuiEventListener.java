package com.tacz.guns.legacy.util.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code GuiEventListener}, reduced to the input a TACZ widget uses. 1.12.2 screens have no widget tree, so
 * {@link ScreenWidgets} dispatches these calls to every widget that was added to the screen. {@code keyTyped} is the
 * 1.12.2 key callback (typed character + LWJGL 2 key code) in place of {@code keyPressed} / {@code charTyped}.
 */
@SideOnly(Side.CLIENT)
public interface GuiEventListener {
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    default boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    /**
     * Called every client tick while the screen is open.
     */
    default void tick() {
    }
}
