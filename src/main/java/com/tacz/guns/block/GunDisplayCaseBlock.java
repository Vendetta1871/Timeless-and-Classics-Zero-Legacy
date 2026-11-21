package com.tacz.guns.block;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.12.2: {@code FACING} and {@code LOCKED} are the metadata; the {@code LEFT} / {@code RIGHT} / {@code UP} /
 * {@code DOWN} connections are kept by the {@link GunDisplayCaseBlockEntity} (a sneak-placed case does not connect, so
 * they cannot be derived from the neighbours) and exposed through {@link #getActualState}. Code that reads a
 * connection goes through {@link #getConnectedState}.
 */
public class GunDisplayCaseBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool LOCKED = PropertyBool.create("locked");
    public static final PropertyBool LEFT = PropertyBool.create("left");
    public static final PropertyBool RIGHT = PropertyBool.create("right");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");
    private static final int MAX_GROUP_TOGGLE = 512;
    /**
     * {@code Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS}: 1.12.2 send-to-clients (2) without observers (16).
     */
    private static final int LOCK_UPDATE_FLAGS = 2 | 16;
    /**
     * {@code MapColor.METAL} without {@code requiresCorrectToolForDrops}: 1.12.2's {@code Material.IRON} needs a pickaxe
     * to drop anything.
     */
    private static final Material MATERIAL = new Material(MapColor.IRON);

    private static final AxisAlignedBB NORTH_LOCKED_SHAPE = box(0, 0, 4, 16, 16, 16);
    private static final AxisAlignedBB SOUTH_LOCKED_SHAPE = box(0, 0, 0, 16, 16, 12);
    private static final AxisAlignedBB WEST_LOCKED_SHAPE = box(4, 0, 0, 16, 16, 16);
    private static final AxisAlignedBB EAST_LOCKED_SHAPE = box(0, 0, 0, 12, 16, 16);
    private static final AxisAlignedBB NORTH_BACK_PANEL_SHAPE = box(0, 0, 13, 16, 16, 16);
    private static final AxisAlignedBB SOUTH_BACK_PANEL_SHAPE = box(0, 0, 0, 16, 16, 3);
    private static final AxisAlignedBB WEST_BACK_PANEL_SHAPE = box(13, 0, 0, 16, 16, 16);
    private static final AxisAlignedBB EAST_BACK_PANEL_SHAPE = box(0, 0, 0, 3, 16, 16);
    // 1.12.2: a VoxelShape is a list of boxes
    private static final List<AxisAlignedBB> NORTH_OPEN_SHAPE = Collections.unmodifiableList(Arrays.asList(
            NORTH_BACK_PANEL_SHAPE,
            box(0, 0, 4, 1, 16, 16),
            box(15, 0, 4, 16, 16, 16),
            box(0, 0, 4, 16, 1, 16),
            box(0, 15, 4, 16, 16, 16)
    ));
    private static final List<AxisAlignedBB> SOUTH_OPEN_SHAPE = Collections.unmodifiableList(Arrays.asList(
            SOUTH_BACK_PANEL_SHAPE,
            box(0, 0, 0, 1, 16, 12),
            box(15, 0, 0, 16, 16, 12),
            box(0, 0, 0, 16, 1, 12),
            box(0, 15, 0, 16, 16, 12)
    ));
    private static final List<AxisAlignedBB> WEST_OPEN_SHAPE = Collections.unmodifiableList(Arrays.asList(
            WEST_BACK_PANEL_SHAPE,
            box(4, 0, 0, 16, 16, 1),
            box(4, 0, 15, 16, 16, 16),
            box(4, 0, 0, 16, 1, 16),
            box(4, 15, 0, 16, 16, 16)
    ));
    private static final List<AxisAlignedBB> EAST_OPEN_SHAPE = Collections.unmodifiableList(Arrays.asList(
            EAST_BACK_PANEL_SHAPE,
            box(0, 0, 0, 12, 16, 1),
            box(0, 0, 15, 12, 16, 16),
            box(0, 0, 0, 12, 1, 16),
            box(0, 15, 0, 12, 16, 16)
    ));

    public GunDisplayCaseBlock() {
        super(MATERIAL);
        this.setSoundType(SoundType.GLASS);
        // strength(0.5F, 1200.0F): 1.12.2 multiplies the explosion resistance by 3 in setResistance
        this.setHardness(0.5F);
        this.setResistance(400.0F);
        // upstream's minecraft:mineable/pickaxe tag
        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(LOCKED, true)
                .withProperty(LEFT, false)
                .withProperty(RIGHT, false)
                .withProperty(UP, false)
                .withProperty(DOWN, false));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LOCKED, LEFT, RIGHT, UP, DOWN);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(LOCKED, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LOCKED) ? 4 : 0);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess level, BlockPos pos) {
        TileEntity blockEntity = level instanceof ChunkCache cache ? cache.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : level.getTileEntity(pos);
        if (blockEntity instanceof GunDisplayCaseBlockEntity displayCase) {
            return displayCase.withConnections(state);
        }
        return state.withProperty(LEFT, false).withProperty(RIGHT, false).withProperty(UP, false).withProperty(DOWN, false);
    }

    /**
     * The state at {@code pos} with its connections (upstream reads them straight from the block state).
     */
    private static IBlockState getConnectedState(IBlockAccess level, BlockPos pos) {
        IBlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof GunDisplayCaseBlock ? state.getActualState(level, pos) : state;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new GunDisplayCaseBlockEntity();
    }

    @Override
    public IBlockState getStateForPlacement(World level, BlockPos pos, EnumFacing clickedFace, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        BlockPos clickedPos = pos.offset(clickedFace.getOpposite());
        IBlockState clickedState = getConnectedState(level, clickedPos);
        boolean clickedDisplayCase = clickedState.getBlock() instanceof GunDisplayCaseBlock;
        EnumFacing facing = getPlacementFacing(placer, clickedFace, clickedState, clickedDisplayCase);
        IBlockState state = getDefaultState().withProperty(FACING, facing);
        if (placer.isSneaking()) {
            return state;
        }
        Set<BlockPos> forceConnectedPositions = clickedDisplayCase ? collectConnectedCases(level, clickedPos, facing) : null;
        return updateConnections(level, pos, state, forceConnectedPositions);
    }

    /**
     * Upstream's {@code updateShape}: a connection is dropped when the neighbour no longer connects (server side; the
     * block entity syncs it).
     */
    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (level.isRemote) {
            return;
        }
        EnumFacing direction = EnumFacing.getFacingFromVector(fromPos.getX() - pos.getX(), fromPos.getY() - pos.getY(), fromPos.getZ() - pos.getZ());
        if (!pos.offset(direction).equals(fromPos)) {
            return;
        }
        PropertyBool property = propertyForDirection(state.getValue(FACING), direction);
        if (property != null && level.getTileEntity(pos) instanceof GunDisplayCaseBlockEntity displayCase
                && displayCase.isConnected(property) && !connectsTo(state, level.getBlockState(fromPos))) {
            displayCase.setConnected(property, false);
        }
    }

    /**
     * Upstream's {@code onPlace}. 1.12.2: {@code state} is the exact state {@code getStateForPlacement} returned, its
     * connections go to the block entity first.
     */
    @Override
    public void onBlockAdded(World level, BlockPos pos, IBlockState state) {
        super.onBlockAdded(level, pos, state);
        if (!level.isRemote && level.getTileEntity(pos) instanceof GunDisplayCaseBlockEntity displayCase) {
            displayCase.setConnectionsFrom(state);
            syncConnectedNeighbors(level, pos, displayCase.withConnections(state));
        }
    }

    @Override
    public boolean onBlockActivated(World level, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        boolean holdingGun = held.getItem() instanceof IGun;
        if (player.isSneaking()) {
            if (player.getHeldItemMainhand().getItem() instanceof IGun) {
                return false;
            }
            toggleLockFromInteraction(level, pos, state);
            return true;
        }
        if (state.getValue(LOCKED)) {
            return held.isEmpty() || holdingGun;
        }

        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ));
        if (slot < 0) {
            return false;
        }
        TileEntity blockEntity = level.getTileEntity(pos);
        if (!(blockEntity instanceof GunDisplayCaseBlockEntity displayCase)) {
            return false;
        }

        ItemStack stored = displayCase.getItem(slot);
        if (level.isRemote) {
            // upstream: SUCCESS on the client; 1.12.2 would then skip the item use, which upstream does on PASS
            return held.isEmpty() || !stored.isEmpty() || GunPegboardItemConfigRead.canPlace(held);
        }

        if (stored.isEmpty()) {
            if (!held.isEmpty() && GunPegboardItemConfigRead.canPlace(held) && displayCase.setItem(slot, held)) {
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return true;
            }
            return held.isEmpty();
        }

        if (displayCase.rotateItem(slot)) {
            level.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    /**
     * Upstream's {@code getShape} (outline and ray trace).
     */
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess level, BlockPos pos) {
        if (state.getValue(LOCKED)) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH_LOCKED_SHAPE;
                case WEST -> WEST_LOCKED_SHAPE;
                case EAST -> EAST_LOCKED_SHAPE;
                default -> NORTH_LOCKED_SHAPE;
            };
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_BACK_PANEL_SHAPE;
            case WEST -> WEST_BACK_PANEL_SHAPE;
            case EAST -> EAST_BACK_PANEL_SHAPE;
            default -> NORTH_BACK_PANEL_SHAPE;
        };
    }

    /**
     * Upstream's {@code getCollisionShape}.
     */
    public List<AxisAlignedBB> getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos) {
        if (state.getValue(LOCKED)) {
            return Collections.singletonList(getBoundingBox(state, level, pos));
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_OPEN_SHAPE;
            case WEST -> WEST_OPEN_SHAPE;
            case EAST -> EAST_OPEN_SHAPE;
            default -> NORTH_OPEN_SHAPE;
        };
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World level, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        for (AxisAlignedBB box : getCollisionShape(state, level, pos)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
        }
    }

    @Override
    public void breakBlock(World level, BlockPos pos, IBlockState state) {
        TileEntity blockEntity = level.getTileEntity(pos);
        if (blockEntity instanceof GunDisplayCaseBlockEntity displayCase) {
            displayCase.dropAllItems();
        }
        super.breakBlock(level, pos, state);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /**
     * Upstream {@code ItemBlockRenderTypes.setRenderLayer(GUN_DISPLAY_CASE, RenderType.cutout())}.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
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

    private IBlockState updateConnections(IBlockAccess level, BlockPos pos, IBlockState state, @Nullable Set<BlockPos> forceConnectedPositions) {
        EnumFacing facing = state.getValue(FACING);
        return state
                .withProperty(LEFT, shouldAutoConnectTo(level, pos, state, leftOf(facing), forceConnectedPositions))
                .withProperty(RIGHT, shouldAutoConnectTo(level, pos, state, rightOf(facing), forceConnectedPositions))
                .withProperty(UP, shouldAutoConnectTo(level, pos, state, EnumFacing.UP, forceConnectedPositions))
                .withProperty(DOWN, shouldAutoConnectTo(level, pos, state, EnumFacing.DOWN, forceConnectedPositions));
    }

    private static EnumFacing getPlacementFacing(EntityLivingBase placer, EnumFacing clickedFace, IBlockState clickedState, boolean clickedDisplayCase) {
        if (clickedDisplayCase) {
            return clickedState.getValue(FACING);
        }
        return clickedFace.getAxis() == EnumFacing.Axis.Y ? placer.getHorizontalFacing().getOpposite() : clickedFace;
    }

    private static boolean shouldAutoConnectTo(IBlockAccess level, BlockPos pos, IBlockState state, EnumFacing direction, @Nullable Set<BlockPos> forceConnectedPositions) {
        BlockPos neighborPos = pos.offset(direction);
        IBlockState neighborState = getConnectedState(level, neighborPos);
        if (!connectsTo(state, neighborState)) {
            return false;
        }
        if (forceConnectedPositions != null && forceConnectedPositions.contains(neighborPos)) {
            return true;
        }
        PropertyBool neighborProperty = propertyForDirection(neighborState.getValue(FACING), direction.getOpposite());
        return neighborProperty != null && neighborState.getValue(neighborProperty);
    }

    private static Set<BlockPos> collectConnectedCases(IBlockAccess level, BlockPos start, EnumFacing facing) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> checked = new HashSet<>();
        Set<BlockPos> connected = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && checked.size() < MAX_GROUP_TOGGLE) {
            BlockPos pos = queue.removeFirst();
            if (!checked.add(pos)) {
                continue;
            }
            IBlockState state = getConnectedState(level, pos);
            if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(FACING) != facing) {
                continue;
            }
            connected.add(pos);
            queueConnectedCases(queue, pos, state, facing);
        }
        return connected;
    }

    private static void syncConnectedNeighbors(World level, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        syncConnectedNeighbor(level, pos, state, leftOf(facing), LEFT);
        syncConnectedNeighbor(level, pos, state, rightOf(facing), RIGHT);
        syncConnectedNeighbor(level, pos, state, EnumFacing.UP, UP);
        syncConnectedNeighbor(level, pos, state, EnumFacing.DOWN, DOWN);
    }

    private static void syncConnectedNeighbor(World level, BlockPos pos, IBlockState state, EnumFacing direction, PropertyBool property) {
        if (!state.getValue(property)) {
            return;
        }
        BlockPos neighborPos = pos.offset(direction);
        IBlockState neighborState = getConnectedState(level, neighborPos);
        if (!connectsTo(state, neighborState)) {
            return;
        }
        PropertyBool neighborProperty = propertyForDirection(neighborState.getValue(FACING), direction.getOpposite());
        if (neighborProperty != null && !neighborState.getValue(neighborProperty)
                && level.getTileEntity(neighborPos) instanceof GunDisplayCaseBlockEntity neighbor) {
            neighbor.setConnected(neighborProperty, true);
        }
    }

    private static boolean connectsTo(IBlockState state, IBlockState neighbor) {
        return neighbor.getBlock() instanceof GunDisplayCaseBlock && neighbor.getValue(FACING) == state.getValue(FACING);
    }

    @Nullable
    private static PropertyBool propertyForDirection(EnumFacing facing, EnumFacing direction) {
        if (direction == EnumFacing.UP) {
            return UP;
        }
        if (direction == EnumFacing.DOWN) {
            return DOWN;
        }
        if (direction == leftOf(facing)) {
            return LEFT;
        }
        if (direction == rightOf(facing)) {
            return RIGHT;
        }
        return null;
    }

    private static EnumFacing leftOf(EnumFacing facing) {
        return facing.rotateYCCW();
    }

    private static EnumFacing rightOf(EnumFacing facing) {
        return facing.rotateY();
    }

    public void toggleLockFromInteraction(World level, BlockPos pos, IBlockState state) {
        if (level.isRemote) {
            return;
        }
        toggleLinkedCases(level, pos, !state.getValue(LOCKED));
        level.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.8F, state.getValue(LOCKED) ? 1.1F : 0.9F);
    }

    private void toggleLinkedCases(World level, BlockPos start, boolean locked) {
        IBlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof GunDisplayCaseBlock)) {
            return;
        }
        EnumFacing facing = startState.getValue(FACING);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < MAX_GROUP_TOGGLE) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            IBlockState state = getConnectedState(level, pos);
            if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(FACING) != facing) {
                continue;
            }
            if (state.getValue(LOCKED) != locked) {
                // shouldRefresh keeps the block entity (and the connections)
                level.setBlockState(pos, state.withProperty(LOCKED, locked), LOCK_UPDATE_FLAGS);
            }
            queueConnectedCases(queue, pos, state, facing);
        }
    }

    private static void queueConnectedCases(ArrayDeque<BlockPos> queue, BlockPos pos, IBlockState state, EnumFacing facing) {
        if (state.getValue(LEFT)) {
            queue.add(pos.offset(leftOf(facing)));
        }
        if (state.getValue(RIGHT)) {
            queue.add(pos.offset(rightOf(facing)));
        }
        if (state.getValue(UP)) {
            queue.add(pos.up());
        }
        if (state.getValue(DOWN)) {
            queue.add(pos.down());
        }
    }
}
