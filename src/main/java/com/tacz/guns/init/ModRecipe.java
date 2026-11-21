package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.AmmoStorageUpgradeRecipe;
import com.tacz.guns.crafting.GunPegboardDyeRecipe;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import com.tacz.guns.legacy.crafting.RecipeType;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.function.Supplier;

/**
 * 1.12.2: there are no recipe serializer / recipe type registries; both are plain singletons behind the same
 * {@code .get()} accessors as upstream's {@code RegistryObject}s.
 * <p>
 * Upstream's special crafting recipes ({@code SimpleCraftingRecipeSerializer}s, selected by the {@code type} of
 * {@code data/tacz/recipes/*.json}) are 1.12 {@link IRecipe} instances registered under the ids of those JSON files.
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ModRecipe {
    private static final GunSmithTableSerializer SERIALIZER = new GunSmithTableSerializer();
    private static final RecipeType<GunSmithTableRecipe> TYPE = new RecipeType<>(new ResourceLocation(GunMod.MOD_ID, "gun_smith_table_crafting"));

    public static Supplier<GunSmithTableSerializer> GUN_SMITH_TABLE_RECIPE_SERIALIZER = () -> SERIALIZER;
    public static Supplier<RecipeType<GunSmithTableRecipe>> GUN_SMITH_TABLE_CRAFTING = () -> TYPE;

    /**
     * Upstream {@code GUN_PEGBOARD_DYE_RECIPE_SERIALIZER} ({@code gun_pegboard_dye.json}) and
     * {@code AMMO_STORAGE_UPGRADE_RECIPE_SERIALIZER} ({@code ammo_storage_gold.json}, {@code ammo_storage_diamond.json}).
     */
    @SubscribeEvent
    public static void onRecipeRegister(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new GunPegboardDyeRecipe().setRegistryName(new ResourceLocation(GunMod.MOD_ID, "gun_pegboard_dye")));
        registerAmmoStorageUpgrade(event, "ammo_storage_gold");
        registerAmmoStorageUpgrade(event, "ammo_storage_diamond");
    }

    private static void registerAmmoStorageUpgrade(RegistryEvent.Register<IRecipe> event, String name) {
        ResourceLocation id = new ResourceLocation(GunMod.MOD_ID, name);
        event.getRegistry().register(new AmmoStorageUpgradeRecipe(id).setRegistryName(id));
    }
}
