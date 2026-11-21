package com.tacz.guns.block;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.init.ModContainer;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.HoverNames;
import com.tacz.guns.util.AmmoStorageHelper;
import com.tacz.guns.util.AmmoStorageInteractionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class AmmoStorageBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final int IRON_CAPACITY_STACKS = 216;
    public static final int GOLD_CAPACITY_STACKS = 432;
    public static final int DIAMOND_CAPACITY_STACKS = 864;
    /**
     * 1.12.2: {@code Material.IRON} requires a tool to drop anything; upstream's {@code Properties.of().mapColor(METAL)}
     * drops with any tool (it is only mineable with a pickaxe, see {@link #setHarvestLevel}).
     */
    private static final Material MATERIAL = new Material(MapColor.IRON);

    private final int capacityStacks;

    public AmmoStorageBlock() {
        this(IRON_CAPACITY_STACKS);
    }

    public AmmoStorageBlock(int capacityStacks) {
        super(MATERIAL, MapColor.IRON);
        this.setSoundType(SoundType.METAL);
        // strength(2.0F): 1.12.2 derives the explosion resistance (hardness * 5 / 5) from the hardness
        this.setHardness(2.0F);
        // tags/blocks/mineable/pickaxe.json
        this.setHarvestLevel("pickaxe", 0);
        this.capacityStacks = capacityStacks;
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public int getCapacityStacks() {
        return capacityStacks;
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
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new AmmoStorageBlockEntity();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(World level, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity blockEntity = level.getTileEntity(pos);
        if (!(blockEntity instanceof AmmoStorageBlockEntity storage)) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() instanceof IAmmoBox ammoBox) {
            if (level.isRemote) {
                return true;
            }
            if (player.isSneaking()) {
                return AmmoStorageInteractionHelper.unloadAmmoBox(storage, held, ammoBox, player) != EnumActionResult.PASS;
            }
            return fillAmmoBox(storage, held, ammoBox, player) != EnumActionResult.PASS;
        }
        if (level.isRemote) {
            return true;
        }
        if (player instanceof EntityPlayerMP) {
            // NetworkHooks.openScreen(serverPlayer, storage)
            ModContainer.openAmmoStorage(player, level, pos);
        }
        return true;
    }

    private EnumActionResult fillAmmoBox(AmmoStorageBlockEntity storage, ItemStack boxStack, IAmmoBox ammoBox, EntityPlayer player) {
        ResourceLocation ammoId = ammoBox.getAmmoId(boxStack);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
            player.sendStatusMessage(Components.translatable("message.tacz.ammo_storage.ammo_box_empty"), true);
            return EnumActionResult.SUCCESS;
        }
        int current = ammoBox.getAmmoCount(boxStack);
        int levelMultiplier = ammoBox.getAmmoLevel(boxStack) + 1;
        int max = AmmoStorageHelper.getStackSize(AmmoStorageHelper.createAmmoStack(ammoId, 1)) * com.tacz.guns.config.sync.SyncConfig.AMMO_BOX_STACK_SIZE.get() * levelMultiplier;
        int need = Math.max(0, max - current);
        if (need <= 0) {
            return EnumActionResult.SUCCESS;
        }
        ItemStack extracted = storage.extractAmmo(ammoId, need, false);
        if (extracted.isEmpty()) {
            player.sendStatusMessage(Components.translatable("message.tacz.ammo_storage.no_ammo_box_stock"), true);
            return EnumActionResult.SUCCESS;
        }
        ammoBox.setAmmoCount(boxStack, current + extracted.getCount());
        player.sendStatusMessage(Components.translatable("message.tacz.ammo_storage.picked", HoverNames.getHoverName(extracted), extracted.getCount()), true);
        player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 0.8F, 1.0F);
        return EnumActionResult.SUCCESS;
    }

    /**
     * 1.12.2: the block (and its block entity) would be removed before {@link #harvestBlock} computes the drops. Like
     * vanilla's flower pot, removal is delayed until the drops have been made so that {@link #getDrops} still sees the
     * block entity (upstream reads it from the loot context).
     */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, te, tool);
        world.setBlockToAir(pos);
    }

    /**
     * Upstream's {@code getDrops} ignores the loot table's explosion decay: an exploded storage always drops itself
     * with its contents (1.12.2 explosions drop with a chance of {@code 1 / size}).
     */
    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        super.dropBlockAsItemWithChance(world, pos, state, chance < 0 ? chance : 1.0F, fortune);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity blockEntity = world.getTileEntity(pos);
        ItemStack stack = new ItemStack(state.getBlock());
        if (blockEntity instanceof AmmoStorageBlockEntity storage) {
            NBTTagCompound tag = storage.saveWithoutMetadata();
            if (!tag.isEmpty()) {
                // BlockItem.setBlockEntityData(stack, ModBlocks.AMMO_STORAGE_BE.get(), tag)
                stack.setTagInfo("BlockEntityTag", tag);
            }
        }
        drops.add(stack);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World level, BlockPos pos) {
        TileEntity blockEntity = level.getTileEntity(pos);
        if (blockEntity instanceof AmmoStorageBlockEntity storage) {
            return storage.getComparatorSignal();
        }
        return 0;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }
}
