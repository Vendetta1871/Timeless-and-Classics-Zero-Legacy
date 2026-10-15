package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.InteractKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumHand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import com.tacz.guns.legacy.client.InputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class ClientPreventGunClick {
    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        // 当交互键按下时，允许交互
        if (InteractKey.INTERACT_KEY.isKeyDown()) {
            return;
        }
        // 只要主手有枪，那么禁止交互
        ItemStack itemInHand = player.getHeldItem(EnumHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            // 展示框可以交互
            RayTraceResult hitResult = Minecraft.getMinecraft().objectMouseOver;
            if (hitResult != null && hitResult.typeOfHit == RayTraceResult.Type.ENTITY && hitResult.entityHit instanceof EntityItemFrame) {
                return;
            }
            // 这个设置为 false 就能阻止客户端粒子的生成
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
