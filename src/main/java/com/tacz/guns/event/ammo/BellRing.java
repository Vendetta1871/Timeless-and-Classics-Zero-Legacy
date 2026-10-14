package com.tacz.guns.event.ammo;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import net.minecraft.world.World;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BellRing {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        World level = event.getLevel();
        IBlockState state = event.getState();
        RayTraceResult hitResult = event.getHitResult();
        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(level, hitResult.getBlockPos(), hitResult.getDirection());
        }
    }
}
