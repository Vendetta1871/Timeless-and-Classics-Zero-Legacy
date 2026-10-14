package com.tacz.guns.legacy.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;

/**
 * Stand-ins for the 1.20.1 {@code CraftingHelper} JSON helpers that 1.12.2's {@link CraftingHelper} lacks or has
 * with a different signature (it needs a {@link JsonContext}).
 */
public final class LegacyCraftingHelper {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private LegacyCraftingHelper() {
    }

    /**
     * 1.20.1 {@code CraftingHelper.getNBT(JsonElement)}: an NBT compound from either a JSON object or an SNBT string.
     */
    public static NBTTagCompound getNBT(JsonElement element) {
        try {
            if (element.isJsonObject()) {
                return JsonToNBT.getTagFromJson(GSON.toJson(element));
            }
            return JsonToNBT.getTagFromJson(element.getAsString());
        } catch (NBTException e) {
            throw new JsonSyntaxException("Invalid NBT Entry: " + e, e);
        }
    }

    /**
     * 1.20.1 {@code CraftingHelper.getItemStack(JsonObject, boolean readNBT)}.
     */
    public static ItemStack getItemStack(JsonObject json, boolean readNBT) {
        JsonContext context = new JsonContext(GunMod.MOD_ID);
        return readNBT ? CraftingHelper.getItemStack(json, context) : CraftingHelper.getItemStackBasic(json, context);
    }

    /**
     * 1.20.1 {@code CraftingHelper.getItemStack(JsonObject, boolean readNBT, boolean disallowsAirInRecipe)}.
     */
    public static ItemStack getItemStack(JsonObject json, boolean readNBT, boolean disallowsAirInRecipe) {
        ItemStack stack = getItemStack(json, readNBT);
        if (disallowsAirInRecipe && stack.isEmpty()) {
            throw new JsonSyntaxException("Invalid item: air is not allowed here");
        }
        return stack;
    }
}
