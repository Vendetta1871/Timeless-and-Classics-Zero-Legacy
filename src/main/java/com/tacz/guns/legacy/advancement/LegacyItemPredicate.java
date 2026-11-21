package com.tacz.guns.legacy.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.util.LegacyIngredients;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.NBTPredicate;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.advancements.critereon.ItemPredicates;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A 1.20.1 item predicate: {@code items} (a list of item ids), {@code tag}, {@code count}, {@code durability} and
 * {@code nbt}. 1.12.2's own item predicate takes a single {@code item}; gun pack advancements select this one with
 * {@code "type": "tacz:items"} (added by {@link GunPackAdvancements}), the custom triggers parse it directly.
 */
public final class LegacyItemPredicate extends ItemPredicate {
    public static final ResourceLocation TYPE = new ResourceLocation(GunMod.MOD_ID, "items");

    /**
     * {@code null}: any item.
     */
    @Nullable
    private final List<Ingredient> items;
    private final MinMaxBounds count;
    private final MinMaxBounds durability;
    private final NBTPredicate nbt;

    private LegacyItemPredicate(@Nullable List<Ingredient> items, MinMaxBounds count, MinMaxBounds durability, NBTPredicate nbt) {
        this.items = items;
        this.count = count;
        this.durability = durability;
        this.nbt = nbt;
    }

    public static void register() {
        ItemPredicates.register(TYPE, LegacyItemPredicate::fromJson);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (items != null) {
            boolean found = false;
            for (Ingredient ingredient : items) {
                if (ingredient.apply(stack)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        if (!count.test(stack.getCount())) {
            return false;
        }
        if (durability != MinMaxBounds.UNBOUNDED && (!stack.isItemStackDamageable()
                || !durability.test(stack.getMaxDamage() - stack.getItemDamage()))) {
            return false;
        }
        return nbt.test(stack);
    }

    /**
     * Any item for a missing or null element, as 1.20.1's {@code ItemPredicate.fromJson}.
     */
    public static ItemPredicate fromJson(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return ItemPredicate.ANY;
        }
        return fromJson(JsonUtils.getJsonObject(element, "item"));
    }

    public static ItemPredicate fromJson(JsonObject json) {
        List<Ingredient> items = null;
        if (json.has("items")) {
            items = new ArrayList<>();
            JsonArray array = JsonUtils.getJsonArray(json, "items");
            for (JsonElement id : array) {
                items.add(item(JsonUtils.getString(id, "item")));
            }
        }
        if (json.has("item")) {
            // the 1.12.2 / 1.19 single item form
            items = items == null ? new ArrayList<>() : items;
            items.add(item(JsonUtils.getString(json, "item")));
        }
        if (json.has("tag")) {
            Ingredient tag = LegacyIngredients.fromTag(JsonUtils.getString(json, "tag"));
            if (tag.getMatchingStacks().length == 0) {
                throw new JsonSyntaxException("Unknown item tag '" + JsonUtils.getString(json, "tag") + "'");
            }
            items = items == null ? new ArrayList<>() : items;
            items.add(tag);
        }
        return new LegacyItemPredicate(items, MinMaxBounds.deserialize(json.get("count")),
                MinMaxBounds.deserialize(json.get("durability")), NBTPredicate.deserialize(json.get("nbt")));
    }

    private static Ingredient item(String id) {
        ItemStack stack = LegacyIngredients.flattenItem(id);
        if (stack == null) {
            throw new JsonSyntaxException("Unknown item id '" + id + "'");
        }
        return Ingredient.fromStacks(stack);
    }
}
