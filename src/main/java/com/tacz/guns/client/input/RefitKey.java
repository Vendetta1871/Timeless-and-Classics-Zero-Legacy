package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
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
public class RefitKey {
    public static final KeyBinding REFIT_KEY = new KeyBinding("key.tacz.refit.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_Z,
            "key.category.tacz");

    public static void onRefitPress() {
        if (!isInGame()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || player.isSpectator() || !IGun.mainHandHoldGun(player)) {
            return;
        }
        if (IClientPlayerGunOperator.fromLocalPlayer(player).isCharging()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(player.getHeldItemMainhand());
        if (iGun != null && iGun.hasAttachmentLock(player.getHeldItemMainhand())) {
            return;
        }
        mc.displayGuiScreen(new GunRefitScreen());
    }
}
