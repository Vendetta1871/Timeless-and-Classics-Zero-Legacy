package com.tacz.guns.legacy.fp;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;

/**
 * Port of SimpleBedrockModel's (2.6.3) {@code v1.client.animation.IFPAnimationInstance}. The MAE pose accessors are left out: TACZ only returns
 * a dummy pose from them.
 */
@SideOnly(Side.CLIENT)
public interface IFPAnimationInstance {
    ItemStack currentItem();

    void tick(float partialTicks);

    Quaternionf getCameraRotation();

    void setCameraRotation(Quaternionf cameraRotation);

    void updateItem(ItemStack stack);

    void triggerDraw();

    void triggerPutAway();

    /**
     * 本实例（当前固定的渲染形态）是否霸占整个第一人称视野，从而禁止副手渲染。默认 {@code true}。
     */
    default boolean occupiesView() {
        return true;
    }

    default boolean shouldRenderHand() {
        return false;
    }
}
