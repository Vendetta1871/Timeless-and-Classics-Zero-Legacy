package com.tacz.guns.legacy.debug;

import com.tacz.guns.GunMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Development-only checks, active only when the {@code TACZ_AUTOTEST} environment variable is set.
 * Not part of the port; removed once real gun items render.
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public final class DevTools {
    public static final boolean ENABLED = System.getenv("TACZ_AUTOTEST") != null;
    /**
     * Root of an unpacked gun pack, e.g. the upstream default pack.
     */
    public static final String PACK_DIR = System.getenv("TACZ_DEBUG_PACK");
    public static Item debugGun;

    private DevTools() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!ENABLED) {
            return;
        }
        debugGun = new Item().setMaxStackSize(1).setTranslationKey("tacz.debug_gun").setCreativeTab(CreativeTabs.COMBAT)
                .setRegistryName(GunMod.MOD_ID, "debug_gun");
        event.getRegistry().register(debugGun);
    }
}
