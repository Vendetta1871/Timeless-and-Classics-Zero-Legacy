package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Optional;
import java.util.function.Function;
import com.tacz.guns.legacy.util.LegacyMath;

public class AdjustmentYRotModifier implements Function<String, Optional<AdjustmentModifier.PartModifier>> {
    private final EntityPlayer player;
    private float partialTick;

    private AdjustmentYRotModifier(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public Optional<AdjustmentModifier.PartModifier> apply(String partName) {
        if (player.getRidingEntity() != null && "body".equals(partName)) {
            return Optional.empty();
        }

        float yBodyRot = LegacyMath.rotLerp(partialTick, player.prevRenderYawOffset, player.renderYawOffset);
        float yHeadRot = LegacyMath.rotLerp(partialTick, player.prevRotationYawHead, player.rotationYawHead);
        float xRot = LegacyMath.lerp(partialTick, player.prevRotationPitch, player.rotationPitch);

        float yaw = yHeadRot - yBodyRot;
        yaw = MathHelper.wrapDegrees(yaw);
        yaw = MathHelper.clamp(yaw, -85f, 85f);

        float pitch = MathHelper.wrapDegrees(xRot);

        return switch (partName) {
            case "body" -> {
                if (AnimationManager.isPlayerLie(player)) {
                    yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0, 0, -yaw * LegacyMath.DEG_TO_RAD), Vec3f.ZERO));
                }
                yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0, -yaw * LegacyMath.DEG_TO_RAD, 0), Vec3f.ZERO));
            }
            case "head" -> Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch * LegacyMath.DEG_TO_RAD, 0, 0), Vec3f.ZERO));
            case "leftArm", "rightArm" -> {
                if (TimelessAPI.getGunDisplay(player.getHeldItemMainhand()).map(GunDisplayInstance::is3rdFixedHand).orElse(false)) {
                    yield Optional.empty();
                }
                yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch * LegacyMath.DEG_TO_RAD, 0, 0), Vec3f.ZERO));
            }
            default -> Optional.empty();
        };
    }

    public static AdjustmentModifier getModifier(EntityPlayer player) {
        AdjustmentYRotModifier adjustmentSource = new AdjustmentYRotModifier(player);
        return new AdjustmentModifier(adjustmentSource) {
            @Override
            public void setupAnim(float tickDelta) {
                adjustmentSource.partialTick = tickDelta;
                super.setupAnim(tickDelta);
            }

            // Apply the correction immediately; the outer layer owns its fade-out.
            @Override
            protected float getFadeIn(float delta) {
                return 1;
            }

            @Override
            protected float getFadeOut(float delta) {
                return 1;
            }
        };
    }
}
