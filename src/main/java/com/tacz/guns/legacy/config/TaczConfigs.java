package com.tacz.guns.legacy.config;

import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.ServerConfig;

import java.io.File;

/**
 * 1.12.2 replacement for the {@code ModLoadingContext.registerConfig(...)} calls of upstream's {@code GunMod}.
 * <p>
 * Call {@link #init(File)} once, as early as possible (pre-init), with the Forge config directory
 * ({@code FMLPreInitializationEvent#getModConfigurationDirectory()}). It builds the specs, loads (and creates)
 * {@code tacz-common.cfg}, {@code tacz-server.cfg} and {@code tacz-client.cfg} and fills every static config value.
 * Use {@link #init(File, boolean)} with {@code client = false} on a dedicated server to skip the client file.
 * {@link #save()} writes all loaded files back.
 */
public final class TaczConfigs {
    public static ForgeConfigSpec COMMON_SPEC;
    public static ForgeConfigSpec SERVER_SPEC;
    public static ForgeConfigSpec CLIENT_SPEC;

    private TaczConfigs() {
    }

    public static void init(File configDir) {
        init(configDir, true);
    }

    public static void init(File configDir, boolean client) {
        COMMON_SPEC = CommonConfig.init();
        SERVER_SPEC = ServerConfig.init();
        COMMON_SPEC.load(new File(configDir, "tacz-common.cfg"));
        SERVER_SPEC.load(new File(configDir, "tacz-server.cfg"));
        if (client) {
            CLIENT_SPEC = ClientConfig.init();
            CLIENT_SPEC.load(new File(configDir, "tacz-client.cfg"));
        }
    }

    public static void save() {
        if (COMMON_SPEC != null) {
            COMMON_SPEC.save();
        }
        if (SERVER_SPEC != null) {
            SERVER_SPEC.save();
        }
        if (CLIENT_SPEC != null) {
            CLIENT_SPEC.save();
        }
    }
}
