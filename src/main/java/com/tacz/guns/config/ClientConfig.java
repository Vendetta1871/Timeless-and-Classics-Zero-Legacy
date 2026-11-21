package com.tacz.guns.config;

import com.tacz.guns.config.client.*;
import com.tacz.guns.legacy.config.ForgeConfigSpec;

public class ClientConfig {
    public static ForgeConfigSpec CLIENT_CONFIG_SPEC;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        KeyConfig.init(builder);
        RenderConfig.init(builder);
        PerformanceConfig.init(builder);
        ResourceConfig.init(builder);
        SoundConfig.init(builder);
        ZoomConfig.init(builder);
        CLIENT_CONFIG_SPEC = builder.build();
        return CLIENT_CONFIG_SPEC;
    }
}
