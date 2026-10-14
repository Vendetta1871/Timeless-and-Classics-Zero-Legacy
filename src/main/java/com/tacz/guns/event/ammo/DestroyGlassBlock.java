package com.tacz.guns.event.ammo;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        World level = event.getLevel();
        IBlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        EntityKineticBullet ammo = event.getAmmo();
        Block stateBlock = state.getBlock();
        NoteBlockInstrument instrument = state.instrument();
        if (AmmoConfig.DESTROY_GLASS.get() && (stateBlock instanceof AbstractGlassBlock ||
                stateBlock instanceof StainedGlassPaneBlock ||
                (stateBlock instanceof IronBarsBlock && instrument.equals(NoteBlockInstrument.HAT)))) {
            level.destroyBlock(pos, false, ammo.getOwner());
        }
    }
}
