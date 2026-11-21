package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
public class UnloadKey {
    public static final KeyBinding UNLOAD_KEY = new KeyBinding("key.tacz.unload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            Keyboard.KEY_R,
            "key.category.tacz");

    public static void onUnloadPress() {
        if (!isInGame()) {
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && player.isEntityAlive() && !player.isSpectator()) {
            IClientPlayerGunOperator.fromLocalPlayer(player).unloadAmmo();
        }
    }
}
