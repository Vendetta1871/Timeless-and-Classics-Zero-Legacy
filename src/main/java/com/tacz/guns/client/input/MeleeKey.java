package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.input.Keyboard;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
public class MeleeKey {
    public static final KeyBinding MELEE_KEY = new KeyBinding("key.tacz.melee.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_V,
            "key.category.tacz");

    public static boolean onMeleeControllerPress(boolean isPress) {
        return isPress && onMeleePress();
    }

    public static boolean onMeleePress() {
        if (!isInGame()) {
            return false;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (!operator.isAim()) {
            operator.melee();
            return true;
        }
        return false;
    }
}
