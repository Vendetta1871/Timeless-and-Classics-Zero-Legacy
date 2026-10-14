package com.tacz.guns.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 双方块的枪械工作台，1x2x1
 */
public class GunSmithTableBlockC extends AbstractGunSmithTableBlock {
    public static final PropertyEnum<BlockDoublePlant.EnumBlockHalf> HALF = BlockDoublePlant.HALF;

    public GunSmithTableBlockC() {
        super();
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER));
    }


    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HALF);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(HALF, (meta & 4) != 0 ? BlockDoublePlant.EnumBlockHalf.UPPER : BlockDoublePlant.EnumBlockHalf.LOWER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, direction);
    }

    @Override
    public boolean canPlaceAt(World level, BlockPos pos, IBlockState state) {
        BlockPos above = pos.up();
        return level.getBlockState(above).getBlock().isReplaceable(level, above) && level.getWorldBorder().contains(above);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            BlockPos above = pos.up();
            world.setBlockState(above, state.withProperty(HALF, BlockDoublePlant.EnumBlockHalf.UPPER), 3);
            world.notifyNeighborsRespectDebug(pos, Blocks.AIR, false);
        }
    }

    /**
     * Upstream's {@code updateShape}: the block disappears when the other half does.
     */
    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos) {
        BlockDoublePlant.EnumBlockHalf half = state.getValue(HALF);
        if (half.equals(BlockDoublePlant.EnumBlockHalf.LOWER) && fromPos.equals(pos.up())
                || half.equals(BlockDoublePlant.EnumBlockHalf.UPPER) && fromPos.equals(pos.down())) {
            // 拆一半另外一半跟着没
            if (level.getBlockState(fromPos).getBlock() != this) {
                level.setBlockToAir(pos);
            }
        }
    }

    @Override
    public void onBlockHarvested(World level, BlockPos pos, IBlockState blockState, EntityPlayer player) {
        // 用于抑制创造模式下摧毁upper方块时lower的掉落
        if (!level.isRemote && player.capabilities.isCreativeMode) {
            BlockDoublePlant.EnumBlockHalf half = blockState.getValue(HALF);
            if (half == BlockDoublePlant.EnumBlockHalf.UPPER) {
                BlockPos blockpos = pos.down();
                IBlockState blockstate = level.getBlockState(blockpos);
                if (blockstate.getBlock() == this && blockstate.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER) {
                    level.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 35);
                    level.playEvent(player, 2001, blockpos, Block.getStateId(blockstate));
                }
            }
        }
        super.onBlockHarvested(level, pos, blockState, player);
    }

    @Override
    public boolean isRoot(IBlockState blockState) {
        return blockState.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER);
    }

    @Override
    public BlockPos getRootPos(BlockPos pos, IBlockState blockState) {
        return blockState.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER) ? pos : pos.down();
    }
}
