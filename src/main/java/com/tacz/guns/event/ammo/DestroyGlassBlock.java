package com.tacz.guns.event.ammo;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.util.AmmoBlockConfigRead;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        if (!AmmoConfig.DESTROY_GLASS.get()) {
            return;
        }
        World level = event.getLevel();
        IBlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        float hardness = state.getBlockHardness(level, pos);
        if (hardness < 0 || hardness > AmmoConfig.DESTROY_BLOCK_HARDNESS_THRESHOLD.get()) {
            return;
        }
        EntityKineticBullet ammo = event.getAmmo();
        if (AmmoBlockConfigRead.canDestroy(state)) {
            // 1.12.2: World#destroyBlock has no destroying entity parameter
            level.destroyBlock(pos, false);
        }
    }
}
