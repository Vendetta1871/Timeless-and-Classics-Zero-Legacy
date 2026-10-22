package com.tacz.guns.config.client;

import com.tacz.guns.legacy.config.ForgeConfigSpec;

public class KeyConfig {
    public static ForgeConfigSpec.BooleanValue HOLD_TO_AIM;
    public static ForgeConfigSpec.BooleanValue HOLD_TO_CRAWL;
    public static ForgeConfigSpec.BooleanValue AUTO_RELOAD;
    // 1.12.2 only
    public static ForgeConfigSpec.BooleanValue RMB_INTERACT_WITH_OBJECTS;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("key");

        builder.comment("True if you want to hold the right mouse button to aim");
        HOLD_TO_AIM = builder.define("HoldToAim", true);

        builder.comment("True if you want to hold the crawl button to crawl");
        HOLD_TO_CRAWL = builder.define("HoldToCrawl", true);

        builder.comment("Try to reload automatically when the gun is empty");
        AUTO_RELOAD = builder.define("AutoReload", false);

        // 1.12.2 only
        builder.comment("True if the right mouse button should keep interacting with the block or entity under the crosshair (chests, furnaces, villagers, ...) while holding a gun, instead of aiming; aiming then only starts when the crosshair is not on such a target. False is the original behaviour: right mouse button always aims and interacting uses the separate interact key.");
        RMB_INTERACT_WITH_OBJECTS = builder.define("RmbInteractWithObjects", false);

        builder.pop();
    }
}
