package com.tacz.guns.legacy.fp;

import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.PoseStack;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Port of SimpleBedrockModel's {@code IFPGeoItemRenderer} (the library TACZ 1.20.1 bundles): an item renderer that
 * draws the item itself in first person, replacing vanilla hand rendering.
 */
@SideOnly(Side.CLIENT)
public interface IFPGeoItemRenderer {
    default boolean isSameItem(ItemStack oldStack, ItemStack newStack) {
        return ItemStack.areItemsEqual(oldStack, newStack);
    }

    @Nullable
    default IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity) {
        return null;
    }

    default long getPutAwayDuration(ItemStack stack) {
        return 0;
    }

    default boolean blockOffhandRender() {
        return false;
    }

    void renderFirstPerson(EntityPlayerSP player, ItemStack stack, ItemCameraTransforms.TransformType ctx, PoseStack poseStack,
                           MultiBufferSource bufferSource, int light, float partialTick);
}
