package com.tacz.guns.compat.optifine;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class OptifineCompat {
    /**
     * Upstream probes {@code net.optifine.Config}; on 1.12.2 Forge reports OptiFine itself.
     */
    private static final boolean IS_OPTIFINE_INSTALLED = FMLCommonHandler.instance().getSide() == Side.CLIENT
            && net.minecraftforge.fml.client.FMLClientHandler.instance().hasOptifine();

    public static boolean isOptifineInstalled() {
        return IS_OPTIFINE_INSTALLED;
    }

    public static boolean isClassFound(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
