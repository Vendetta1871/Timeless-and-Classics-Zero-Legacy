package com.tacz.guns.client.input;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
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
public class RefitKey {
    public static final KeyBinding REFIT_KEY = new KeyBinding("key.tacz.refit.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_Z,
            "key.category.tacz");

    @SubscribeEvent
    public static void onRefitPress(InputEvent.KeyInputEvent event) {
        if (KeyInputs.isPress() && KeyInputs.matches(REFIT_KEY)) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (isInGame()) {
                if (IGun.mainHandHoldGun(player) && Minecraft.getMinecraft().currentScreen == null) {
                    IGun iGun = IGun.getIGunOrNull(player.getHeldItemMainhand());
                    if (iGun != null && iGun.hasAttachmentLock(player.getHeldItemMainhand())) {
                        return;
                    }
                    Minecraft.getMinecraft().displayGuiScreen(new GunRefitScreen());
                }
            } else if (Minecraft.getMinecraft().currentScreen instanceof GunRefitScreen refitScreen) {
                refitScreen.onClose();
            }
        }
    }
}
