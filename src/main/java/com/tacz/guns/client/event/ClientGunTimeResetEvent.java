package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.particle.MuzzleParticleManager;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.client.resource.pojo.display.gun.RangefinderManager;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Clears timestamps tied to a client world after SBM resets its animation clock. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class ClientGunTimeResetEvent {
    private ClientGunTimeResetEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoggingOut(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // 1.12.2: fired on the network thread; reset on the client thread
        Minecraft.getMinecraft().addScheduledTask(ClientGunTimeResetEvent::reset);
    }

    private static void reset() {
        // TODO(port): ScopeLensRenderer.close() once scope lens effects exist
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) {
            LocalPlayerDataHolder dataHolder = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
            dataHolder.cancelShootFutures();
            dataHolder.resetAmmoCountCache();
        }
        GunTime.resetEpoch();
        LocalPlayerDataHolder.clientClickButtonTimestamp = -1L;
        RefitTransform.resetTimestamps();
        RenderCrosshairEvent.resetTimestamps();
        KillAmountOverlay.resetTimestamp();
        ClientHitMark.resetTimestamp();
        GunHudOverlay.resetTimestamp();
        FirstPersonRenderGunEvent.resetTimestamps();
        CameraSetupEvent.resetTimestamp();
        MuzzleFlashRender.resetTimestamps();
        MuzzleParticleManager.resetMuzzleLight();
        ThirdPersonMuzzleParticleManager.resetTimestamps();
        RangefinderManager.resetTimestamps();
        GunHurtBobTweak.resetTimestamp();
    }
}
