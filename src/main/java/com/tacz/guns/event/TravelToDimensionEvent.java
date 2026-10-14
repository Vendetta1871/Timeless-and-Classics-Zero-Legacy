package com.tacz.guns.event;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 修正跨纬度时，枪械数据不刷新的问题，这是服务端的刷新
 */
@Mod.EventBusSubscriber
public class TravelToDimensionEvent {
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityLivingBase livingEntity && livingEntity.getHeldItemMainhand().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(livingEntity).initialData();
        }
    }
}
