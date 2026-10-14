package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumHand;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.PoseStack;
import net.minecraftforge.fml.common.Mod;
import com.tacz.guns.legacy.render.RenderBuffers;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderEvent {
    private static AnimationStateMachine<?> lastStateMachine = null;

    /**
     * Unused upstream (SimpleBedrockModel's handler renders first person, see {@code legacy.fp}); kept ported.
     * 1.12.2: {@code RenderHandEvent} is {@link RenderSpecificHandEvent}, which has no pose stack, buffers or light.
     */
//    @SubscribeEvent
    public static void onRenderHand(RenderSpecificHandEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        if (event.getHand() == EnumHand.OFF_HAND) {
            ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
            if (stack.getItem() instanceof IGun) {
                event.setCanceled(true);
            }
            return;
        }
        // 事件事件给的是被延长渲染修改过后的物品，不是玩家实际手持的
        ItemStack stack = event.getItemStack();

        // 获取 TransformType
        ItemCameraTransforms.TransformType transformType;
        if (event.getHand() == EnumHand.MAIN_HAND) {
            transformType = ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
        } else {
            transformType = ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
        }

        // 渲染相关内容整理到物品的IClientItemExtensions了，这个接口有待进一步抽象
        if (stack.getItem().getTileEntityItemStackRenderer() instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            // 如果旧的状态机已经不再使用且未正常退出，使其静默退出
            AnimationStateMachine<?> machine = renderer.getStateMachine(stack);
            if (machine != lastStateMachine) {
                if (lastStateMachine != null && lastStateMachine.isInitialized()) {
                    lastStateMachine.exit();
                }
                lastStateMachine = machine;
            }
            // 物品处于后台时，阻止状态机初始化
            boolean flag = ItemStack.areItemStacksEqual(player.getHeldItemMainhand(), stack);
            if (flag && renderer.needReInit(stack)) {
                renderer.tryInit(stack, player, event.getPartialTicks());
            }

			// 防止内存泄漏
			OculusCompat.endBatch(RenderBuffers.bufferSource());

            renderer.renderFirstPerson(player, stack, transformType, new PoseStack(), RenderBuffers.bufferSource(),
                    LightTexture.current(), event.getPartialTicks());
            event.setCanceled(true);
        }
    }
}
