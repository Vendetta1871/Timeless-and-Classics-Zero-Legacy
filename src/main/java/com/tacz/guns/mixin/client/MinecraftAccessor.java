package com.tacz.guns.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.12.2 only: the scope view pass stands its framebuffer in for the main one
 * ({@link com.tacz.guns.legacy.client.ScopeView}).
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("framebuffer")
    void tacz$setFramebuffer(Framebuffer framebuffer);
}
