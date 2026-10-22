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
import com.tacz.guns.legacy.compat.AquaAcrobaticsCompat;
import com.tacz.guns.legacy.util.KeyInputs;
import net.minecraft.item.ItemStack;

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
            if (AquaAcrobaticsCompat.isAvailable()) {
                crawlWithAquaAcrobatics(player, operator);
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

    /**
     * AquaAcrobatics installed: the key drives its forced crawling (the real crawl pose and hitbox). Its API is
     * toggle only, so {@code KeyConfig.HOLD_TO_CRAWL} has no effect while the mod is installed. The TACZ crawl
     * state comes back through {@code AquaAcrobaticsCompat#onClientTick} once the server flipped the pose.
     */
    private static void crawlWithAquaAcrobatics(EntityPlayerSP player, IClientPlayerGunOperator operator) {
        // When both mods share the same physical key, AquaAcrobatics' own handler toggles the pose; driving it
        // here as well would flip it twice per press
        if (AquaAcrobaticsCompat.isSameKey(CRAWL_KEY)) {
            return;
        }
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        if (!KeyInputs.isPress() || !AquaAcrobaticsCompat.canCrawl(player)) {
            return;
        }
        boolean target = !AquaAcrobaticsCompat.isCrawling(player);
        // a weapon that disallows crawling must not start it; stopping is always allowed
        if (target && !iGun.isCanCrawl(mainHandItem)) {
            return;
        }
        AquaAcrobaticsCompat.toggle();
        // immediate local feedback; the mirrored state from the server confirms or corrects it
        operator.crawl(target);
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
        if (AquaAcrobaticsCompat.isAvailable()) {
            // toggle only (AquaAcrobatics has no hold-to-crawl); see crawlWithAquaAcrobatics
            if (!isPress || !AquaAcrobaticsCompat.canCrawl(player)) {
                return false;
            }
            ItemStack mainHandItem = player.getHeldItemMainhand();
            boolean target = !AquaAcrobaticsCompat.isCrawling(player);
            if (target && mainHandItem.getItem() instanceof IGun iGun && !iGun.isCanCrawl(mainHandItem)) {
                return false;
            }
            AquaAcrobaticsCompat.toggle();
            operator.crawl(target);
            return true;
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
