package com.tacz.guns.block;

import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import com.tacz.guns.item.GunPegboardItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 1.12.2: {@code FACING} and {@code DYED} are the metadata; {@code SHAPE} is kept by the {@link GunPegboardBlockEntity}
 * (recomputed on the server in {@link #onBlockAdded} and {@link #neighborChanged}, upstream's {@code updateShape}) and
 * exposed through {@link #getActualState}. Upstream's {@code DyeableLeatherItem} color of the block item is the
 * {@code display.color} tag ({@link GunPegboardItem}).
 */
public class GunPegboardBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool DYED = PropertyBool.create("dyed");
    public static final PropertyEnum<PegboardShape> SHAPE = PropertyEnum.create("shape", PegboardShape.class);
    /**
     * {@code MapColor.METAL} without {@code requiresCorrectToolForDrops}: 1.12.2's {@code Material.IRON} needs a pickaxe
     * to drop anything.
     */
    private static final Material MATERIAL = new Material(MapColor.IRON);
    private static final AxisAlignedBB NORTH_SHAPE = box(0, 0, 13, 16, 16, 16);
    private static final AxisAlignedBB SOUTH_SHAPE = box(0, 0, 0, 16, 16, 3);
    private static final AxisAlignedBB WEST_SHAPE = box(13, 0, 0, 16, 16, 16);
    private static final AxisAlignedBB EAST_SHAPE = box(0, 0, 0, 3, 16, 16);
    private final boolean glowing;

    public GunPegboardBlock() {
        this(false);
    }

    public GunPegboardBlock(boolean glowing) {
        super(MATERIAL);
        this.setSoundType(SoundType.METAL);
        // strength(5.0F, 6.0F): 1.12.2 multiplies the explosion resistance by 3 in setResistance
        this.setHardness(5.0F);
        this.setResistance(2.0F);
        this.setLightLevel(glowing ? 10 / 15.0F : 0.0F);
        // upstream's minecraft:mineable/pickaxe tag
        this.setHarvestLevel("pickaxe", 0);
        this.glowing = glowing;
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(DYED, false).withProperty(SHAPE, PegboardShape.STRAIGHT));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    public boolean isGlowing() {
        return glowing;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, DYED, SHAPE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(DYED, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(DYED) ? 4 : 0);
    }

    /**
     * The block entity without creating it: {@code getActualState} and the block colors run on the chunk render
     * threads with a {@link ChunkCache}.
     */
    @Nullable
    public static TileEntity getBlockEntity(IBlockAccess level, BlockPos pos) {
        return level instanceof ChunkCache cache ? cache.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : level.getTileEntity(pos);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess level, BlockPos pos) {
        PegboardShape shape = getBlockEntity(level, pos) instanceof GunPegboardBlockEntity pegboard ? pegboard.getShape() : PegboardShape.STRAIGHT;
        return state.withProperty(SHAPE, shape);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new GunPegboardBlockEntity();
    }

    /**
     * {@code context.replacingClickedOnBlock()}: 1.12.2 only tells the placement position, the clicked block was
     * replaced when the position still holds a replaceable block other than air.
     */
    static boolean replacingClickedOnBlock(World level, BlockPos pos) {
        return !level.isAirBlock(pos) && level.getBlockState(pos).getBlock().isReplaceable(level, pos);
    }

    @Override
    public IBlockState getStateForPlacement(World level, BlockPos pos, EnumFacing clickedFace, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState state;
        BlockPos target = pos.offset(clickedFace.getOpposite());
        IBlockState targetState = level.getBlockState(target);
        if (!replacingClickedOnBlock(level, pos) && targetState.getBlock() instanceof GunPegboardBlock) {
            targetState = targetState.getActualState(level, target);
            EnumFacing targetFacing = targetState.getValue(FACING);
            if (GunPegboardSlotHelper.hasCorner(targetState)
                    && clickedFace == targetState.getValue(SHAPE).getCornerFacing(targetFacing)) {
                targetFacing = clickedFace;
            }
            EnumFacing playerFacing = placer.getHorizontalFacing();
            // 水平视线正对板面时延续朝向，沿板面看时按玩家朝向放置垂直板。
            EnumFacing placedFacing = playerFacing.getAxis() == targetFacing.getAxis()
                    ? targetFacing : playerFacing.getOpposite();
            state = getDefaultState().withProperty(FACING, placedFacing);
        } else if (clickedFace.getAxis() == EnumFacing.Axis.Y) {
            state = getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        } else {
            state = getDefaultState().withProperty(FACING, clickedFace);
        }
        // 1.12.2: the shape is set by onBlockAdded, once the block entity exists
        return state;
    }

    private PegboardShape getConnectedShape(IBlockState state, IBlockAccess level, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        IBlockState front = level.getBlockState(pos.offset(facing));
        if (front.getBlock() instanceof GunPegboardBlock) {
            EnumFacing otherFacing = front.getValue(FACING);
            // 折面位于其朝向的反侧；该边已有同朝向直板时不能转角。
            if (otherFacing == facing.rotateYCCW()
                    && !hasStraightConnection(level, pos.offset(facing.rotateY()), facing)) {
                return PegboardShape.CORNER_LEFT;
            }
            if (otherFacing == facing.rotateY()
                    && !hasStraightConnection(level, pos.offset(facing.rotateYCCW()), facing)) {
                return PegboardShape.CORNER_RIGHT;
            }
        }
        return PegboardShape.STRAIGHT;
    }

    private static boolean hasStraightConnection(IBlockAccess level, BlockPos pos, EnumFacing facing) {
        IBlockState neighbor = level.getBlockState(pos);
        return neighbor.getBlock() instanceof GunPegboardBlock && neighbor.getValue(FACING) == facing;
    }

    /**
     * Upstream's {@code updateShape} on the server; the block entity drops the items of the slots the new shape loses
     * and syncs the shape.
     */
    private void updateShape(World level, BlockPos pos, IBlockState state) {
        if (level.isRemote) {
            return;
        }
        if (level.getTileEntity(pos) instanceof GunPegboardBlockEntity pegboard) {
            pegboard.setShape(getConnectedShape(state, level, pos));
        }
    }

    @Override
    public void onBlockAdded(World level, BlockPos pos, IBlockState state) {
        super.onBlockAdded(level, pos, state);
        updateShape(level, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos) {
        updateShape(level, pos, state);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * Upstream also swaps {@code CORNER_LEFT} / {@code CORNER_RIGHT}; 1.12.2 structure templates call
     * {@link GunPegboardBlockEntity#mirror} for that.
     */
    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public boolean onBlockActivated(World level, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // TODO(port): upstream passes for Items.DEBUG_STICK, which 1.12.2 does not have
        if (player.getHeldItem(hand).getItem() instanceof GunPegboardItem) {
            return false;
        }
        if (player.isSneaking()) {
            return false;
        }
        state = state.getActualState(level, pos);
        int slot = GunPegboardSlotHelper.getSlot(state, pos, new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ));
        if (slot < 0) {
            return false;
        }
        TileEntity blockEntity = level.getTileEntity(pos);
        if (!(blockEntity instanceof GunPegboardBlockEntity pegboard)) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);
        ItemStack stored = pegboard.getItem(slot);
        if (level.isRemote) {
            // upstream: SUCCESS on the client; 1.12.2 would then skip the item use, which upstream does on PASS
            return !stored.isEmpty() || !held.isEmpty() && GunPegboardItemConfigRead.canPlace(held);
        }

        if (stored.isEmpty()) {
            if (!held.isEmpty() && GunPegboardItemConfigRead.canPlace(held) && pegboard.setItem(slot, held)) {
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return true;
            }
            return false;
        }

        if (pegboard.rotateItem(slot)) {
            level.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    /**
     * Upstream's {@code getShape}: every plate of the (actual) state.
     */
    public List<AxisAlignedBB> getShape(IBlockState state) {
        AxisAlignedBB plate = getPlateShape(state.getValue(FACING));
        if (state.getValue(SHAPE) == PegboardShape.STRAIGHT) return Collections.singletonList(plate);
        return Arrays.asList(plate, getPlateShape(state.getValue(SHAPE).getCornerFacing(state.getValue(FACING))));
    }

    private static AxisAlignedBB getPlateShape(EnumFacing facing) {
        return switch (facing) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        AxisAlignedBB bounds = null;
        for (AxisAlignedBB box : getShape(state.getActualState(source, pos))) {
            bounds = bounds == null ? box : bounds.union(box);
        }
        return bounds;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World level, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        if (!isActualState) {
            state = state.getActualState(level, pos);
        }
        for (AxisAlignedBB box : getShape(state)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
        }
    }

    /**
     * Corner pegboards are hit plate by plate (the hit location decides the slot face), as {@link TargetBlock} does.
     */
    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World level, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (AxisAlignedBB box : getShape(state.getActualState(level, pos))) {
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
    public void breakBlock(World level, BlockPos pos, IBlockState state) {
        TileEntity blockEntity = level.getTileEntity(pos);
        if (blockEntity instanceof GunPegboardBlockEntity pegboard) {
            pegboard.dropAllItems();
        }
        super.breakBlock(level, pos, state);
    }

    /**
     * Keeps the block (and its block entity) until {@link #harvestBlock} computed the drops, as {@code BlockFlowerPot}
     * does: the dropped pegboard carries the block entity's color.
     */
    @Override
    public boolean removedByPlayer(IBlockState state, World level, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        return super.removedByPlayer(state, level, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World level, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(level, player, pos, state, te, tool);
        level.setBlockToAir(pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess level, BlockPos pos, IBlockState state, int fortune) {
        ItemStack drop = new ItemStack(this);
        if (level.getTileEntity(pos) instanceof GunPegboardBlockEntity pegboard && pegboard.getColor() >= 0 && !pegboard.hasInheritedColor()
                && drop.getItem() instanceof GunPegboardItem item) {
            item.setColor(drop, pegboard.getColor());
        }
        drops.add(drop);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World level, BlockPos pos, EntityPlayer player) {
        ItemStack stack = new ItemStack(this);
        if (level.getTileEntity(pos) instanceof GunPegboardBlockEntity pegboard && pegboard.getColor() >= 0
                && stack.getItem() instanceof GunPegboardItem item) {
            item.setColor(stack, pegboard.getColor());
        }
        return stack;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess level, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }
}
