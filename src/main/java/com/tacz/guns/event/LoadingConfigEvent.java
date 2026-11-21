package com.tacz.guns.event;

import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.util.AmmoBlockConfigRead;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;

/**
 * 1.12.2 has no {@code ModConfigEvent}: {@link #onLoadingConfig()} is called by {@code GunMod} once the configs are
 * loaded (the configs are only read at startup, so there is no reloading event).
 */
public class LoadingConfigEvent {
    /**
     * 客户端和服务端启动时，会触发此事件
     */
    public static void onLoadingConfig() {
        AmmoBlockConfigRead.init();
        HeadShotAABBConfigRead.init();
        InteractKeyConfigRead.init();
        GunPegboardItemConfigRead.init();
    }

    /**
     * 玩家进入服务端，或者服务端自动重置配置时，会触发此方法
     */
    public static void onReloadingConfig() {
        AmmoBlockConfigRead.init();
        HeadShotAABBConfigRead.init();
        InteractKeyConfigRead.init();
        GunPegboardItemConfigRead.init();
//            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientGunPackDownloadManager::downloadClientGunPack);
    }
}
