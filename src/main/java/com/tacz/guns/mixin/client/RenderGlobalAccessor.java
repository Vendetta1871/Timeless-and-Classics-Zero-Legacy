package com.tacz.guns.mixin.client;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.12.2 only: the scope view pass and the main pass need their own visible chunk lists
 * ({@link com.tacz.guns.legacy.client.ScopeView}).
 */
@Mixin(RenderGlobal.class)
public interface RenderGlobalAccessor {
    @Accessor("displayListEntitiesDirty")
    void tacz$setDisplayListEntitiesDirty(boolean dirty);
}
