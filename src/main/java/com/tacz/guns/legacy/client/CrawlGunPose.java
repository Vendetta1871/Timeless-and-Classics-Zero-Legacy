package com.tacz.guns.legacy.client;

import com.tacz.guns.legacy.compat.AquaAcrobaticsCompat;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 + AquaAcrobatics: while the player crawls, the first-person gun drops and rolls aside, the way the
 * swimming-pose arm carries it on 1.20.1 (upstream forces {@code Pose.SWIMMING}, and the 1.13+ first-person arm
 * swings the item with the pose; 1.12.2 has no such pose, so the arm stays put and the gun moves by itself).
 * Aiming cancels the offset, so the scope still comes up to the eye while crawling.
 */
@SideOnly(Side.CLIENT)
public final class CrawlGunPose {
    // the same smoothing family as the aiming dynamics: quick to start, no overshoot
    private static final SecondOrderDynamics CRAWL_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    // model units (1/16 block); the render space is turned 180 degrees around Z before this runs, so +X is left
    // and +Y is down on screen; the roll turns the gun around a pivot near its middle
    private static final float ROLL_DEG = 35f;
    private static final float PIVOT_Y = -4f / 16;
    private static final float DROP = 2.5f / 16;
    private static final float SHIFT_X = 1.5f / 16;
    private static final float PULL_BACK = 1f / 16;

    private CrawlGunPose() {
    }

    public static void apply(PoseStack poseStack, EntityPlayerSP player, float aimingProgress) {
        if (!AquaAcrobaticsCompat.isAvailable()) {
            return;
        }
        float progress = CRAWL_DYNAMICS.update(AquaAcrobaticsCompat.isCrawling(player) ? 1f : 0f) * (1f - aimingProgress);
        if (Math.abs(progress) < 0.001f) {
            return;
        }
        poseStack.rotateAround(Axis.ZP.rotationDegrees(ROLL_DEG * progress), 0, PIVOT_Y, 0);
        // down and to the right on screen
        poseStack.translate(-SHIFT_X * progress, DROP * progress, -PULL_BACK * progress);
    }
}
