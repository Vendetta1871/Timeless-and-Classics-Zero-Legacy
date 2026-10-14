package com.tacz.guns.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
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
 * 双方块的枪械工作台，2x1x1
 */
public class GunSmithTableBlockB extends AbstractGunSmithTableBlock {
    public static final PropertyEnum<BlockBed.EnumPartType> PART = BlockBed.PART;

    public GunSmithTableBlockB() {
        super();
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(PART, BlockBed.EnumPartType.FOOT));
    }

    private static EnumFacing getNeighbourDirection(BlockBed.EnumPartType bedPart, EnumFacing direction) {
        return bedPart == BlockBed.EnumPartType.FOOT ? direction : direction.getOpposite();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, PART);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(PART, (meta & 4) != 0 ? BlockBed.EnumPartType.HEAD : BlockBed.EnumPartType.FOOT);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(PART) == BlockBed.EnumPartType.HEAD ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = placer.getHorizontalFacing().rotateY();
        return this.getDefaultState().withProperty(FACING, direction);
    }

    @Override
    public boolean canPlaceAt(World level, BlockPos pos, IBlockState state) {
        BlockPos relative = pos.offset(state.getValue(FACING));
        return level.getBlockState(relative).getBlock().isReplaceable(level, relative) && level.getWorldBorder().contains(relative);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (!worldIn.isRemote) {
            BlockPos relative = pos.offset(state.getValue(FACING));
            worldIn.setBlockState(relative, state.withProperty(PART, BlockBed.EnumPartType.HEAD), 3);
            worldIn.notifyNeighborsRespectDebug(pos, Blocks.AIR, false);
        }
    }

    @Override
    public void onBlockHarvested(World level, BlockPos pos, IBlockState blockState, EntityPlayer player) {
        // 用于抑制创造模式下摧毁head方块时foot的掉落
        if (!level.isRemote && player.capabilities.isCreativeMode) {
            BlockBed.EnumPartType bedPart = blockState.getValue(PART);
            if (bedPart == BlockBed.EnumPartType.FOOT) {
                BlockPos blockpos = pos.offset(getNeighbourDirection(bedPart, blockState.getValue(FACING)));
                IBlockState blockstate = level.getBlockState(blockpos);
                if (blockstate.getBlock() == this && blockstate.getValue(PART) == BlockBed.EnumPartType.HEAD) {
                    level.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 35);
                    level.playEvent(player, 2001, blockpos, Block.getStateId(blockstate));
                }
            }
        }
        super.onBlockHarvested(level, pos, blockState, player);
    }

    /**
     * Upstream's {@code updateShape}: the block disappears when the other half does.
     */
    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos) {
        EnumFacing direction = getNeighbourDirection(state.getValue(PART), state.getValue(FACING));
        if (pos.offset(direction).equals(fromPos)) {
            IBlockState facingState = level.getBlockState(fromPos);
            if (!(facingState.getBlock() == this && facingState.getValue(PART) != state.getValue(PART))) {
                level.setBlockToAir(pos);
            }
        }
    }

    @Override
    public boolean isRoot(IBlockState blockState) {
        return blockState.getValue(PART).equals(BlockBed.EnumPartType.FOOT);
    }

    @Override
    public float parseRotation(EnumFacing direction) {
        return 90 - 90 * direction.getHorizontalIndex();
    }

    @Override
    public BlockPos getRootPos(BlockPos pos, IBlockState blockState) {
        return blockState.getValue(PART).equals(BlockBed.EnumPartType.FOOT) ? pos : pos.offset(getNeighbourDirection(BlockBed.EnumPartType.HEAD, blockState.getValue(FACING)));
    }
}
