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
public class MeleeKey {
    public static final KeyBinding MELEE_KEY = new KeyBinding("key.tacz.melee.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_V,
            "key.category.tacz");

    @SubscribeEvent
    public static void onMeleeKeyPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress() && KeyInputs.matches(MELEE_KEY)) {
            doMeleeLogic();
        }
    }

    @SubscribeEvent
    public static void onMeleeMousePress(InputEvent.MouseInputEvent event) {
        if (isInGame() && KeyInputs.isMousePress() && KeyInputs.matchesMouse(MELEE_KEY)) {
            doMeleeLogic();
        }
    }

    public static boolean onMeleeControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (!operator.isAim()) {
                operator.melee();
                return true;
            }
        }
        return false;
    }

    private static void doMeleeLogic() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (!operator.isAim()) {
            operator.melee();
        }
    }
}
