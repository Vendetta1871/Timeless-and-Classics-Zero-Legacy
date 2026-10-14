package com.tacz.guns.block;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.init.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public abstract class AbstractGunSmithTableBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public AbstractGunSmithTableBlock() {
        super(Material.WOOD);
        this.setSoundType(SoundType.WOOD);
        // strength(2.0F, 3.0F): 1.12.2 multiplies the explosion resistance by 3 in setResistance
        this.setHardness(2.0F);
        this.setResistance(1.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean onBlockActivated(World level, BlockPos pos, IBlockState pState, EntityPlayer player, EnumHand pHand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (level.isRemote) {
            return true;
        } else {
            BlockPos rootPos = getRootPos(pos, pState);
            TileEntity blockEntity = level.getTileEntity(rootPos);
            if (blockEntity instanceof GunSmithTableBlockEntity gunSmithTable && player instanceof EntityPlayerMP serverPlayer) {
                // the menu reads the block id from the block entity, see ModContainer (upstream writes it to the buffer)
                ModContainer.openGunSmithTable(serverPlayer, level, rootPos);
            }
            return true;
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new GunSmithTableBlockEntity();
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

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            if (stack.getItem() instanceof BlockItemDataAccessor accessor) {
                ResourceLocation id = accessor.getBlockId(stack);
                TileEntity blockentity = world.getTileEntity(pos);
                if (blockentity instanceof GunSmithTableBlockEntity e) {
                    e.setId(id);
                }
            }
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World level, BlockPos pos, EntityPlayer player) {
        BlockPos blockPos = getRootPos(pos, state);
        TileEntity blockentity = level.getTileEntity(blockPos);
        if (blockentity instanceof GunSmithTableBlockEntity e) {
            if (e.getId() != null) {
                return BlockItemBuilder.create(Item.getItemFromBlock(this)).setId(e.getId()).build();
            }
            return new ItemStack(this);
        }
        return super.getPickBlock(state, target, level, pos, player);
    }

    /**
     * Upstream's loot tables (data/tacz/loot_tables/blocks): the root part drops the item, with the block entity's
     * {@code BlockId} copied into it.
     */
    private ItemStack getDropStack(IBlockState state, @Nullable TileEntity blockEntity) {
        if (!isRoot(state)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(this);
        if (blockEntity instanceof GunSmithTableBlockEntity e && e.getId() != null && stack.getItem() instanceof BlockItemDataAccessor accessor) {
            accessor.setBlockId(stack, e.getId());
        }
        return stack;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = getDropStack(state, world.getTileEntity(pos));
        if (!stack.isEmpty()) {
            drops.add(stack);
        }
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        // the block entity is already gone when the drops are collected: read it from the argument
        player.addExhaustion(0.005F);
        if (!world.isRemote && !world.restoringBlockSnapshots) {
            ItemStack stack = getDropStack(state, te);
            if (!stack.isEmpty()) {
                spawnAsEntity(world, pos, stack);
            }
        }
    }

    public abstract boolean isRoot(IBlockState blockState);

    public float parseRotation(EnumFacing direction) {
        return 90.0F * (3 - direction.getHorizontalIndex()) - 90;
    }

    public abstract BlockPos getRootPos(BlockPos pos, IBlockState blockState);

    /**
     * 1.12.2: upstream's {@code getStateForPlacement} returns {@code null} when the second half has no room; the
     * item (see {@code GunSmithTableItem#placeBlockAt}) asks this before placing.
     */
    public boolean canPlaceAt(World level, BlockPos pos, IBlockState state) {
        return true;
    }
}
