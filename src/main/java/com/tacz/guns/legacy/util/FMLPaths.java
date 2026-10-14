package com.tacz.guns.legacy.util;

import net.minecraftforge.fml.common.Loader;

import java.nio.file.Path;

/**
 * 1.20.1 {@code net.minecraftforge.fml.loading.FMLPaths} (the directories TACZ uses).
 */
public enum FMLPaths {
    GAMEDIR,
    CONFIGDIR;

    public Path get() {
        Path configDir = Loader.instance().getConfigDir().toPath();
        return this == CONFIGDIR ? configDir : configDir.getParent();
    }
}
