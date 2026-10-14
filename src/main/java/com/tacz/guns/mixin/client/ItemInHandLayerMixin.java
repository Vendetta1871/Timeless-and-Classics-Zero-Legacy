package com.tacz.guns.mixin.client;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2: {@code ItemInHandLayer} is {@link LayerHeldItem} ({@code render} → {@code doRenderLayer},
 * {@code renderArmWithItem} → {@code renderHeldItem}). The layer draws with the GL matrix in body model space, which
 * is where upstream's pose stack is, so the extra guns are drawn with a fresh pose on top of it.
 */
@Mixin(LayerHeldItem.class)
public class ItemInHandLayerMixin {
    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", at = @At("HEAD"))
    private void render(EntityLivingBase livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                        float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
        MultiBufferSource.BufferSource buffer = RenderBuffers.bufferSource();
        HumanoidOffhandRender.renderGun(livingEntity, new PoseStack(), buffer, LightTexture.current());
        buffer.endBatch();
    }

    @Inject(method = "renderHeldItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Lnet/minecraft/util/EnumHandSide;)V",
            at = @At("HEAD"), cancellable = true)
    private void renderArmWithItemHead(EntityLivingBase livingEntity, ItemStack itemStack, ItemCameraTransforms.TransformType pDisplayContext, EnumHandSide arm, CallbackInfo ci) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (livingEntity.equals(player)) {
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
        }
        if (IGun.mainHandHoldGun(livingEntity) && arm == EnumHandSide.LEFT) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Lnet/minecraft/util/EnumHandSide;)V",
            at = @At("TAIL"))
    private void renderArmWithItemTail(EntityLivingBase livingEntity, ItemStack itemStack, ItemCameraTransforms.TransformType pDisplayContext, EnumHandSide arm, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
    }
}
