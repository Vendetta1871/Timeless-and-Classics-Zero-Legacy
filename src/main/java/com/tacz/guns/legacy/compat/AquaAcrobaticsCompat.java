package com.tacz.guns.legacy.compat;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Compatibility with AquaAcrobatics (https://www.curseforge.com/minecraft/mc-mods/aqua-acrobatics), a 1.12.2 mod
 * that backports the modern swimming/crawl pose. 1.12.2 has no poses of its own, so AquaAcrobatics is what makes
 * the player actually crawl (its own keybind, with the "Enable Toggle Crawling" option on); TACZ does not add a
 * crawl key of its own and only mirrors that pose: {@link #onClientTick} copies it into the TACZ crawl state, so
 * gun animations and the gun rules for crawling follow the pose.
 * <p>
 * All direct references to AquaAcrobatics classes live in {@link Bridge}, which the JVM only loads when the mod
 * is present.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class AquaAcrobaticsCompat {
    public static final String MOD_ID = "aquaacrobatics";

    private static final boolean PRESENT = Loader.isModLoaded(MOD_ID);

    private AquaAcrobaticsCompat() {
    }

    /** AquaAcrobatics is installed and its crawl toggle is active (its config option and networking are on). */
    public static boolean isAvailable() {
        return PRESENT && Bridge.isToggleEnabled();
    }

    /** True while the local player forces the AquaAcrobatics crawl pose. */
    public static boolean isCrawling(EntityPlayerSP player) {
        return PRESENT && Bridge.isCrawling(player);
    }

    private static Boolean lastForcedCrawling = null;

    /**
     * Mirrors the server-synced AquaAcrobatics crawl state into the TACZ crawl state whenever it changes, so the
     * gun animations follow the real pose. {@code LocalPlayerCrawl} ignores the request unless a gun that allows
     * crawling is held, which keeps the per-weapon rules in charge.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (!isAvailable() || !SyncConfig.ENABLE_CRAWL.get() || player == null || mc.world == null) {
            lastForcedCrawling = null;
            return;
        }
        boolean forced = Bridge.isCrawling(player);
        if (lastForcedCrawling != null && forced != lastForcedCrawling && player instanceof IClientPlayerGunOperator operator) {
            operator.crawl(forced);
        }
        lastForcedCrawling = forced;
    }

    /** Loaded only when AquaAcrobatics is installed. */
    private static final class Bridge {
        private static boolean isToggleEnabled() {
            // registered by AquaAcrobatics in preInit only when its "Enable Toggle Crawling" option is on
            return com.fuzs.aquaacrobatics.network.NetworkHandler.INSTANCE != null;
        }

        private static boolean isCrawling(EntityPlayerSP player) {
            return player instanceof com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable resizeable
                    && resizeable.isForcingCrawling();
        }
    }
}
