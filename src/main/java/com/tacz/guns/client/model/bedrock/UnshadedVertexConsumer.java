package com.tacz.guns.client.model.bedrock;

import com.tacz.guns.legacy.render.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class UnshadedVertexConsumer implements VertexConsumer {
    private static final float FALLBACK_NORMAL_X = 0.0F;
    private static final float FALLBACK_NORMAL_Y = 1.0F;
    private static final float FALLBACK_NORMAL_Z = 0.0F;

    private final VertexConsumer delegate;
    private final float normalX;
    private final float normalY;
    private final float normalZ;

    private UnshadedVertexConsumer(VertexConsumer delegate, float normalX, float normalY, float normalZ) {
        this.delegate = delegate;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }

    /**
     * 1.12.2: the fixed-function pipeline lights with GL_LIGHT0/1, not with shader light directions, so there is no
     * normal that maps to full brightness for every pose; illuminated parts keep their normals and only get the
     * full-bright lightmap value, as before.
     * TODO(port): unshaded illuminated cubes (needs the GL light directions in the pose's space)
     */
    public static VertexConsumer wrap(VertexConsumer consumer) {
        return consumer;
    }

    public static VertexConsumer wrapFixedNormal(VertexConsumer consumer) {
        return new UnshadedVertexConsumer(consumer,
                FALLBACK_NORMAL_X, FALLBACK_NORMAL_Y, FALLBACK_NORMAL_Z);
    }

    public VertexConsumer getDelegate() {
        return delegate;
    }

    public Matrix3f createNormalMatrix() {
        // Accelerated meshes store +Y as their canonical unshaded normal. The second matrix column
        // maps that normal to the shader light direction captured for this render call.
        return new Matrix3f()
                .m10(normalX)
                .m11(normalY)
                .m12(normalZ);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(normalX, normalY, normalZ);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }


}
