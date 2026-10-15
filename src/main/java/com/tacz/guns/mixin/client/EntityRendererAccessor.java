package com.tacz.guns.mixin.client;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.12.2 only: lets the scope view pass skip the hand ({@link com.tacz.guns.legacy.client.ScopeView}).
 */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Accessor("renderHand")
    boolean tacz$getRenderHand();

    @Accessor("renderHand")
    void tacz$setRenderHand(boolean renderHand);
}
