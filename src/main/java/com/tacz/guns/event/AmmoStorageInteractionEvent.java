package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.util.AmmoStorageInteractionHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class AmmoStorageInteractionEvent {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking() || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        ItemStack held = player.getHeldItem(event.getHand());
        if (!(held.getItem() instanceof IAmmoBox ammoBox)) {
            return;
        }
        World level = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AmmoStorageBlock)) {
            return;
        }
        if (level.isRemote) {
            // 1.12.2: PlayerControllerMP still sends the use packet for a cancelled event, the server handles it
            event.setCancellationResult(EnumActionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        TileEntity blockEntity = level.getTileEntity(pos);
        if (!(blockEntity instanceof AmmoStorageBlockEntity storage)) {
            return;
        }
        EnumActionResult result = AmmoStorageInteractionHelper.unloadAmmoBox(storage, held, ammoBox, player);
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
