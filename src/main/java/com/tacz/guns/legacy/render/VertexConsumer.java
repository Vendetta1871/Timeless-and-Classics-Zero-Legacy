package com.tacz.guns.legacy.render;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 1.20.1 {@code com.mojang.blaze3d.vertex.VertexConsumer}. Element calls may come in any order; a vertex is
 * committed by {@link #endVertex()}.
 */
@SideOnly(Side.CLIENT)
public interface VertexConsumer {
    VertexConsumer vertex(double x, double y, double z);

    VertexConsumer color(int red, int green, int blue, int alpha);

    VertexConsumer uv(float u, float v);

    VertexConsumer overlayCoords(int u, int v);

    VertexConsumer uv2(int u, int v);

    VertexConsumer normal(float x, float y, float z);

    void endVertex();

    default void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                        float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ) {
        this.vertex(x, y, z);
        this.color(red, green, blue, alpha);
        this.uv(texU, texV);
        this.overlayCoords(overlayUV);
        this.uv2(lightmapUV);
        this.normal(normalX, normalY, normalZ);
        this.endVertex();
    }

    default VertexConsumer color(float red, float green, float blue, float alpha) {
        return this.color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
    }

    default VertexConsumer color(int argb) {
        return this.color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    default VertexConsumer uv2(int lightmapUV) {
        return this.uv2(lightmapUV & 0xFFFF, lightmapUV >> 16 & 0xFFFF);
    }

    default VertexConsumer overlayCoords(int overlayUV) {
        return this.overlayCoords(overlayUV & 0xFFFF, overlayUV >> 16 & 0xFFFF);
    }

    default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        Vector4f vector = matrix.transform(new Vector4f(x, y, z, 1.0F));
        return this.vertex(vector.x(), vector.y(), vector.z());
    }

    default VertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
        Vector3f vector = matrix.transform(new Vector3f(x, y, z));
        return this.normal(vector.x(), vector.y(), vector.z());
    }
}
