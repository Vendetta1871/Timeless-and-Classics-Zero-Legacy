package com.tacz.guns.legacy.compat;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Compatibility with AquaAcrobatics (https://www.curseforge.com/minecraft/mc-mods/aqua-acrobatics), a 1.12.2 mod
 * that backports the modern swimming/crawl pose. While it is installed and its "Enable Toggle Crawling" option is
 * on, the TACZ crawl key drives AquaAcrobatics' forced crawling, so the player really gets the crawl pose and
 * hitbox instead of only the synced TACZ crawl state (see {@code CrawlKey}).
 * <p>
 * AquaAcrobatics only supports toggling (there is no hold-to-crawl), and its state is authoritative: it lives on
 * the server and syncs back, so {@link #onClientTick} mirrors it into the TACZ crawl state on change and gun
 * animations follow the actual pose no matter which mod's key toggled it.
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

    /** True while the local player is allowed to toggle crawling (not flying, riding or on a ladder). */
    public static boolean canCrawl(EntityPlayerSP player) {
        return PRESENT && Bridge.canCrawl(player);
    }

    /** Sends AquaAcrobatics' own toggle packet; the server flips the forced crawling state and syncs it back. */
    public static void toggle() {
        if (PRESENT) {
            Bridge.toggle();
        }
    }

    /**
     * True when AquaAcrobatics' own crawl keybind is bound to the same physical key as {@code key}. Its key
     * handler already toggles crawling for that key, so TACZ must not drive it again on the same press.
     */
    public static boolean isSameKey(KeyBinding key) {
        return PRESENT && Bridge.isSameKey(key);
    }

    @Nullable
    private static Boolean lastForcedCrawling = null;

    /**
     * Mirrors the server-synced AquaAcrobatics crawl state into the TACZ crawl state whenever it changes, so the
     * gun animations follow the real pose. {@code LocalPlayerCrawl} ignores the request unless a gun that allows
     * crawling is held, which keeps the gun rules of {@code CrawlKey} in charge.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (!isAvailable() || player == null || mc.world == null) {
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

        private static boolean canCrawl(EntityPlayerSP player) {
            return player instanceof com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable resizeable
                    && resizeable.canForceCrawling();
        }

        private static void toggle() {
            com.fuzs.aquaacrobatics.network.NetworkHandler.INSTANCE.sendToServer(
                    new com.fuzs.aquaacrobatics.network.message.PacketSendKey(
                            com.fuzs.aquaacrobatics.network.message.PacketSendKey.KeybindPacket.TOGGLE_CRAWLING));
        }

        private static boolean isSameKey(KeyBinding key) {
            KeyBinding crawl = com.fuzs.aquaacrobatics.util.Keybindings.forceCrawling;
            return crawl != null && crawl.getKeyCode() == key.getKeyCode();
        }
    }
}
