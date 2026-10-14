package com.tacz.guns.init;

import net.minecraftforge.fml.common.Loader;

/**
 * 1.12.2: {@code InterModEnqueueEvent} becomes the {@code postInit} lifecycle call made from {@code GunMod}.
 */
public class CompatRegistry {
    // TODO(port): Cloth Config has no 1.12.2 version (compat/cloth, client/gui/compat/ClothConfigScreen are not ported)
//    public static final String CLOTH_CONFIG = "cloth_config";
    // TODO(port): needs OculusCompat (Oculus has no 1.12.2 version; compat is not ported)
//    public static final String OCULUS = "oculus";
    // TODO(port): Carry On has no compatible 1.12.2 version (compat/carryon is not ported)
//    public static final String CARRY_ON_ID = "carryon";

    public static void onEnqueue() {
        // TODO(port): compat/cloth/MenuIntegration, client/gui/compat/ClothConfigScreen, compat/oculus/OculusCompat and
        //  compat/carryon/BlackList are not ported
//        checkModLoad(CLOTH_CONFIG, () -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MenuIntegration::registerModsPage));
//        if (FMLEnvironment.dist == Dist.CLIENT) {
//            ClothConfigScreen.registerNoClothConfigPage();
//        }
//        checkModLoad(OCULUS, OculusCompat::initCompat);
//        checkModLoad(CARRY_ON_ID, BlackList::addBlackList);
    }

    public static void checkModLoad(String modId, Runnable runnable) {
        if (Loader.isModLoaded(modId)) {
            runnable.run();
        }
    }
}
