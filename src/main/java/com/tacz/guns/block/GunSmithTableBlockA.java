package com.tacz.guns.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 单方块的枪械工作台
 */
public class GunSmithTableBlockA extends AbstractGunSmithTableBlock {
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, direction);
    }

    @Override
    public boolean isRoot(IBlockState blockState) {
        return true;
    }

    @Override
    public BlockPos getRootPos(BlockPos pos, IBlockState blockState) {
        return pos;
    }
}
