package com.tacz.guns.legacy.fp;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;

/**
 * Port of SimpleBedrockModel's {@code IFPAnimationInstance}. The MAE pose accessors are left out: TACZ only returns
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

    default boolean shouldRenderHand() {
        return false;
    }
}
