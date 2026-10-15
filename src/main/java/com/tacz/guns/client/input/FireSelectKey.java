package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
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
public class FireSelectKey {
    public static final KeyBinding FIRE_SELECT_KEY = new KeyBinding("key.tacz.fire_select.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_G,
            "key.category.tacz");

    @SubscribeEvent
    public static void onFireSelectKeyPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress() && KeyInputs.matches(FIRE_SELECT_KEY)) {
            doFireSelectLogic();
        }
    }

    @SubscribeEvent
    public static void onFireSelectMousePress(InputEvent.MouseInputEvent event) {
        if (isInGame() && KeyInputs.isMousePress() && KeyInputs.matchesMouse(FIRE_SELECT_KEY)) {
            doFireSelectLogic();
        }
    }

    public static boolean onFireSelectControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            if (IGun.mainHandHoldGun(player)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).fireSelect();
                return true;
            }
        }
        return false;
    }

    private static void doFireSelectLogic() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        if (IGun.mainHandHoldGun(player)) {
            IClientPlayerGunOperator.fromLocalPlayer(player).fireSelect();
        }
    }
}
