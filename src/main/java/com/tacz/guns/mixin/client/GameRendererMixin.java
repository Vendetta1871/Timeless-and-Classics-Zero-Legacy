package com.tacz.guns.mixin.client;

import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.legacy.client.FovContext;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.12.2: {@code GameRenderer} is {@link EntityRenderer}; {@code bobHurt}/{@code bobView}/{@code getFov} are
 * {@code hurtCameraEffect}/{@code applyBobbing}/{@code getFOVModifier}. Those transform the GL matrix rather than a
 * pose stack, so a tweaked pose is applied to the GL matrix before cancelling the vanilla effect.
 */
@Mixin(EntityRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private boolean tacz$useFovSetting;

    @Shadow
    @Final
    private Minecraft mc;

    @Inject(method = "hurtCameraEffect(F)V", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(float pPartialTicks, CallbackInfo ci) {
        // 取消受伤导致的视角摇晃
        if (this.mc.getRenderViewEntity() instanceof EntityPlayerSP player && player.getHealth() > 0) {
            PoseStack pMatrixStack = new PoseStack();
            if (GunHurtBobTweak.onHurtBobTweak(player, pMatrixStack, pPartialTicks)) {
                RenderSystem.multMatrix(pMatrixStack.last().pose());
                ci.cancel();
                return;
            }
        }
        // 触发其他事件
        boolean cancel;
        if (!tacz$useFovSetting) {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderItemInHandBobEvent.BobHurt());
        } else {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderLevelBobEvent.BobHurt());
        }
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "applyBobbing(F)V", at = @At("HEAD"), cancellable = true)
    public void onBobView(float pPartialTicks, CallbackInfo ci) {
        boolean cancel;
        if (!tacz$useFovSetting) {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderItemInHandBobEvent.BobView());
        } else {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderLevelBobEvent.BobView());
        }
        if (cancel) {
            ci.cancel();
        }
    }

    /**
     * 是一个 hack 实现。因为 getFov 这个方法只有在构建 投影矩阵 的时候调用。
     * 因此可以根据 getFov 中的 pUseFovSetting 来判断当前准备渲染 Level 还是渲染 HandWithItem 。
     * 至于为什么不直接对 renderItemInHand 这个方法 mixin ，是因为安装了 Optifine 之后，这个方法的内容被大幅度修改了。
     * <p>
     * 1.12.2: also published through {@link FovContext} for the FOV event, which lacks {@code usedConfiguredFov()}.
     */
    @Inject(method = "getFOVModifier(FZ)F", at = @At("HEAD"))
    public void switchRenderType(float pPartialTicks, boolean pUseFOVSetting, CallbackInfoReturnable<Float> cir) {
        this.tacz$useFovSetting = pUseFOVSetting;
        FovContext.setUseFovSetting(pUseFOVSetting);
    }
}
