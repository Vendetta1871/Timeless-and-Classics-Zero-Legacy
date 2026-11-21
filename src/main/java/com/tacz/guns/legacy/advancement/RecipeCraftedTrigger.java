package com.tacz.guns.legacy.advancement;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.util.SimpleCriterionTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1.20.1 {@code minecraft:recipe_crafted} (1.12.2 has none): a crafting table recipe was crafted, optionally from
 * the given ingredients (each predicate matching a different input stack). Fired from {@link AdvancementEvents}.
 */
public final class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
    public static final RecipeCraftedTrigger INSTANCE = new RecipeCraftedTrigger();
    static final ResourceLocation ID = new ResourceLocation(GunMod.MOD_ID, "recipe_crafted");
    /**
     * 1.20.1 recipe ids whose 1.12.2 recipe has another name.
     */
    private static final Map<String, String> RECIPE_IDS = ImmutableMap.of("minecraft:armor_dye", "minecraft:armordye");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(EntityPlayerMP player, ResourceLocation recipeId, List<ItemStack> ingredients) {
        this.trigger(player, instance -> instance.matches(recipeId, ingredients));
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, JsonDeserializationContext context) {
        String id = JsonUtils.getString(json, "recipe_id");
        ResourceLocation recipeId = new ResourceLocation(RECIPE_IDS.getOrDefault(new ResourceLocation(id).toString(), id));
        List<ItemPredicate> ingredients = new ArrayList<>();
        if (json.has("ingredients")) {
            for (JsonElement element : JsonUtils.getJsonArray(json, "ingredients")) {
                ingredients.add(LegacyItemPredicate.fromJson(element));
            }
        }
        return new TriggerInstance(recipeId, ingredients);
    }

    public static class TriggerInstance extends AbstractCriterionInstance {
        private final ResourceLocation recipeId;
        private final List<ItemPredicate> predicates;

        public TriggerInstance(ResourceLocation recipeId, List<ItemPredicate> predicates) {
            super(ID);
            this.recipeId = recipeId;
            this.predicates = predicates;
        }

        public boolean matches(ResourceLocation recipeId, List<ItemStack> items) {
            if (!this.recipeId.equals(recipeId)) {
                return false;
            }
            List<ItemStack> left = new ArrayList<>(items);
            for (ItemPredicate predicate : predicates) {
                boolean found = false;
                for (int i = 0; i < left.size(); i++) {
                    if (predicate.test(left.get(i))) {
                        left.remove(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
    }
}
