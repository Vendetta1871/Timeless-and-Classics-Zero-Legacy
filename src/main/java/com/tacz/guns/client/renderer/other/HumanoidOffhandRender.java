package com.tacz.guns.client.renderer.other;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.pojo.display.gun.LayerGunShow;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.render.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import com.tacz.guns.legacy.render.OverlayTexture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class HumanoidOffhandRender {
    public static void renderGun(EntityLivingBase entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        renderOffhandGun(entity, matrixStack, buffer, packedLight);
        renderHotbarGun(entity, matrixStack, buffer, packedLight);
    }

    private static void renderOffhandGun(EntityLivingBase entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        ItemStack itemStack = entity.getOffhandItem();
        if (itemStack.isEmpty()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(itemStack).ifPresent(index -> {
            LayerGunShow offhandShow = index.getOffhandShow();
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, offhandShow);
        });
    }

    private static void renderHotbarGun(EntityLivingBase entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        if (!(entity instanceof EntityPlayer player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (i == inventory.selected) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            renderHotbarGun(entity, matrixStack, buffer, packedLight, stack, i);
        }
    }

    private static void renderHotbarGun(EntityLivingBase entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, int inventoryIndex) {
        if (itemStack.isEmpty()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(itemStack).ifPresent(display -> {
            var hotbarShow = display.getHotbarShow();
            if (hotbarShow == null || hotbarShow.isEmpty()) {
                return;
            }
            if (!hotbarShow.containsKey(inventoryIndex)) {
                return;
            }
            LayerGunShow gunShow = hotbarShow.get(inventoryIndex);
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, gunShow);
        });
    }

    private static void renderGunItem(EntityLivingBase entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, LayerGunShow offhandShow) {
        ItemRenderer renderer = Minecraft.getMinecraft().getItemRenderer();
        Vector3f pos = offhandShow.getPos();
        Vector3f rotate = offhandShow.getRotate();
        Vector3f scale = offhandShow.getScale();
        matrixStack.pushPose();
        matrixStack.translate(-pos.x() / 16f, 1.5 - pos.y() / 16f, pos.z() / 16f);
        matrixStack.scale(-scale.x(), -scale.y(), scale.z());
        Quaternionf rotation = new Quaternionf();
        MathUtil.toQuaternion((float) Math.toRadians(rotate.x), (float) Math.toRadians(rotate.y), (float) Math.toRadians(rotate.z), rotation);
        matrixStack.mulPose(rotation);
        renderer.renderStatic(itemStack, ItemCameraTransforms.TransformType.FIXED, packedLight, OverlayTexture.NO_OVERLAY, matrixStack, buffer, entity.level(), entity.getId());
        matrixStack.popPose();
    }
}
