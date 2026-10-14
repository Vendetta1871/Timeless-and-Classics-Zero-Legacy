package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
// TODO(port): needs ClientMessagePlayerAim
//import com.tacz.guns.network.message.ClientMessagePlayerAim;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.util.LegacyMath;
import com.tacz.guns.legacy.util.Optionals;

public class LocalPlayerAim {
    private final LocalPlayerDataHolder data;
    private final EntityPlayerSP player;

    public LocalPlayerAim(LocalPlayerDataHolder data, EntityPlayerSP player) {
        this.data = data;
        this.player = player;
    }

    public void aim(boolean isAim) {
        // 暂定为主手
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            data.clientIsAiming = isAim;
            // 发送切换开火模式的数据包，通知服务器
            // TODO(port): needs ClientMessagePlayerAim
            // NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAim(isAim));
        });
    }

    public float getClientAimingProgress(float partialTicks) {
        return LegacyMath.lerp(partialTicks, LocalPlayerDataHolder.oldAimingProgress, data.clientAimingProgress);
    }

    public boolean isAim() {
        return data.clientIsAiming;
    }

    public void tickAimingProgress() {
        ItemStack mainHandItem = player.getHeldItemMainhand();
        // 如果主手物品不是枪械，则取消瞄准状态并将 aimingProgress 归零，返回。
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            data.clientAimingProgress = 0;
            LocalPlayerDataHolder.oldAimingProgress = 0;
            return;
        }
        // 如果正在收枪，则不能瞄准
        if (System.currentTimeMillis() - data.clientDrawTimestamp < 0) {
            data.clientIsAiming = false;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        Optionals.ifPresentOrElse(TimelessAPI.getCommonGunIndex(gunId), index -> {
            float alphaProgress = this.getAlphaProgress(index.getGunData());
            this.aimProgressCalculate(alphaProgress);
        }, () -> {
            data.clientAimingProgress = 0;
            LocalPlayerDataHolder.oldAimingProgress = 0;
        });
    }

    private void aimProgressCalculate(float alphaProgress) {
        LocalPlayerDataHolder.oldAimingProgress = data.clientAimingProgress;
        if (data.clientIsAiming) {
            // 处于执行瞄准状态，增加 aimingProgress
            data.clientAimingProgress += alphaProgress;
            if (data.clientAimingProgress > 1) {
                data.clientAimingProgress = 1;
            }
        } else {
            // 处于取消瞄准状态，减小 aimingProgress
            data.clientAimingProgress -= alphaProgress;
            if (data.clientAimingProgress < 0) {
                data.clientAimingProgress = 0;
            }
        }
        data.clientAimingTimestamp = System.currentTimeMillis();
    }

    private float getAlphaProgress(GunData gunData) {
        float aimTime = gunData.getAimTime();
        IGunOperator operator = IGunOperator.fromLivingEntity(this.player);
        if (operator.getCacheProperty() != null) {
            aimTime = operator.getCacheProperty().<Float>getCache(AdsModifier.ID);
        }
        aimTime = Math.max(0, aimTime);
        return (System.currentTimeMillis() - data.clientAimingTimestamp + 1) / (aimTime * 1000);
    }
}
