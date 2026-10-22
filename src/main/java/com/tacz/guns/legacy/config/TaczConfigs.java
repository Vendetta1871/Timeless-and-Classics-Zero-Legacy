package com.tacz.guns.legacy.config;

import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.ServerConfig;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

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
            dropMovedScopeSettings();
            dropRemovedKeys();
        }
    }

    /**
     * The TACZ crawl key was dropped in favor of AquaAcrobatics' own crawl keybind; remove the stale
     * {@code HoldToCrawl} entry so it does not show up in the config GUI.
     */
    private static void dropRemovedKeys() {
        Configuration configuration = CLIENT_SPEC.getConfiguration();
        if (configuration == null || !configuration.hasCategory("key")) {
            return;
        }
        ConfigCategory key = configuration.getCategory("key");
        if (key.containsKey("HoldToCrawl")) {
            key.remove("HoldToCrawl");
            CLIENT_SPEC.save();
        }
    }

    /**
     * The 1.12-only scope settings moved from the client config's {@code render} category to {@code performance};
     * drop the stale entries so they do not show up in the config GUI.
     */
    private static void dropMovedScopeSettings() {
        Configuration configuration = CLIENT_SPEC.getConfiguration();
        if (configuration == null || !configuration.hasCategory("render")) {
            return;
        }
        ConfigCategory render = configuration.getCategory("render");
        boolean changed = false;
        for (String key : new String[]{"ScopePictureInPicture", "ScopeViewResolution", "ScopeFisheye", "ScopeMirror", "ScopeMirrorFov"}) {
            if (render.containsKey(key)) {
                render.remove(key);
                changed = true;
            }
        }
        if (changed) {
            CLIENT_SPEC.save();
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

    /**
     * Re-reads every value from the loaded files' properties (after a config GUI edited them);
     * {@link #save()} writes them back.
     */
    public static void reload() {
        if (COMMON_SPEC != null) {
            COMMON_SPEC.reload();
        }
        if (SERVER_SPEC != null) {
            SERVER_SPEC.reload();
        }
        if (CLIENT_SPEC != null) {
            CLIENT_SPEC.reload();
        }
    }
}
