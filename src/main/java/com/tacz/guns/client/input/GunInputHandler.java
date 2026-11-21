package com.tacz.guns.client.input;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

/**
 * 1.12.2: key codes are LWJGL 2 ints instead of {@code InputConstants.Key}: keyboard keys are {@code Keyboard.KEY_*}
 * (or {@code char + 256} for keys without a code), mouse buttons are {@code -100 + button}.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class GunInputHandler {
    private static final List<PressBinding> PRESS_BINDINGS = Collections.unmodifiableList(Arrays.asList(
            new PressBinding(UnloadKey.UNLOAD_KEY, UnloadKey::onUnloadPress),
            new PressBinding(AimKey.AIM_KEY, AimKey::onAimPress),
            // 1.12.2: no CrawlKey - the port relies on AquaAcrobatics' own crawl keybind and only mirrors its pose
            new PressBinding(ReloadKey.RELOAD_KEY, ReloadKey::onReloadPress),
            new PressBinding(InspectKey.INSPECT_KEY, InspectKey::onInspectPress),
            new PressBinding(FireSelectKey.FIRE_SELECT_KEY, FireSelectKey::onFireSelectPress),
            new PressBinding(MeleeKey.MELEE_KEY, MeleeKey::onMeleePress),
            new PressBinding(ZoomKey.ZOOM_KEY, ZoomKey::onZoomPress),
            new PressBinding(InteractKey.INTERACT_KEY, InteractKey::onInteractPress),
            new PressBinding(RefitKey.REFIT_KEY, RefitKey::onRefitPress),
            new PressBinding(ConfigKey.OPEN_CONFIG_KEY, ConfigKey::onOpenConfig)
    ));

    private GunInputHandler() {
    }

    /**
     * 1.12.2: there is no {@code RegisterKeyMappingsEvent}; called from the client setup instead.
     */
    public static void registerKeyMappings() {
        for (PressBinding binding : PRESS_BINDINGS) {
            ClientRegistry.registerKeyBinding(binding.keyMapping());
        }
        ClientRegistry.registerKeyBinding(ShootKey.SHOOT_KEY);
    }

    /**
     * 1.12.2: {@code KeyInputEvent} only fires while no screen takes the input (in game), see {@link #onScreenKeyPress}.
     */
    @SubscribeEvent
    public static void onKeyPress(InputEvent.KeyInputEvent event) {
        int key = getEventKey();
        if (!Keyboard.getEventKeyState()) {
            onRelease(key);
        }
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) {
            return;
        }
        if (!closeRefitScreen(key)) {
            onPress(key);
        }
    }

    @SubscribeEvent
    public static void onMousePress(InputEvent.MouseInputEvent event) {
        int button = Mouse.getEventButton();
        // 1.12.2: mouse move and wheel events carry no button
        if (button < 0) {
            return;
        }
        int key = -100 + button;
        if (!Mouse.getEventButtonState()) {
            onRelease(key);
        } else {
            onPress(key);
        }
    }

    /**
     * 1.12.2: a screen consumes the keyboard input itself, so the refit key closes the refit screen from the
     * screen's input event (the 1.20.1 {@code InputEvent.Key} also fires while a screen is open).
     */
    @SubscribeEvent
    public static void onScreenKeyPress(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent() && closeRefitScreen(getEventKey())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMousePressPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        int button = Mouse.getEventButton();
        if (button >= 0 && Mouse.getEventButtonState()
                && closeRefitScreen(-100 + button)) {
            // 消费关闭界面的点击，避免同一次输入在 Post 阶段重新打开界面。
            event.setCanceled(true);
        }
    }

    private static int getEventKey() {
        return Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
    }

    private static void onPress(int key) {
        for (PressBinding binding : PRESS_BINDINGS) {
            // 某个动作可能打开界面，后续动作不能继续穿透到游戏中。
            if (!isInGame()) {
                return;
            }
            if (binding.keyMapping().isActiveAndMatches(key)) {
                binding.action().run();
                // 即使卸弹条件不满足，也不能让 Alt+R 穿透成普通换弹。
                if (binding.keyMapping() == UnloadKey.UNLOAD_KEY) {
                    return;
                }
            }
        }
    }

    private static boolean closeRefitScreen(int key) {
        Minecraft mc = Minecraft.getMinecraft();
        // 1.12.2: no loading overlay (resource reloads block the game thread)
        if (!Display.isActive() || !(mc.currentScreen instanceof GunRefitScreen refitScreen)) {
            return false;
        }
        KeyBinding refitKey = RefitKey.REFIT_KEY;
        if (key != Keyboard.KEY_NONE && key == refitKey.getKeyCode()
                && refitKey.getKeyModifier().isActive(refitKey.getKeyConflictContext())) {
            refitScreen.onClose();
            return true;
        }
        return false;
    }

    private static void onRelease(int key) {
        // 1.12.2: KeyBinding has no setDown; setKeyBindState releases every binding on the key code, whatever its
        // modifier, like upstream's loop over the bindings
        for (PressBinding binding : PRESS_BINDINGS) {
            if (key == binding.keyMapping().getKeyCode()) {
                KeyBinding.setKeyBindState(key, false);
            }
        }
        if (key == ShootKey.SHOOT_KEY.getKeyCode()) {
            KeyBinding.setKeyBindState(key, false);
        }
    }

    private static final class PressBinding {
        private final KeyBinding keyMapping;
        private final Runnable action;

        private PressBinding(KeyBinding keyMapping, Runnable action) {
            this.keyMapping = keyMapping;
            this.action = action;
        }

        public KeyBinding keyMapping() {
            return keyMapping;
        }

        public Runnable action() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PressBinding)) return false;
            PressBinding that = (PressBinding) o;
            return java.util.Objects.equals(keyMapping, that.keyMapping) && java.util.Objects.equals(action, that.action);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(keyMapping, action);
        }

        @Override
        public String toString() {
            return "PressBinding[" + "keyMapping=" + keyMapping + ", " + "action=" + action + "]";
        }
    }
}
