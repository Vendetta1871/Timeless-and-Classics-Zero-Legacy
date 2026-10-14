package com.tacz.guns.block;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.block.entity.TargetBlockEntity;
// TODO(port): needs EntityKineticBullet
// import com.tacz.guns.entity.EntityKineticBullet;
// TODO(port): needs ProjectileHitBlock
// import com.tacz.guns.legacy.entity.Projectile;
// import com.tacz.guns.legacy.entity.ProjectileHitBlock;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// TODO(port): needs ProjectileHitBlock
// public class TargetBlock extends Block implements ProjectileHitBlock {
public class TargetBlock extends Block {
    /**
     * 1.12.2: block states have at most 16 variants: upstream's {@code OUTPUT_POWER} property is kept in the lower
     * half's {@link TargetBlockEntity}, see {@link #getOutputPower}.
     */
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<BlockDoublePlant.EnumBlockHalf> HALF = BlockDoublePlant.HALF;
    public static final PropertyBool STAND = PropertyBool.create("stand");
    // 1.12.2: a VoxelShape is a list of boxes
    public static final List<AxisAlignedBB> BOX_BOTTOM_STAND_X = Collections.unmodifiableList(Arrays.asList(box(6, 0, 6, 10, 16, 10), box(6, 13, 2, 10, 16, 14)));
    public static final List<AxisAlignedBB> BOX_BOTTOM_STAND_Z = Collections.unmodifiableList(Arrays.asList(box(6, 0, 6, 10, 16, 10), box(2, 13, 6, 14, 16, 10)));
    public static final List<AxisAlignedBB> BOX_BOTTOM_DOWN = Collections.singletonList(box(6, 0, 6, 10, 4, 10));
    public static final List<AxisAlignedBB> BOX_UPPER_X = Collections.singletonList(box(6, 0, 2, 10, 16, 14));
    public static final List<AxisAlignedBB> BOX_UPPER_Z = Collections.singletonList(box(2, 0, 6, 14, 16, 10));

    public TargetBlock() {
        super(Material.WOOD);
        this.setSoundType(SoundType.WOOD);
        // strength(2.0F, 3.0F): 1.12.2 multiplies the explosion resistance by 3 in setResistance
        this.setHardness(2.0F);
        this.setResistance(1.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER).withProperty(STAND, true));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    public static int getRedstoneStrength(RayTraceResult hit, boolean isUpperBlock) {
        // 击中下方，恒为 1
        if (!isUpperBlock) {
            return 1;
        }
        Vec3d hitLocation = hit.hitVec;
        EnumFacing direction = hit.sideHit;
        // 标靶中心为 (0.5, 0.32, 0.5)
        double x = Math.abs(frac(hitLocation.x) - 0.5);
        double y = Math.abs(frac(hitLocation.y) - 0.32);
        double z = Math.abs(frac(hitLocation.z) - 0.5);
        EnumFacing.Axis axis = direction.getAxis();
        double distance;
        if (axis == EnumFacing.Axis.Y) {
            distance = Math.max(x, z);
        } else if (axis == EnumFacing.Axis.Z) {
            distance = Math.max(x, y);
        } else {
            distance = Math.max(y, z);
        }
        // 离开中心 0.25 单位就是最低分？
        double percent = MathHelper.clamp((0.25 - distance) / 0.25, 0, 1);
        return Math.max(1, MathHelper.ceil(15 * percent));
    }

    /**
     * {@code Mth.frac}
     */
    private static double frac(double value) {
        return value - Math.floor(value);
    }

