package com.tacz.guns.client.event;

// TODO(port): needs GunRefitScreen
//import com.tacz.guns.client.gui.GunRefitScreen;
// TODO(port): needs GunSmithTableScreen
//import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class PreventsHotbarEvent {
    @SubscribeEvent
    public static void onRenderHotbarEvent(RenderGameOverlayEvent.Pre event) {
        // todo 需要测试行为
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        // 枪械合成台界面关闭背景
        // TODO(port): needs GunSmithTableScreen
//        if (screen instanceof GunSmithTableScreen) {
//            event.setCanceled(true);
//            return;
//        }
        // 枪械改装界面关闭背景
        // TODO(port): needs GunRefitScreen
//        if (screen instanceof GunRefitScreen) {
//            event.setCanceled(true);
//        }
    }
}
