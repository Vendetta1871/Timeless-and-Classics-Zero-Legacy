package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class CommonNetworkCacheEvent {
    /**
     * 1.12.2: {@code ClientPlayerNetworkEvent.LoggingIn} is {@code ClientConnectedToServerEvent}, fired on the network
     * thread, so the work is moved to the client thread.
     */
    @SubscribeEvent
    public static void onClientPlayerLoggingIn(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event.getManager() == null || event.getManager().isLocalChannel()) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(() -> {
            CommonAssetsManager.clearInstance();
            CommonNetworkCache.INSTANCE.clear();
            ClientIndexManager.clear();
        });
    }
}
