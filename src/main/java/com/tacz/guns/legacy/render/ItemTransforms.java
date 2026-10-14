package com.tacz.guns.legacy.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;
import org.lwjgl.util.vector.Vector3f;

import java.lang.reflect.Type;

/**
 * 1.20.1 {@code ItemTransforms} / {@code ItemTransform} helpers for their 1.12.2 counterparts
 * ({@link ItemCameraTransforms} / {@link ItemTransformVec3f}), whose JSON deserializers are package-private.
 */
@SideOnly(Side.CLIENT)
public final class ItemTransforms {
    private ItemTransforms() {
    }

    /**
     * {@code ItemTransform#apply(boolean leftHand, PoseStack)}.
     */
    public static void apply(ItemTransformVec3f transform, boolean leftHand, PoseStack poseStack) {
        if (transform == ItemTransformVec3f.DEFAULT) {
            return;
        }
        float x = transform.rotation.x;
        float y = transform.rotation.y;
        float z = transform.rotation.z;
        if (leftHand) {
            y = -y;
            z = -z;
        }
        int i = leftHand ? -1 : 1;
        poseStack.translate((float) i * transform.translation.x, transform.translation.y, transform.translation.z);
        poseStack.mulPose(new Quaternionf().rotationXYZ(x * ((float) Math.PI / 180F), y * ((float) Math.PI / 180F), z * ((float) Math.PI / 180F)));
        poseStack.scale(transform.scale.x, transform.scale.y, transform.scale.z);
    }

    /**
     * Same as the vanilla {@code ItemCameraTransforms.Deserializer}.
     */
    public static class Deserializer implements JsonDeserializer<ItemCameraTransforms> {
        @Override
        public ItemCameraTransforms deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            ItemTransformVec3f thirdRight = get(context, object, "thirdperson_righthand");
            ItemTransformVec3f thirdLeft = get(context, object, "thirdperson_lefthand");
            if (thirdLeft == ItemTransformVec3f.DEFAULT) {
                thirdLeft = thirdRight;
            }
            ItemTransformVec3f firstRight = get(context, object, "firstperson_righthand");
            ItemTransformVec3f firstLeft = get(context, object, "firstperson_lefthand");
            if (firstLeft == ItemTransformVec3f.DEFAULT) {
                firstLeft = firstRight;
            }
            return new ItemCameraTransforms(thirdLeft, thirdRight, firstLeft, firstRight, get(context, object, "head"),
                    get(context, object, "gui"), get(context, object, "ground"), get(context, object, "fixed"));
        }

        private static ItemTransformVec3f get(JsonDeserializationContext context, JsonObject object, String key) {
            return object.has(key) ? context.deserialize(object.get(key), ItemTransformVec3f.class) : ItemTransformVec3f.DEFAULT;
        }
    }

    /**
     * Same as the vanilla {@code ItemTransformVec3f.Deserializer}.
     */
    public static class TransformDeserializer implements JsonDeserializer<ItemTransformVec3f> {
        @Override
        public ItemTransformVec3f deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            Vector3f rotation = vector(object, "rotation", 0);
            Vector3f translation = vector(object, "translation", 0);
            translation.scale(0.0625F);
            translation.x = MathHelper.clamp(translation.x, -5.0F, 5.0F);
            translation.y = MathHelper.clamp(translation.y, -5.0F, 5.0F);
            translation.z = MathHelper.clamp(translation.z, -5.0F, 5.0F);
            Vector3f scale = vector(object, "scale", 1);
            scale.x = MathHelper.clamp(scale.x, -4.0F, 4.0F);
            scale.y = MathHelper.clamp(scale.y, -4.0F, 4.0F);
            scale.z = MathHelper.clamp(scale.z, -4.0F, 4.0F);
            return new ItemTransformVec3f(rotation, translation, scale);
        }

        private static Vector3f vector(JsonObject object, String key, float fallback) {
            if (!object.has(key)) {
                return new Vector3f(fallback, fallback, fallback);
            }
            JsonArray array = JsonUtils.getJsonArray(object, key);
            if (array.size() != 3) {
                throw new JsonParseException("Expected 3 " + key + " values, found: " + array.size());
            }
            return new Vector3f(JsonUtils.getFloat(array.get(0), key + "[0]"), JsonUtils.getFloat(array.get(1), key + "[1]"),
                    JsonUtils.getFloat(array.get(2), key + "[2]"));
        }
    }
}