    /**
     * Upstream's {@code OUTPUT_POWER} block state value of either half.
     */
    public static int getOutputPower(IBlockAccess level, BlockPos pos, IBlockState state) {
        BlockPos lowerPos = state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER) ? pos : pos.down();
        TileEntity blockEntity = level.getTileEntity(lowerPos);
        return blockEntity instanceof TargetBlockEntity target ? target.getOutputPower() : 0;
    }

    /**
     * Upstream sets {@code STAND} and {@code OUTPUT_POWER} on the lower half and lets {@code updateShape} copy them to
     * the upper half; here both halves are set directly.
     */
    public static void setStandState(World level, BlockPos lowerPos, IBlockState lowerState, boolean stand, int outputPower) {
        TileEntity blockEntity = level.getTileEntity(lowerPos);
        if (blockEntity instanceof TargetBlockEntity target) {
            target.setOutputPower(outputPower);
            target.markDirty();
        }
        level.setBlockState(lowerPos, lowerState.withProperty(STAND, stand), 3);
        BlockPos upperPos = lowerPos.up();
        IBlockState upperState = level.getBlockState(upperPos);
        if (upperState.getBlock() == lowerState.getBlock()) {
            level.setBlockState(upperPos, upperState.withProperty(STAND, stand), 3);
        }
        level.notifyNeighborsOfStateChange(lowerPos, lowerState.getBlock(), false);
        level.notifyNeighborsOfStateChange(upperPos, lowerState.getBlock(), false);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HALF, STAND);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(HALF, (meta & 4) != 0 ? BlockDoublePlant.EnumBlockHalf.UPPER : BlockDoublePlant.EnumBlockHalf.LOWER)
                .withProperty(STAND, (meta & 8) == 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER ? 4 : 0)
                | (state.getValue(STAND) ? 0 : 8);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState blockState) {
        if (blockState.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER)) {
            return new TargetBlockEntity();
        } else {
            return null;
        }
    }

    /**
     * Upstream's {@code getShape}: every box of the shape.
     */
    public List<AxisAlignedBB> getShape(IBlockState state) {
        boolean stand = state.getValue(STAND);
        boolean axis = state.getValue(FACING).getAxis().equals(EnumFacing.Axis.X);
        if (state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.UPPER)) {
            return stand ? (axis ? BOX_UPPER_X : BOX_UPPER_Z) : Collections.<AxisAlignedBB>emptyList();
        }
        return stand ? (axis ? BOX_BOTTOM_STAND_X : BOX_BOTTOM_STAND_Z) : BOX_BOTTOM_DOWN;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        List<AxisAlignedBB> shape = getShape(state);
        if (shape.isEmpty()) {
            return NULL_AABB;
        }
        AxisAlignedBB bounds = shape.get(0);
        for (AxisAlignedBB box : shape) {
            bounds = bounds.union(box);
        }
        return bounds;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        for (AxisAlignedBB box : getShape(state)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
        }
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (AxisAlignedBB box : getShape(blockState)) {
            RayTraceResult result = rayTrace(pos, start, end, box);
            if (result != null) {
                double distance = result.hitVec.squareDistanceTo(start);
                if (distance < nearestDistance) {
                    nearest = result;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    @Override
    public void updateTick(World level, BlockPos pos, IBlockState state, Random random) {
        // 计划刻的内容
        if (!state.getValue(STAND)) {
            setStandState(level, pos, state, true, 0);
        }
    }

    // TODO(port): needs ProjectileHitBlock
//    @Override
//    public void onProjectileHit(World world, IBlockState state, RayTraceResult hit, Projectile projectile) {
//        if (hit.sideHit.getOpposite().equals(state.getValue(FACING))) {
//            if (state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER)) {
//                if (world.getTileEntity(hit.getBlockPos()) instanceof TargetBlockEntity e) {
//                    e.hit(world, state, hit, false);
//                }
//            } else if (state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.UPPER)) {
//                if (world.getTileEntity(hit.getBlockPos().down()) instanceof TargetBlockEntity e) {
//                    e.hit(world, state, hit, true);
//                }
//            }
//
//            if (!world.isRemote && projectile.getOwner() instanceof EntityPlayer player && state.getValue(STAND)) {
//                if (projectile instanceof EntityKineticBullet bullet) {
//                    String formattedDamage = String.format("%.1f", bullet.getDamage(hit.hitVec));
//                    String formattedDistance = String.format("%.2f", hit.hitVec.distanceTo(player.getPositionVector()));
//                    player.sendStatusMessage(Components.translatable("message.tacz.target_minecart.hit", formattedDamage, formattedDistance), true);
//                }
//
//            }
//        }
//    }

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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = placer.getHorizontalFacing();
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
            if (stack.hasDisplayName()) {
                TileEntity blockentity = world.getTileEntity(pos);
                if (blockentity instanceof TargetBlockEntity e) {
                    GameProfile gameprofile = new GameProfile(null, stack.getDisplayName());
                    e.setOwner(gameprofile);
                    e.setCustomName(Components.literal(stack.getDisplayName()));
                    e.refresh();
                }
            }
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World level, BlockPos pos, EntityPlayer player) {
        BlockPos blockPos = state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER ? pos : pos.down();
        TileEntity blockentity = level.getTileEntity(blockPos);
        if (blockentity instanceof TargetBlockEntity e) {
            return getDropStack(state, e);
        }
        return super.getPickBlock(state, target, level, pos, player);
    }

    /**
     * Upstream's loot table: only the lower half drops the target, with the block entity's name ({@code copy_name}).
     */
    private ItemStack getDropStack(IBlockState state, @Nullable TileEntity blockEntity) {
        if (!state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(this);
        if (blockEntity instanceof TargetBlockEntity e && e.getCustomName() != null) {
            stack.setStackDisplayName(e.getCustomName().getUnformattedText());
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

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return getOutputPower(blockAccess, pos, blockState);
    }

    @Override
    public void onBlockAdded(World level, BlockPos pos, IBlockState state) {
        if (!level.isRemote && state.getValue(HALF).equals(BlockDoublePlant.EnumBlockHalf.LOWER)) {
            TileEntity blockEntity = level.getTileEntity(pos);
            if (blockEntity instanceof TargetBlockEntity target && target.getOutputPower() > 0 && !level.isUpdateScheduled(pos, this)) {
                target.setOutputPower(0);
            }
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
