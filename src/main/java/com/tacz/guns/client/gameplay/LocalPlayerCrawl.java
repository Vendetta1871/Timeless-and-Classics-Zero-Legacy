package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
// TODO(port): needs ClientMessagePlayerCrawl
//import com.tacz.guns.network.message.ClientMessagePlayerCrawl;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
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
        // 持枪才能按键趴下
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 不允许趴下的武器
        if (!iGun.isCanCrawl(mainHandItem)) {
            return;
        }
        // 冷却时间没到，不执行
        if (crawCooldownTicks > 0) {
            return;
        }
        if (player.isSpectator() || player.isRiding() || !player.onGround) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            this.isCrawling = isCrawl;
            this.crawCooldownTicks = COOLDOWN_TICKS;
            // TODO(port): needs ClientMessagePlayerCrawl
            // NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerCrawl(isCrawl));
        });
    }

    public void tickCrawl() {
        if (crawCooldownTicks > 0) {
            crawCooldownTicks--;
        }
        // 持枪才能按键趴下
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 不允许趴下的武器，则取消趴下状态
        if (!iGun.isCanCrawl(mainHandItem)) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 如果获取不到 gunIndex，则取消趴下状态
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        if (!TimelessAPI.getCommonGunIndex(gunId).isPresent()) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 如果玩家是观察者模型、骑乘、跳跃、在游泳、不在地上，取消
        if (player.isSpectator() || player.isRiding() || player.movementInput.jump || player.isInWater() || !player.onGround) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        this.setCrawlPose();
    }

    public boolean isCrawling() {
        return isCrawling;
    }

    private void setCrawlPose() {
        // TODO(port): crawling (1.12.2 has no swimming pose to force); the state is tracked and synced only
//        if (isCrawling) {
//            player.setForcedPose(Pose.SWIMMING);
//        } else {
//            player.setForcedPose(null);
//        }
    }
}
