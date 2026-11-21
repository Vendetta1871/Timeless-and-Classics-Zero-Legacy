package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class LivingEntityCrawl {
    private final EntityLivingBase shooter;
    private final ShooterDataHolder data;

    public LivingEntityCrawl(EntityLivingBase shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void crawl(boolean isCrawl) {
        setCrawling(isCrawl && canCrawl());
    }

    public void tickCrawling() {
        if (!data.isCrawling) {
            return;
        }
        // 1.12.2: there is no forced pose that something else could have changed (see setCrawling)
        boolean poseChanged = false;
        if (poseChanged || !canCrawl()) {
            setCrawling(false);
        }
    }

    private boolean canCrawl() {
        if (!SyncConfig.ENABLE_CRAWL.get()) {
            return false;
        }
        // 1.12.2: EntityLivingBase#jumping is protected, an entity that jumped is not on the ground anyway; swimming is
        // "in water"; there is no riptide spin attack
        boolean invalidState = !shooter.isEntityAlive() || (shooter instanceof EntityPlayer player && player.isSpectator()) || shooter.isRiding();
        boolean invalidMovement = shooter.isInWater() || !shooter.onGround;
        boolean incompatiblePose = shooter.isPlayerSleeping() || shooter.isElytraFlying();
        if (invalidState || invalidMovement || incompatiblePose) {
            return false;
        }
        // 玩家可以从未持过枪，也可能刚切换物品，以实际主手物品为准。
        ItemStack gunItem = shooter instanceof EntityPlayer ? shooter.getHeldItemMainhand()
                : data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        if (gunItem.getItem() instanceof IGun gun) {
            return gun.isCanCrawl(gunItem)
                    && TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem)).isPresent();
        }
        return shooter instanceof EntityPlayer && SyncConfig.ENABLE_CRAWL_WITHOUT_GUN.get();
    }

    private void setCrawling(boolean crawling) {
        if (data.isCrawling == crawling) {
            return;
        }
        data.isCrawling = crawling;
        // 只在 TACZ 的趴下状态变化时设置或清除一次，不持续写入姿态。
        // TODO(port): 1.12.2 has no Pose.SWIMMING / Player#setForcedPose / LivingEntity#setPose, so the crawl state is
        //  only synced, the entity is not put in the swimming pose
//        if (shooter instanceof EntityPlayer player) {
//            if (crawling || player.getForcedPose() == Pose.SWIMMING) {
//                player.setForcedPose(crawling ? Pose.SWIMMING : null);
//            }
//        } else {
//            shooter.setPose(crawling ? Pose.SWIMMING : Pose.STANDING);
//        }
    }
}
