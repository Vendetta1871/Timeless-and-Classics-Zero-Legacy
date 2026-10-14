package com.tacz.guns.legacy.render;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 1.20.1 {@code com.mojang.blaze3d.vertex.PoseStack} on top of JOML. Vertices are transformed on the CPU,
 * on top of whatever GL modelview matrix is current when the batch is drawn.
 */
@SideOnly(Side.CLIENT)
public class PoseStack {
    private final Deque<Pose> poseStack = new ArrayDeque<>();

    public PoseStack() {
        this.poseStack.add(new Pose(new Matrix4f(), new Matrix3f()));
    }

    public void translate(double x, double y, double z) {
        this.translate((float) x, (float) y, (float) z);
    }

    public void translate(float x, float y, float z) {
        this.poseStack.getLast().pose.translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        Pose pose = this.poseStack.getLast();
        pose.pose.scale(x, y, z);
        if (x == y && y == z) {
            if (x > 0.0F) {
                return;
            }
            pose.normal.scale(-1.0F);
        }
        float invX = 1.0F / x;
        float invY = 1.0F / y;
        float invZ = 1.0F / z;
        float invCbrt = (float) (1.0 / Math.cbrt(invX * invY * invZ));
        pose.normal.scale(invCbrt * invX, invCbrt * invY, invCbrt * invZ);
    }

    public void mulPose(Quaternionf quaternion) {
        Pose pose = this.poseStack.getLast();
        pose.pose.rotate(quaternion);
        pose.normal.rotate(quaternion);
    }

    public void rotateAround(Quaternionf quaternion, float x, float y, float z) {
        Pose pose = this.poseStack.getLast();
        pose.pose.rotateAround(quaternion, x, y, z);
        pose.normal.rotate(quaternion);
    }

    public void pushPose() {
        Pose pose = this.poseStack.getLast();
        this.poseStack.addLast(new Pose(new Matrix4f(pose.pose), new Matrix3f(pose.normal)));
    }

    public void popPose() {
        this.poseStack.removeLast();
    }

    public Pose last() {
        return this.poseStack.getLast();
    }

    public boolean clear() {
        return this.poseStack.size() == 1;
    }

    public void setIdentity() {
        Pose pose = this.poseStack.getLast();
        pose.pose.identity();
        pose.normal.identity();
    }

    public void mulPoseMatrix(Matrix4f matrix) {
        this.poseStack.getLast().pose.mul(matrix);
    }

    @SideOnly(Side.CLIENT)
    public static final class Pose {
        final Matrix4f pose;
        final Matrix3f normal;

        Pose(Matrix4f pose, Matrix3f normal) {
            this.pose = pose;
            this.normal = normal;
        }

        public Matrix4f pose() {
            return this.pose;
        }

        public Matrix3f normal() {
            return this.normal;
        }
    }
}
