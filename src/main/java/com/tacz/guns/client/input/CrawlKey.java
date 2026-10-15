package com.tacz.guns.client.input;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import com.tacz.guns.config.sync.SyncConfig;
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
public class CrawlKey {
    public static final KeyBinding CRAWL_KEY = new KeyBinding("key.tacz.crawl.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_C,
            "key.category.tacz");

    @SubscribeEvent
    public static void onCrawlPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.matches(CRAWL_KEY)) {
            if (!SyncConfig.ENABLE_CRAWL.get()) {
                return;
            }
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator() || player.isRiding()) {
                return;
            }
            if (!(player instanceof IClientPlayerGunOperator operator)) {
                return;
            }
            if (player.getHeldItemMainhand().getItem() instanceof IGun iGun) {
                // 如果不允许下蹲，则禁止进行下蹲
                if (!iGun.isCanCrawl(player.getHeldItemMainhand())) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).crawl(false);
                    return;
                }
                boolean action = true;
                if (!KeyConfig.HOLD_TO_CRAWL.get()) {
                    action = !operator.isCrawl();
                }
                if (KeyInputs.isPress()) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).crawl(action);
                }
                if (KeyConfig.HOLD_TO_CRAWL.get() && KeyInputs.isRelease()) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).crawl(false);
                }
            }
        }
    }

    public static boolean onCrawlControllerPress(boolean isPress) {
        if (!isInGame()) {
            return false;
        }
        if (!SyncConfig.ENABLE_CRAWL.get()) {
            return false;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.isSpectator() || player.isRiding()) {
            return false;
        }
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return false;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return false;
        }
        boolean action = true;
        if (!KeyConfig.HOLD_TO_CRAWL.get()) {
            action = !operator.isCrawl();
        }
        if (isPress) {
            IClientPlayerGunOperator.fromLocalPlayer(player).crawl(action);
            return true;
        }
        if (KeyConfig.HOLD_TO_CRAWL.get()) {
            IClientPlayerGunOperator.fromLocalPlayer(player).crawl(false);
            return true;
        }
        return false;
    }
}
