package com.tacz.guns.config;

import com.tacz.guns.legacy.config.ForgeConfigSpec;

import java.nio.file.Path;

public class PreLoadConfig {
    private static ForgeConfigSpec spec;
    public static ForgeConfigSpec.BooleanValue override;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gunpack");
        builder.comment("Enable to preserve local default gun pack edits.",
                "When disabled, changes to the TACZ version or bundled resources trigger replacement at startup.",
                "To regenerate, disable this option, delete <game directory>/tacz/tacz_default_gun, then restart.");
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
     * Writes the current values (e.g. after {@code override.set(..)}) back to tacz-pre.cfg.
     */
    public static void save() {
        spec.save();
    }
}
