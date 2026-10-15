package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
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
public class InspectKey {
    public static final KeyBinding INSPECT_KEY = new KeyBinding("key.tacz.inspect.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_H,
            "key.category.tacz");

    @SubscribeEvent
    public static void onInspectPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress() && KeyInputs.matches(INSPECT_KEY)) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            IClientPlayerGunOperator.fromLocalPlayer(player).inspect();
        }
    }

    public static boolean onInspectControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            IClientPlayerGunOperator.fromLocalPlayer(player).inspect();
            return true;
        }
        return false;
    }
}
