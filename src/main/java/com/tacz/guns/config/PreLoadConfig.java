package com.tacz.guns.config;

import com.tacz.guns.legacy.config.ForgeConfigSpec;

import java.nio.file.Path;

public class PreLoadConfig {
    private static ForgeConfigSpec spec;
    public static ForgeConfigSpec.BooleanValue override;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gunpack");
        builder.comment("When enabled, the mod will not try to overwrite the default pack under .minecraft/tacz\n" +
                "Since 1.0.4, the overwriting will only run when you start client or a dedicated server");
        override = builder.define("DefaultPackDebug", false);
        builder.pop();
        spec = builder.build();
    }

    // TODO(port): upstream goes through PreLoadModConfig (Forge ModConfig + night-config handler) and fires a config event.
    //  1.12.2 has no ModConfig, so the spec is loaded from the file directly (tacz-pre.cfg instead of tacz-pre.toml).
    public static void load(Path configBasePath) {
        if (spec.isLoaded()) return;
        spec.load(configBasePath.resolve("tacz-pre.cfg").toFile());
    }

    /**
     * 1.12.2 addition: writes the current values (e.g. after {@code override.set(..)}) back to tacz-pre.cfg.
     */
    public static void save() {
        spec.save();
    }
}
