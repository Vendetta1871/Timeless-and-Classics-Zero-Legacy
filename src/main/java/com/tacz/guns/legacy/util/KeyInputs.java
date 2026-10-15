package com.tacz.guns.legacy.util;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * 1.12.2 replacement for the 1.20.1 {@code InputEvent.Key} / {@code InputEvent.MouseButton.Post} payload
 * ({@code getKey / getAction / getButton}). Call from inside a {@code KeyInputEvent} / {@code MouseInputEvent}
 * handler: the LWJGL 2 event state is the event that is being dispatched. Mouse buttons are key codes {@code -100 + button}.
 */
@SideOnly(Side.CLIENT)
public final class KeyInputs {
    private KeyInputs() {
    }

    /**
     * {@code KeyMapping#matches(key, scanCode)} for the current keyboard event.
     */
    public static boolean matches(KeyBinding key) {
        return key.isActiveAndMatches(Keyboard.getEventKey());
    }

    /**
     * {@code KeyMapping#matchesMouse(button)} for the current mouse event; false for move / wheel events.
     */
    public static boolean matchesMouse(KeyBinding key) {
        int button = Mouse.getEventButton();
        return button >= 0 && key.isActiveAndMatches(-100 + button);
    }

    /**
     * {@code event.getAction() == GLFW_PRESS} for the current keyboard event (key repeats are not delivered in game).
     */
    public static boolean isPress() {
        return Keyboard.getEventKeyState();
    }

    public static boolean isRelease() {
        return !Keyboard.getEventKeyState();
    }

    /**
     * {@code event.getAction() == GLFW_PRESS} for the current mouse event.
     */
    public static boolean isMousePress() {
        return Mouse.getEventButtonState();
    }

    public static boolean isMouseRelease() {
        return !Mouse.getEventButtonState();
    }
}
