package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import com.tacz.guns.legacy.crafting.RecipeType;
import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

/**
 * 1.12.2: there are no recipe serializer / recipe type registries; both are plain singletons behind the same
 * {@code .get()} accessors as upstream's {@code RegistryObject}s.
 */
public class ModRecipe {
    private static final GunSmithTableSerializer SERIALIZER = new GunSmithTableSerializer();
    private static final RecipeType<GunSmithTableRecipe> TYPE = new RecipeType<>(new ResourceLocation(GunMod.MOD_ID, "gun_smith_table_crafting"));

    public static Supplier<GunSmithTableSerializer> GUN_SMITH_TABLE_RECIPE_SERIALIZER = () -> SERIALIZER;
    public static Supplier<RecipeType<GunSmithTableRecipe>> GUN_SMITH_TABLE_CRAFTING = () -> TYPE;
}
