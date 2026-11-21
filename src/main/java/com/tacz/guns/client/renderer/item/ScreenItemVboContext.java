package com.tacz.guns.client.renderer.item;

import com.tacz.guns.GunMod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tracks the actual screen draw, which occurs after any visible world has been rendered. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class ScreenItemVboContext {
    private static int depth;

    private ScreenItemVboContext() {}

    public static boolean isActive() {
        return depth > 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        depth++;
    }

    // ForgeHooksClient posts DrawScreenEvent.Post even when DrawScreenEvent.Pre cancels the screen body.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (depth > 0) depth--;
    }
}
