package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.tacz.guns.GunMod;
import net.minecraftforge.fml.common.Mod;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class AimKey {
    public static final KeyBinding AIM_KEY = new KeyBinding("key.tacz.aim.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            -100 + 1, // right mouse button
            "key.category.tacz");
    private static boolean controllerAimDown;

    public static boolean onAimPress() {
        return onAimInput(true);
    }

    /**
     * 1.12.2: whether the aim button should be left to vanilla interaction with the block or entity under the
     * crosshair instead of starting to aim ({@code KeyConfig.RMB_INTERACT_WITH_OBJECTS}).
     */
    private static boolean interactInsteadOfAim() {
        return KeyConfig.RMB_INTERACT_WITH_OBJECTS.get() && InteractKey.isTargetInteractable(Minecraft.getMinecraft());
    }

    /**
     * 合并键鼠和手柄的按住状态，同时保留切换武器时的瞄准预输入。
     */
    @SubscribeEvent
    public static void onAimHoldingPreInput(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !KeyConfig.HOLD_TO_AIM.get()) {
            return;
        }
        onAimInput(false);
    }

    public static boolean onAimControllerPress(boolean isPress) {
        controllerAimDown = isPress;
        return onAimInput(isPress);
    }

    public static void setControllerAimDown(boolean isDown) {
        controllerAimDown = isDown;
    }

    private static boolean onAimInput(boolean isPress) {
        if (!isInGame()) {
            return false;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return false;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return false;
        }
        // 1.12.2: the aim button can be left to interact with the target under the crosshair
        if (isPress && interactInsteadOfAim()) {
            return false;
        }
        boolean holdToAim = KeyConfig.HOLD_TO_AIM.get();
        if (!isPress && !holdToAim) {
            return false;
        }
        boolean aim = holdToAim ? AIM_KEY.isKeyDown() || controllerAimDown : !operator.isAim();
        // 1.12.2: do not start aiming while the aim button is held for interacting
        if (aim && !operator.isAim() && interactInsteadOfAim()) {
            return false;
        }
        if (operator.isAim() != aim) {
            operator.aim(aim);
        }
        return true;
    }

    @SubscribeEvent
    public static void cancelAim(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!isInGame()) {
            controllerAimDown = false;
        }
        EntityPlayerSP player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return;
        }
        if (operator.isAim() && (!isInGame() || player.isSpectator())) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
        }
    }
}
