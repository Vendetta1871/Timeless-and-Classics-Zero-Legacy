package com.tacz.guns.client.animation.third;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.EnumHandSide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Restore the shared player model after a vanilla or mod-provided hand draw.
 * <p>
 * 1.12.2: {@code ModelRenderer} has no per-part scale and the model has no swim amount; the pose is the rotation
 * angles, the rotation point and the offset, {@code visible}/{@code skipDraw} are {@code showModel}/{@code isHidden}.
 */
public final class PlayerModelRenderState implements AutoCloseable {
    private final ModelPlayer model;
    private final List<PartState> parts;
    private final float attackTime;
    private final boolean crouching;
    private final boolean riding;
    private final boolean young;
    private final ModelBiped.ArmPose leftArmPose;
    private final ModelBiped.ArmPose rightArmPose;
    private final Runnable restoreAnimation;

    public PlayerModelRenderState(ModelPlayer model) {
        this.model = model;
        Map<ModelRenderer, Boolean> all = new IdentityHashMap<>();
        for (ModelRenderer part : new ModelRenderer[]{model.bipedHead, model.bipedHeadwear, model.bipedBody,
                model.bipedLeftArm, model.bipedRightArm, model.bipedLeftLeg, model.bipedRightLeg, model.bipedBodyWear,
                model.bipedLeftArmwear, model.bipedRightArmwear, model.bipedLeftLegwear, model.bipedRightLegwear}) {
            collect(part, all);
        }
        List<PartState> states = new ArrayList<>(all.size());
        for (ModelRenderer part : all.keySet()) {
            states.add(new PartState(part));
        }
        parts = Collections.unmodifiableList(states);
        attackTime = model.swingProgress;
        crouching = model.isSneak;
        riding = model.isRiding;
        young = model.isChild;
        leftArmPose = model.leftArmPose;
        rightArmPose = model.rightArmPose;
        restoreAnimation = PlayerAnimatorCompat.captureModelState(model);
    }

    /** 1.20 {@code ModelPart#getAllParts}: the part and all its children. */
    private static void collect(ModelRenderer part, Map<ModelRenderer, Boolean> out) {
        if (part == null || out.put(part, Boolean.TRUE) != null) {
            return;
        }
        if (part.childModels != null) {
            for (ModelRenderer child : part.childModels) {
                collect(child, out);
            }
        }
    }

    public void prepareArm(EnumHandSide arm, boolean visible) {
        // A RenderArmEvent replacement can draw sleeve geometry before vanilla initializes it.
        // Vanilla's fallback still applies the player's skin-part settings in renderHand().
        if (arm == EnumHandSide.RIGHT) {
            model.bipedRightArm.showModel = visible;
            model.bipedRightArmwear.showModel = visible;
        } else {
            model.bipedLeftArm.showModel = visible;
            model.bipedLeftArmwear.showModel = visible;
        }
    }

    @Override
    public void close() {
        restoreAnimation.run();
        parts.forEach(PartState::restore);
        model.swingProgress = attackTime;
        model.isSneak = crouching;
        model.isRiding = riding;
        model.isChild = young;
        model.leftArmPose = leftArmPose;
        model.rightArmPose = rightArmPose;
    }

    private static final class PartState {
        private final ModelRenderer part;
        private final float rotateAngleX, rotateAngleY, rotateAngleZ;
        private final float rotationPointX, rotationPointY, rotationPointZ;
        private final float offsetX, offsetY, offsetZ;
        private final boolean visible;
        private final boolean skipDraw;

        private PartState(ModelRenderer part) {
            this.part = part;
            this.rotateAngleX = part.rotateAngleX;
            this.rotateAngleY = part.rotateAngleY;
            this.rotateAngleZ = part.rotateAngleZ;
            this.rotationPointX = part.rotationPointX;
            this.rotationPointY = part.rotationPointY;
            this.rotationPointZ = part.rotationPointZ;
            this.offsetX = part.offsetX;
            this.offsetY = part.offsetY;
            this.offsetZ = part.offsetZ;
            this.visible = part.showModel;
            this.skipDraw = part.isHidden;
        }

        private void restore() {
            part.rotateAngleX = rotateAngleX;
            part.rotateAngleY = rotateAngleY;
            part.rotateAngleZ = rotateAngleZ;
            part.rotationPointX = rotationPointX;
            part.rotationPointY = rotationPointY;
            part.rotationPointZ = rotationPointZ;
            part.offsetX = offsetX;
            part.offsetY = offsetY;
            part.offsetZ = offsetZ;
            part.showModel = visible;
            part.isHidden = skipDraw;
        }
    }
}
