package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.legacy.util.GsonHelper;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;

import java.lang.reflect.Type;

public class GunSmithTableIngredientSerializer implements JsonDeserializer<GunSmithTableIngredient> {
    @Override
    public GunSmithTableIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("item")) {
                throw new JsonSyntaxException("Expected " + jsonObject + " must has a item member");
            }
            Ingredient ingredient = CraftingHelper.getIngredient(jsonObject.get("item"), new JsonContext(GunMod.MOD_ID));
            int count = 1;
            if (jsonObject.has("count")) {
                count = Math.max(GsonHelper.getAsInt(jsonObject, "count"), 1);
            }
            return new GunSmithTableIngredient(ingredient, count);
        } else {
            throw new JsonSyntaxException("Expected " + json + " to be a Pair because it's not an object");
        }
    }
}
