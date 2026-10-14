package com.tacz.guns.legacy.render;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stands in for {@code Minecraft.getInstance().renderBuffers()}: one shared buffer source for the render thread.
 */
@SideOnly(Side.CLIENT)
public final class RenderBuffers {
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE = new MultiBufferSource.BufferSource();

    private RenderBuffers() {
    }

    public static MultiBufferSource.BufferSource bufferSource() {
        return BUFFER_SOURCE;
    }
}
