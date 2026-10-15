package com.tacz.guns.legacy.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.20.1 {@code MultiBufferSource}. {@link BufferSource} collects vertices per {@link RenderType} and draws them
 * with the 1.12.2 {@link Tessellator} on {@link BufferSource#endBatch()}.
 */
@SideOnly(Side.CLIENT)
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType renderType);

    static BufferSource immediate() {
        return new BufferSource();
    }

    @SideOnly(Side.CLIENT)
    class BufferSource implements MultiBufferSource {
        private final Map<RenderType, CollectingConsumer> buffers = new LinkedHashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return buffers.computeIfAbsent(renderType, CollectingConsumer::new);
        }

        /**
         * Draws opaque and cutout batches first, then translucent ones, in first-use order.
         */
        public void endBatch() {
            // consumers (and their vertex arrays) stay in the map and are reused by later frames
            for (CollectingConsumer consumer : buffers.values()) {
                if (!consumer.renderType.isTranslucent()) {
                    consumer.draw();
                }
            }
            for (CollectingConsumer consumer : buffers.values()) {
                if (consumer.renderType.isTranslucent()) {
                    consumer.draw();
                }
            }
        }

        public void endBatch(RenderType renderType) {
            CollectingConsumer consumer = buffers.get(renderType);
            if (consumer != null) {
                consumer.draw();
            }
        }
    }

    /**
     * Buffers vertices of one render type, grouped by packed light so each group is one draw call.
     */
    @SideOnly(Side.CLIENT)
    final class CollectingConsumer implements VertexConsumer {
        private static final int STRIDE = 12;

        private final RenderType renderType;
        private final Map<Integer, FloatList> groups = new LinkedHashMap<>();
        private float x, y, z;
        private int red = 255, green = 255, blue = 255, alpha = 255;
        private float u, v;
        private int light = LightTexture.FULL_BRIGHT;
        private float normalX, normalY = 1, normalZ;
        private int currentKey;
        private FloatList currentGroup;

        CollectingConsumer(RenderType renderType) {
            this.renderType = renderType;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.light = u & 0xFFFF | (v & 0xFFFF) << 16;
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.normalX = x;
            this.normalY = y;
            this.normalZ = z;
            return this;
        }

        @Override
        public void endVertex() {
            int key = renderType.isEmissive() ? LightTexture.FULL_BRIGHT : light;
            // consecutive vertices almost always share the light of their part: skip the map lookup then
            if (currentGroup == null || key != currentKey) {
                currentKey = key;
                currentGroup = groups.computeIfAbsent(key, k -> new FloatList());
            }
            currentGroup.add(x, y, z, red, green, blue, alpha, u, v, normalX, normalY, normalZ);
        }

        void draw() {
            boolean empty = true;
            for (FloatList data : groups.values()) {
                if (data.size > 0) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return;
            }
            boolean lightingWasEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
            float lastBrightnessX = net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX;
            float lastBrightnessY = net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
            renderType.setupRenderState();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder builder = tessellator.getBuffer();
            boolean positionColorOnly = renderType.getFormat() == DefaultVertexFormats.POSITION_COLOR;
            for (Map.Entry<Integer, FloatList> entry : groups.entrySet()) {
                FloatList data = entry.getValue();
                if (data.size == 0) {
                    continue;
                }
                renderType.applyLight(entry.getKey());
                builder.begin(renderType.getGlMode(), renderType.getFormat());
                for (int i = 0; i < data.size; i += STRIDE) {
                    float[] d = data.values;
                    builder.pos(d[i], d[i + 1], d[i + 2]);
                    builder.color((int) d[i + 3], (int) d[i + 4], (int) d[i + 5], (int) d[i + 6]);
                    if (!positionColorOnly) {
                        builder.tex(d[i + 7], d[i + 8]);
                        builder.normal(d[i + 9], d[i + 10], d[i + 11]);
                    }
                    builder.endVertex();
                }
                tessellator.draw();
            }
            // keep the lists (and their arrays) for the next frame
            for (FloatList data : groups.values()) {
                data.size = 0;
            }
            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                    net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            renderType.clearRenderState(lightingWasEnabled);
        }
    }

    final class FloatList {
        float[] values = new float[256];
        int size;

        void add(float x, float y, float z, float r, float g, float b, float a, float u, float v, float nx, float ny, float nz) {
            if (size + 12 > values.length) {
                values = Arrays.copyOf(values, Math.max(values.length * 2, size + 12));
            }
            float[] d = values;
            int i = size;
            d[i] = x;
            d[i + 1] = y;
            d[i + 2] = z;
            d[i + 3] = r;
            d[i + 4] = g;
            d[i + 5] = b;
            d[i + 6] = a;
            d[i + 7] = u;
            d[i + 8] = v;
            d[i + 9] = nx;
            d[i + 10] = ny;
            d[i + 11] = nz;
            size = i + 12;
        }
    }
}
