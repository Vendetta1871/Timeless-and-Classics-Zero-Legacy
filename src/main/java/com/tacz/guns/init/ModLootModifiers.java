package com.tacz.guns.init;

/**
 * 1.12.2 has no global loot modifiers (no {@code IGlobalLootModifier} codec registry): the loot table injections are
 * applied by {@link com.tacz.guns.loot.LootTableInjectorModifier} from {@code LootTableLoadEvent}.
 */
public class ModLootModifiers {
    // TODO(port): no global loot modifier serializer registry on 1.12.2 (LootTableInjectorModifier hooks LootTableLoadEvent instead)
//    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
//            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, GunMod.MOD_ID);
//
//    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> LOOT_TABLE_INJECTOR =
//            LOOT_MODIFIER_SERIALIZERS.register("loot_table_injector", () -> LootTableInjectorModifier.CODEC);
}
