package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class TargetMinecartItem extends Item {
    public TargetMinecartItem() {
        super();
        this.setMaxStackSize(1);
        this.setTranslationKey(GunMod.MOD_ID + ".target_minecart");
    }

    @NotNull
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World level, BlockPos blockpos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState blockstate = level.getBlockState(blockpos);
        // 1.12.2: no block tags, vanilla's own rail check (also covers modded rails)
        if (!BlockRailBase.isRailBlock(blockstate)) {
            return EnumActionResult.FAIL;
        } else {
            ItemStack itemstack = player.getHeldItem(hand);
            if (!level.isRemote) {
                BlockRailBase.EnumRailDirection railshape = blockstate.getBlock() instanceof BlockRailBase baseRailBlock ? baseRailBlock.getRailDirection(level, blockpos, blockstate, null) : BlockRailBase.EnumRailDirection.NORTH_SOUTH;
                double yOffset = 0;
                if (railshape.isAscending()) {
                    yOffset = 0.5;
                }
                TargetMinecart targetMinecart = new TargetMinecart(level, (double) blockpos.getX() + 0.5, (double) blockpos.getY() + 0.0625 + yOffset, (double) blockpos.getZ() + 0.5);
                if (itemstack.hasDisplayName()) {
                    targetMinecart.setCustomNameTag(itemstack.getDisplayName());
                }
                level.spawnEntity(targetMinecart);
            }
            itemstack.shrink(1);
            return EnumActionResult.SUCCESS;
        }
    }
}
