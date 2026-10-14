package com.tacz.guns.init;

import com.tacz.guns.command.RootCommand;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * 1.12.2: commands are registered in {@link FMLServerStartingEvent} (called from {@code GunMod}).
 */
public final class CommandRegistry {
    public static void onServerStaring(FMLServerStartingEvent event) {
        RootCommand.register(event);
    }
}
