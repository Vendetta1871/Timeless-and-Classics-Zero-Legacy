package com.tacz.guns.legacy.render;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;

/**
 * Methods of 1.20.1 {@code ItemDisplayContext} that 1.12.2 {@link ItemCameraTransforms.TransformType} lacks.
 */
public final class DisplayContext {
    private DisplayContext() {
    }

    public static boolean firstPerson(ItemCameraTransforms.TransformType transformType) {
        return transformType == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND
                || transformType == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
    }
}
