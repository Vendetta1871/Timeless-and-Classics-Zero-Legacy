package com.tacz.guns.client.input;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.tacz.guns.GunMod;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.input.Keyboard;
import com.tacz.guns.legacy.util.KeyInputs;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class ReloadKey {
    public static final KeyBinding RELOAD_KEY = new KeyBinding("key.tacz.reload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_R,
            "key.category.tacz");

    @SubscribeEvent
    public static void onReloadPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress() && KeyInputs.matches(RELOAD_KEY)) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (player.getHeldItemMainhand().getItem() instanceof IGun iGun) {
                // 如果使用背包直读，且没有换弹冷却机制，则在输入时就屏蔽换弹
                if (iGun.useInventoryAmmo(player.getHeldItemMainhand())) {
                    return;
                }
                IClientPlayerGunOperator.fromLocalPlayer(player).reload();
            }
        }
    }

    public static boolean onReloadControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            if (IGun.mainHandHoldGun(player)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).reload();
                return true;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void autoReload(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.CLIENT) {
            return;
        }

        if (!KeyConfig.AUTO_RELOAD.get()) {
            return;
        }

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator() || player.ticksExisted % 5 != 0) {
            return;
        }
        ItemStack currentGunItem = player.getHeldItemMainhand();
        if (player.getHeldItemMainhand().getItem() instanceof IGun iGun) {
            // 如果使用背包直读，且没有换弹冷却机制，则在输入时就屏蔽换弹
            if (iGun.useInventoryAmmo(player.getHeldItemMainhand())) {
                return;
            }
            boolean flag = TimelessAPI.getCommonGunIndex(iGun.getGunId(currentGunItem))
                    .map(gunIndex -> gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT)
                    .orElse(false);

            int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (iGun.hasBulletInBarrel(currentGunItem) && flag ? 1 : 0);
            if (ammoCount > 0) {
                return;
            }
            IClientPlayerGunOperator.fromLocalPlayer(player).reload();
        }
    }
}
