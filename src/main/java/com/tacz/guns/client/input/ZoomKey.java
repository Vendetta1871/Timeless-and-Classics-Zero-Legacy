package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerZoom;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.tacz.guns.GunMod;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.input.Keyboard;
import com.tacz.guns.legacy.util.KeyInputs;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class ZoomKey {
    public static final KeyBinding ZOOM_KEY = new KeyBinding("key.tacz.zoom.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_V,
            "key.category.tacz");

    @SubscribeEvent
    public static void onZoomKeyPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress() && KeyInputs.matches(ZOOM_KEY)) {
            doZoomLogic();
        }
    }

    @SubscribeEvent
    public static void onZoomMousePress(InputEvent.MouseInputEvent event) {
        if (isInGame() && KeyInputs.isMousePress() && KeyInputs.matchesMouse(ZOOM_KEY)) {
            doZoomLogic();
        }
    }

    public static boolean onZoomControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (operator.isAim()) {
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerZoom());
                return true;
            }
        }
        return false;
    }

    private static void doZoomLogic() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator.isAim()) {
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerZoom());
        }
    }
}
