package com.tacz.guns.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.loot.LootTableInjection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * 1.12.2 has no global loot modifiers. Upstream's {@code LootModifier} adds the injected stacks to the generated loot of
 * every loot table; here {@code LootTableLoadEvent} adds a pool with a single entry to every loot table, which generates
 * the injected stacks when the table is rolled (so injections that are reloaded later still apply).
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class LootTableInjectorModifier {
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        LootTable table = event.getTable();
        if (table == null || table == LootTable.EMPTY_LOOT_TABLE || table.isFrozen()) {
            return;
        }
        LootCondition[] noConditions = new LootCondition[0];
        table.addPool(new LootPool(new LootEntry[]{new InjectionEntry(event.getName(), noConditions)}, noConditions,
                new RandomValueRange(1), new RandomValueRange(0), "tacz_loot_table_injector"));
    }

    protected static void doApply(ResourceLocation lootTableId, Collection<ItemStack> generatedLoot, LootContext context) {
        CommonAssetsManager manager = CommonAssetsManager.getInstance();
        if (manager == null) {
            return;
        }

        List<LootTableInjection> injections = manager.getLootTableInjections(lootTableId);
        if (injections.isEmpty()) {
            return;
        }

        for (LootTableInjection injection : injections) {
            for (ItemStack stack : injection.createStacks(context)) {
                generatedLoot.add(stack);
            }
        }
    }

    private static class InjectionEntry extends LootEntry {
        private final ResourceLocation lootTableId;

        private InjectionEntry(ResourceLocation lootTableId, LootCondition[] conditions) {
            super(1, 0, conditions, "tacz_loot_table_injector");
            this.lootTableId = lootTableId;
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
            doApply(lootTableId, stacks, context);
        }

        @Override
        protected void serialize(JsonObject json, JsonSerializationContext context) {
            // never written to a file
        }
    }
}
