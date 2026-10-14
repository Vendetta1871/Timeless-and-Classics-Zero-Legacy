package com.tacz.guns.client.resource.pojo.display.block;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.ResourceLocation;

public class BlockDisplay implements IDisplay {
    @SerializedName("model")
    private ResourceLocation modelLocation;
    @SerializedName("texture")
    private ResourceLocation modelTexture;
    @SerializedName("transforms")
    private ItemCameraTransforms transforms;

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    public ItemCameraTransforms getTransforms() {
        return transforms;
    }

    @Override
    public void init() {
        if (modelTexture != null) {
            modelTexture = converter.idToFile(modelTexture);
        }
    }
}
