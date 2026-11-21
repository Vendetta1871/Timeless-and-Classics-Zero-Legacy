package com.tacz.guns.init;

import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.advancement.TargetHitTrigger;

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
        // TODO(port): the triggers work, but nothing loads the gun packs' advancements (data/<namespace>/advancements,
        //  e.g. tacz_default_gun's target_hit.json): 1.12.2 only reads assets/<modid>/advancements from mod jars, and
        //  most default pack criteria use 1.20.1 triggers or condition formats (recipe_crafted, item_used_on_block
        //  location predicates, inventory_changed item lists) that 1.12.2 cannot parse
        TargetHitTrigger.register();
        GunRefitTrigger.register();
    }

    public static void onLoadComplete() {
        LOAD_COMPLETE = true;
    }

    public static boolean isLoadComplete() {
        return LOAD_COMPLETE;
    }
}
