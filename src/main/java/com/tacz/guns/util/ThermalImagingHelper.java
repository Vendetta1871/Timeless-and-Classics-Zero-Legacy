package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.tags.TagKey;
import com.tacz.guns.legacy.tags.TagType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/** Entity-type rules shared by thermal imaging; tag membership is read live after data-pack reloads. */
public final class ThermalImagingHelper {
    // 1.12.2: entity tags match entities by their entity id (legacy.tags); players have no entity id and never match
    public static final TagKey<Entity> BLACKLIST = TagKey.create(TagType.ENTITY_TYPE,
            new ResourceLocation(GunMod.MOD_ID, "thermal_imaging/blacklist"));
    public static final TagKey<Entity> WHITELIST = TagKey.create(TagType.ENTITY_TYPE,
            new ResourceLocation(GunMod.MOD_ID, "thermal_imaging/whitelist"));

    private ThermalImagingHelper() {
    }

    /** A blacklist veto takes precedence, including when a type also appears in the whitelist. */
    public static boolean isHeatSource(Entity entity) {
        return !BLACKLIST.contains(entity) && (entity instanceof EntityLivingBase || WHITELIST.contains(entity));
    }
}
