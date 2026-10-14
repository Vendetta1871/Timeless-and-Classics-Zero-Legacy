package com.tacz.guns.event.ammo;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        World level = event.getLevel();
        IBlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        EntityKineticBullet ammo = event.getAmmo();
        Block stateBlock = state.getBlock();
        // 1.12.2: no NoteBlockInstrument; iron bars (a BlockPane too) are not made of glass
        if (AmmoConfig.DESTROY_GLASS.get() && (stateBlock instanceof BlockGlass ||
                stateBlock instanceof BlockStainedGlass ||
                stateBlock instanceof BlockStainedGlassPane ||
                (stateBlock instanceof BlockPane && state.getMaterial() == Material.GLASS))) {
            // 1.12.2: World#destroyBlock has no destroying entity parameter
            level.destroyBlock(pos, false);
        }
    }
}
