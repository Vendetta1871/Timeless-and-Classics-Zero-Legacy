package com.tacz.guns.init;

import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.advancement.TargetHitTrigger;
import com.tacz.guns.legacy.advancement.ItemUsedOnBlockTrigger;
import com.tacz.guns.legacy.advancement.LegacyItemPredicate;
import com.tacz.guns.legacy.advancement.PlayerKilledEntityTrigger;
import com.tacz.guns.legacy.advancement.RecipeCraftedTrigger;

/**
 * 1.12.2: upstream's mod-bus listeners are lifecycle calls made from {@code GunMod}:
 * <ul>
 *     <li>{@code FMLCommonSetupEvent}: {@code NetworkHandler.init()} and {@code ModSyncedEntityData.init()}, see
 *     {@code GunMod#init}, and {@link #onSetupEvent()};</li>
 *     <li>{@code FMLLoadCompleteEvent}: {@link #onLoadComplete()};</li>
 *     <li>{@code EntityAttributeModificationEvent}: {@link ModAttributes#registerAttributes};</li>
 *     <li>{@code AddPackFindersEvent}: gun pack discovery is done by {@code GunPackLoader} / {@code GunPackRepository}.</li>
 * </ul>
 */
public final class CommonRegistry {
    private static boolean LOAD_COMPLETE = false;

    /**
     * The rest of upstream's {@code onSetupEvent} ({@code FMLCommonSetupEvent}), called from {@code GunMod#init}.
     * <p>
     * Upstream also adds the gun pegboards to {@code CauldronInteraction.WATER} ({@code DYED_ITEM}); 1.12.2 has no
     * cauldron interaction registry, the washing is done by {@code GunPegboardItem#onItemUseFirst}.
     */
    public static void onSetupEvent() {
        TargetHitTrigger.register();
        GunRefitTrigger.register();
        // 1.12.2: the gun packs' advancements are loaded by GunPackAdvancements; these are the 1.20.1 vanilla
        // triggers and item predicates they need
        LegacyItemPredicate.register();
        RecipeCraftedTrigger.register();
        ItemUsedOnBlockTrigger.register();
        PlayerKilledEntityTrigger.register();
    }

    public static void onLoadComplete() {
        LOAD_COMPLETE = true;
    }

    public static boolean isLoadComplete() {
        return LOAD_COMPLETE;
    }
}
