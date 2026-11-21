package com.tacz.guns.mixin.client;

import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2: {@code ItemInHandRenderer} is {@link ItemRenderer}, {@code renderHandsWithItems} is
 * {@code renderItemInFirstPerson} and {@code tick} is {@code updateEquippedItem}. The hand pose that
 * {@link BeforeRenderHandEvent} listeners change is applied to the GL matrix, which 1.12.2 renders the hands with
 * ({@code EntityRenderer#renderHand} pops it afterwards).
 */
@Mixin(ItemRenderer.class)
public class ItemInHandRendererMixin implements KeepingItemRenderer {
    @Shadow
    private float equippedProgressMainHand;
    @Shadow
    private float prevEquippedProgressMainHand;
    @Shadow
    private ItemStack itemStackMainHand;
    @Unique
    private ItemStack tacz$KeepItem;
    @Unique
    private long tacz$KeepTimeMs;
    @Unique
    private long tacz$KeepTimestamp;
    @Unique
    private long tacz$KeepEpoch = -1;

    @Inject(method = "renderItemInFirstPerson(F)V", at = @At("HEAD"))
    public void beforeHandRender(float pPartialTicks, CallbackInfo ci) {
        PoseStack pMatrixStack = new PoseStack();
        MinecraftForge.EVENT_BUS.post(new BeforeRenderHandEvent(pMatrixStack));
        RenderSystem.multMatrix(pMatrixStack.last().pose());
    }

    @Inject(method = "updateEquippedItem()V", at = @At("HEAD"))
    public void cancelEquippedProgress(CallbackInfo ci) {
//        if (Minecraft.getInstance().player == null) {
//            return;
//        }
//        if (tacz$KeepItem != null) {
//            long time = GunTime.nowMillis() - tacz$KeepTimestamp;
//            if (time < tacz$KeepTimeMs) {
//                mainHandHeight = 1.0f;
//                oMainHandHeight = 1.0f;
//                mainHandItem = tacz$KeepItem;
//                return;
//            }
//        }
//        ItemStack itemStack = Minecraft.getInstance().player.getMainHandItem();
//        IGun iGun = IGun.getIGunOrNull(itemStack);
//        if (iGun != null) {
//            mainHandHeight = 1.0f;
//            oMainHandHeight = 1.0f;
//            mainHandItem = itemStack;
//        }
    }

    @Unique
    @Override
    public void keep(ItemStack itemStack, long timeMs) {
        if (tacz$KeepItem != null && tacz$KeepEpoch == GunTime.epoch()
                && GunTime.isWithin(tacz$KeepTimestamp, tacz$KeepTimeMs)) {
            return;
        }
        this.tacz$KeepTimeMs = timeMs;
        this.tacz$KeepTimestamp = GunTime.nowMillis();
        this.tacz$KeepEpoch = GunTime.epoch();
        this.tacz$KeepItem = itemStack;
        this.itemStackMainHand = itemStack;
    }

    @Override
    public ItemStack getCurrentItem() {
        if (Minecraft.getMinecraft().player == null) {
            return itemStackMainHand;
        }
        if (tacz$KeepItem != null) {
            if (tacz$KeepEpoch == GunTime.epoch() && GunTime.isWithin(tacz$KeepTimestamp, tacz$KeepTimeMs)) {
                return tacz$KeepItem;
            } else {
                tacz$KeepItem = null;
            }
        }
        return itemStackMainHand;
    }
}
