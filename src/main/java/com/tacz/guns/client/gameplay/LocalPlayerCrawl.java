package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCrawl;
import net.minecraft.client.entity.EntityPlayerSP;
import com.tacz.guns.legacy.compat.AquaAcrobaticsCompat;
import net.minecraft.item.ItemStack;

public class LocalPlayerCrawl {
    /**
     * 冷却时间为 10 tick
     */
    private static final int COOLDOWN_TICKS = 10;
    private final EntityPlayerSP player;
    private boolean isCrawling = false;
    private int crawCooldownTicks = 0;

    public LocalPlayerCrawl(EntityPlayerSP player) {
        this.player = player;
    }

    public void crawl(boolean isCrawl) {
        // 退出不能被冷却、切枪或离地拦截，且只同步实际发生的状态变化。
        if (!isCrawl) {
            setCrawling(false);
            return;
        }
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (isCrawling || crawCooldownTicks > 0 || !canCrawl()) {
            return;
        }
        if (mainHandItem.getItem() instanceof IGun gun
                && !TimelessAPI.getClientGunIndex(gun.getGunId(mainHandItem)).isPresent()) {
            return;
        }
        setCrawling(true);
        this.crawCooldownTicks = COOLDOWN_TICKS;
    }

    public void tickCrawl() {
        if (crawCooldownTicks > 0) {
            crawCooldownTicks--;
        }
        if (!isCrawling) {
            return;
        }
        // 1.12.2: there is no forced swimming pose; the crawl pose is AquaAcrobatics' one, mirrored by
        // AquaAcrobaticsCompat, so the state ends when that pose ends
        boolean poseChanged = !AquaAcrobaticsCompat.isCrawling(player);
        if (poseChanged || !canCrawl()) {
            setCrawling(false);
        }
    }

    public boolean isCrawling() {
        return isCrawling;
    }

    private boolean canCrawl() {
        if (!SyncConfig.ENABLE_CRAWL.get()) {
            return false;
        }
        boolean invalidState = !player.isEntityAlive() || player.isSpectator() || player.isRiding();
        boolean invalidMovement = player.movementInput.jump || player.isInWater() || !player.onGround;
        // 1.12.2: no riptide spin attack
        boolean incompatiblePose = player.isPlayerSleeping() || player.isElytraFlying();
        if (invalidState || invalidMovement || incompatiblePose) {
            return false;
        }
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (mainHandItem.getItem() instanceof IGun gun) {
            return gun.isCanCrawl(mainHandItem)
                    && TimelessAPI.getCommonGunIndex(gun.getGunId(mainHandItem)).isPresent();
        }
        return SyncConfig.ENABLE_CRAWL_WITHOUT_GUN.get();
    }

    private void setCrawling(boolean crawling) {
        if (isCrawling == crawling) {
            return;
        }
        isCrawling = crawling;
        // 1.12.2: no forced pose; the crawl pose comes from Aqua Acrobatics when installed
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerCrawl(crawling));
    }
}
