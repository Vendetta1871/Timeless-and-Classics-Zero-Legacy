package com.tacz.guns.block;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.entity.StatueBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class StatueBlock extends Block {
    public static final PropertyEnum<BlockDoublePlant.EnumBlockHalf> HALF = BlockDoublePlant.HALF;
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public StatueBlock() {
        super(Material.ROCK);
        this.setSoundType(SoundType.STONE);
        // strength(2.0F, 3.0F): 1.12.2 multiplies the explosion resistance by 3 in setResistance
        this.setHardness(2.0F);
        this.setResistance(1.0F);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER)
                .withProperty(FACING, EnumFacing.NORTH)
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF, FACING);
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
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState pState) {
        return pState.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER ? new StatueBlockEntity() : null;
    }

    @Override
    public boolean onBlockActivated(World level, BlockPos pos, IBlockState pState, EntityPlayer player, EnumHand pHand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (level.isRemote) {
            return true;
        } else {
            if (pState.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER) {
                pos = pos.down();
            }

            TileEntity blockEntity = level.getTileEntity(pos);
            if (blockEntity instanceof StatueBlockEntity statueBlockEntity) {
                ItemStack stack = player.getHeldItem(pHand);
                if (stack.getItem() instanceof IGun) {
                    statueBlockEntity.setGun(stack);
                    stack.shrink(1);
                    return true;
                }

                if (stack.isEmpty()) {
                    statueBlockEntity.dropItem();
                    return true;
                }
            }
            return true;
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, direction);
    }

    /**
     * Upstream's {@code getStateForPlacement} returns {@code null} when the upper half has no room.
     */
    @Override
    public boolean canPlaceBlockAt(World level, BlockPos pos) {
        BlockPos above = pos.up();
        return super.canPlaceBlockAt(level, pos) && level.getBlockState(above).getBlock().isReplaceable(level, above)
                && level.getWorldBorder().contains(above);
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
    public void breakBlock(World pLevel, BlockPos pPos, IBlockState pState) {
        TileEntity blockentity = pLevel.getTileEntity(pPos);
        if (blockentity instanceof StatueBlockEntity statueBlockEntity) {
            statueBlockEntity.dropItem();
        }
        super.breakBlock(pLevel, pPos, pState);
    }

    /**
     * Upstream's loot table: only the lower half drops the statue.
     */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER) {
            drops.add(new ItemStack(this));
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }
}
