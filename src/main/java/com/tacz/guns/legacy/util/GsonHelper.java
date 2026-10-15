package com.tacz.guns.legacy.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.JsonUtils;

import javax.annotation.Nullable;
import java.io.Reader;

/**
 * 1.20.1 {@code net.minecraft.util.GsonHelper} (only the methods TACZ uses), implemented on top of 1.12.2's
 * {@link JsonUtils}. Same exceptions ({@link JsonSyntaxException}) as upstream.
 */
public final class GsonHelper {
    private GsonHelper() {
    }

    public static boolean isStringValue(JsonObject json, String memberName) {
        return JsonUtils.isString(json, memberName);
    }

    // 1.12.2: the JsonElement overloads of JsonUtils.isString/isNumber are client-only
    public static boolean isStringValue(JsonElement json) {
        return json.isJsonPrimitive() && ((JsonPrimitive) json).isString();
    }

    public static boolean isNumberValue(JsonElement json) {
        return json.isJsonPrimitive() && ((JsonPrimitive) json).isNumber();
    }

    public static boolean isBooleanValue(JsonObject json, String memberName) {
        // 1.12.2: JsonUtils.isBoolean is client-only
        return json.has(memberName) && isBooleanValue(json.get(memberName));
    }

    public static boolean isBooleanValue(JsonElement json) {
        return json.isJsonPrimitive() && ((JsonPrimitive) json).isBoolean();
    }

    public static boolean isArrayNode(JsonObject json, String memberName) {
        return JsonUtils.isJsonArray(json, memberName);
    }

    public static boolean isValidPrimitive(JsonObject json, String memberName) {
        return JsonUtils.isJsonPrimitive(json, memberName);
    }

    public static boolean isValidNode(JsonObject json, String memberName) {
        return JsonUtils.hasField(json, memberName);
    }

    public static String convertToString(JsonElement json, String memberName) {
        return JsonUtils.getString(json, memberName);
    }

    public static String getAsString(JsonObject json, String memberName) {
        return JsonUtils.getString(json, memberName);
    }

    public static String getAsString(JsonObject json, String memberName, String fallback) {
        return JsonUtils.getString(json, memberName, fallback);
    }

    public static boolean convertToBoolean(JsonElement json, String memberName) {
        return JsonUtils.getBoolean(json, memberName);
    }

    public static boolean getAsBoolean(JsonObject json, String memberName) {
        return JsonUtils.getBoolean(json, memberName);
    }

    public static boolean getAsBoolean(JsonObject json, String memberName, boolean fallback) {
        return JsonUtils.getBoolean(json, memberName, fallback);
    }

    public static double convertToDouble(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && ((JsonPrimitive) json).isNumber()) {
            return json.getAsDouble();
        }
        throw new JsonSyntaxException("Expected " + memberName + " to be a Double, was " + JsonUtils.toString(json));
    }

    public static double getAsDouble(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToDouble(json.get(memberName), memberName);
        }
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Double");
    }

    public static double getAsDouble(JsonObject json, String memberName, double fallback) {
        return json.has(memberName) ? convertToDouble(json.get(memberName), memberName) : fallback;
    }

    public static float convertToFloat(JsonElement json, String memberName) {
        return JsonUtils.getFloat(json, memberName);
    }

    public static float getAsFloat(JsonObject json, String memberName) {
        return JsonUtils.getFloat(json, memberName);
    }

    public static float getAsFloat(JsonObject json, String memberName, float fallback) {
        return JsonUtils.getFloat(json, memberName, fallback);
    }

    public static int convertToInt(JsonElement json, String memberName) {
        return JsonUtils.getInt(json, memberName);
    }

    public static int getAsInt(JsonObject json, String memberName) {
        return JsonUtils.getInt(json, memberName);
    }

    public static int getAsInt(JsonObject json, String memberName, int fallback) {
        return JsonUtils.getInt(json, memberName, fallback);
    }

    public static JsonObject convertToJsonObject(JsonElement json, String memberName) {
        return JsonUtils.getJsonObject(json, memberName);
    }

    public static JsonObject getAsJsonObject(JsonObject json, String memberName) {
        return JsonUtils.getJsonObject(json, memberName);
    }

    public static JsonObject getAsJsonObject(JsonObject json, String memberName, JsonObject fallback) {
        return JsonUtils.getJsonObject(json, memberName, fallback);
    }

    public static JsonArray convertToJsonArray(JsonElement json, String memberName) {
        return JsonUtils.getJsonArray(json, memberName);
    }

    public static JsonArray getAsJsonArray(JsonObject json, String memberName) {
        return JsonUtils.getJsonArray(json, memberName);
    }

    @Nullable
    public static JsonArray getAsJsonArray(JsonObject json, String memberName, @Nullable JsonArray fallback) {
        return JsonUtils.getJsonArray(json, memberName, fallback);
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> type, boolean lenient) {
        return JsonUtils.gsonDeserialize(gson, reader, type, lenient);
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> type) {
        return JsonUtils.fromJson(gson, reader, type);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> type) {
        return JsonUtils.gsonDeserialize(gson, json, type);
    }
}
