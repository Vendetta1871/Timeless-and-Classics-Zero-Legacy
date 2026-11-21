package com.github.mcmodderanchor.simplebedrockmodel.v1.client.event;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.PausedClientAnimationClock;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleBedrockModel.EVENT_MOD_ID, value = Side.CLIENT)
public final class ClientAnimationClockTicker {
    private ClientAnimationClockTicker() {}

    public static AnimationClock getAnimationClock() {
        return PausedClientAnimationClock.getInstance();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PausedClientAnimationClock.getInstance().update();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // 1.12.2: fired on the network thread; reset on the client thread (before TACZ ClientGunTimeResetEvent,
        // which has the lowest priority)
        Minecraft.getMinecraft().addScheduledTask(() -> PausedClientAnimationClock.getInstance().reset());
    }
}
