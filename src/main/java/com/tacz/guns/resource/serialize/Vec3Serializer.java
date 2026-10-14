package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import com.tacz.guns.legacy.util.GsonHelper;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Type;

public class Vec3Serializer implements JsonDeserializer<Vec3d>, JsonSerializer<Vec3d> {
    @Override
    public Vec3d deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            JsonElement xElement = array.get(0);
            JsonElement yElement = array.get(1);
            JsonElement zElement = array.get(2);
            double x = GsonHelper.convertToDouble(xElement, "(array i=0)");
            double y = GsonHelper.convertToDouble(yElement, "(array i=1)");
            double z = GsonHelper.convertToDouble(zElement, "(array i=2)");
            return new Vec3d(x, y, z);
        } else {
            throw new JsonSyntaxException("Expected " + json + " to be a Vec3d because it's not an array");
        }
    }

    @Override
    public JsonElement serialize(Vec3d src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(src.x));
        array.add(new JsonPrimitive(src.y));
        array.add(new JsonPrimitive(src.z));
        return array;
    }
}
