package com.tacz.guns.init;

/**
 * 1.12.2: upstream's mod-bus listeners are lifecycle calls made from {@code GunMod}:
 * <ul>
 *     <li>{@code FMLCommonSetupEvent}: {@code NetworkHandler.init()} and {@code ModSyncedEntityData.init()}, see {@code GunMod#init};</li>
 *     <li>{@code FMLLoadCompleteEvent}: {@link #onLoadComplete()};</li>
 *     <li>{@code EntityAttributeModificationEvent}: {@link ModAttributes#registerAttributes};</li>
 *     <li>{@code AddPackFindersEvent}: gun pack discovery is done by {@code GunPackLoader} / {@code GunPackRepository}.</li>
 * </ul>
 */
public final class CommonRegistry {
    private static boolean LOAD_COMPLETE = false;

    public static void onLoadComplete() {
        LOAD_COMPLETE = true;
    }

    public static boolean isLoadComplete() {
        return LOAD_COMPLETE;
    }
}
