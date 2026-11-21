package com.tacz.guns.block.entity;

import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.PegboardShape;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 1.12.2: block states have at most 16 metadata values, so upstream's {@code SHAPE} block state property is kept here
 * ({@link #getShape}), synced to the client and exposed through {@link GunPegboardBlock#getActualState}.
 * {@link #getBlockState()} returns the block state with that shape, like upstream's.
 */
public class GunPegboardBlockEntity extends TileEntity implements GunDisplayBlockEntity {
    public static final int GRID_SIZE = 4;
    public static final int SLOTS_PER_FACE = GRID_SIZE * GRID_SIZE;
    // 原有 0..15 槽位保持不变，16..31 属于折面；转角时每面仅启用三列。
    public static final int SLOT_COUNT = SLOTS_PER_FACE * 2;
    private static final String ITEMS_TAG = "Items";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String ROTATIONS_TAG = "Rotations";
    private static final String SLOT_SHAPE_TAG = "SlotShape";
    private static final String COLOR_TAG = "Color";
    private static final String INHERITED_COLOR_TAG = "InheritedColor";

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final byte[] rotations = new byte[SLOT_COUNT];
    private int color = -1;
    private boolean inheritedColor;
    private PegboardShape shape = PegboardShape.STRAIGHT;
    private final DisplayGunSlot[] displaySlots = new DisplayGunSlot[SLOT_COUNT];

    public GunPegboardBlockEntity() {
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    /**
     * {@code BlockEntity#getBlockState()}, with the {@code SHAPE} kept by this block entity.
     */
    public IBlockState getBlockState() {
        IBlockState state = world != null ? world.getBlockState(pos) : null;
        if (state == null || !(state.getBlock() instanceof GunPegboardBlock)) {
            state = ModBlocks.GUN_PEGBOARD.get().getDefaultState();
        }
        return state.withProperty(GunPegboardBlock.SHAPE, shape);
    }

    public PegboardShape getShape() {
        return shape;
    }

    /**
     * Upstream {@code setBlockState} with a new {@code SHAPE}: items of slots that are no longer active drop, the
     * rendered slots are refreshed. Called by the block on the server; the client gets the shape with the update packet.
     */
    public void setShape(PegboardShape shape) {
        if (this.shape == shape) {
            return;
        }
        IBlockState previous = getBlockState();
        this.shape = shape;
        IBlockState state = getBlockState();
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (displaySlots[slot] != null) displaySlots[slot].transformed();
            boolean removedFace = slot >= SLOTS_PER_FACE;
            if (world != null && !world.isRemote && !items[slot].isEmpty()
                    && (!GunPegboardSlotHelper.isSlotActive(state, slot) || removedFace)) {
                Block.spawnAsEntity(world, pos.offset(GunPegboardSlotHelper.getSlotFacing(previous, slot)), items[slot]);
                items[slot] = ItemStack.EMPTY;
                rotations[slot] = 0;
                displaySlotChanged(slot);
                changed = true;
            }
        }
        if (world != null && !world.isRemote) {
            update();
        } else if (world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public ItemStack getItem(int slot) {
        return GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) ? items[slot] : ItemStack.EMPTY;
    }

    public int getColor() {
        return color;
    }

    public boolean hasInheritedColor() {
        return inheritedColor;
    }

    public void setColor(int color, boolean inherited) {
        int normalized = color < 0 ? -1 : color & 0xFFFFFF;
        boolean inheritedColor = normalized >= 0 && inherited;
        if (this.color == normalized && this.inheritedColor == inheritedColor) return;
        this.color = normalized;
        this.inheritedColor = inheritedColor;
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof GunPegboardBlock) {
                // shouldRefresh keeps this block entity
                world.setBlockState(pos, state.withProperty(GunPegboardBlock.DYED, this.color >= 0), 3);
            }
        }
        update();
    }

    @Override
    public int getRotation(int slot) {
        return isValidSlot(slot) ? rotations[slot] & 7 : 0;
    }

    @Override
    public DisplayGunSlot getDisplaySlot(int slot) {
        if (!isValidSlot(slot)) throw new IndexOutOfBoundsException(String.valueOf(slot));
        DisplayGunSlot handle = displaySlots[slot];
        if (handle == null) displaySlots[slot] = handle = new DisplayGunSlot(this, slot);
        return handle;
    }

    @Override
    public int getGridSize() {
        return GRID_SIZE;
    }

    @Override
    public EnumFacing getSlotFacing(int slot) {
        return GunPegboardSlotHelper.getSlotFacing(getBlockState(), slot);
    }

    @Override
    public boolean isDisplayGlowing() {
        return getBlockState().getBlock() instanceof GunPegboardBlock pegboard && pegboard.isGlowing();
    }

    @Override
    public TileEntity getBlockEntity() {
        return this;
    }

    private void displaySlotChanged(int slot) {
        if (displaySlots[slot] != null) displaySlots[slot].changed();
    }

    public boolean setItem(int slot, ItemStack stack) {
        if (!GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) || !items[slot].isEmpty()) {
            return false;
        }
        ItemStack stored = stack.copy();
        stored.setCount(1);
        items[slot] = stored;
        rotations[slot] = 0;
        displaySlotChanged(slot);
        update();
        return true;
    }

    /** Fill empty slots in one update when creating a display wall. */
    public boolean setItems(Map<Integer, ItemStack> stacks) {
        if (stacks.isEmpty() || stacks.size() > SLOT_COUNT) {
            return false;
        }
        IBlockState state = getBlockState();
        for (Map.Entry<Integer, ItemStack> entry : stacks.entrySet()) {
            int slot = entry.getKey();
            if (!GunPegboardSlotHelper.isSlotActive(state, slot) || entry.getValue().isEmpty() || !items[slot].isEmpty()) {
                return false;
            }
        }
        for (Map.Entry<Integer, ItemStack> entry : stacks.entrySet()) {
            int slot = entry.getKey();
            ItemStack stored = entry.getValue().copy();
            stored.setCount(1);
            items[slot] = stored;
            rotations[slot] = 0;
            displaySlotChanged(slot);
        }
        update();
        return true;
    }

    public boolean rotateItem(int slot) {
        if (!GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) || items[slot].isEmpty()) {
            return false;
        }
        rotations[slot] = (byte) ((rotations[slot] + 1) & 7);
        if (displaySlots[slot] != null) displaySlots[slot].transformed();
        update();
        return true;
    }

    public boolean dropItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty() || world == null) {
            return false;
        }
        EnumFacing facing = GunPegboardSlotHelper.getSlotFacing(getBlockState(), slot);
        Block.spawnAsEntity(world, pos.offset(facing), items[slot]);
        items[slot] = ItemStack.EMPTY;
        rotations[slot] = 0;
        displaySlotChanged(slot);
        update();
        return true;
    }

    public boolean removeItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty()) {
            return false;
        }
        items[slot] = ItemStack.EMPTY;
        rotations[slot] = 0;
        displaySlotChanged(slot);
        update();
        return true;
    }

    public void dropAllItems() {
        if (world == null) {
            return;
        }
        IBlockState state = getBlockState();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                Block.spawnAsEntity(world, pos.offset(GunPegboardSlotHelper.getSlotFacing(state, i)), items[i]);
                items[i] = ItemStack.EMPTY;
                rotations[i] = 0;
                displaySlotChanged(i);
            }
        }
        update();
    }

    private void update() {
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    /**
     * 1.12.2: the block entity outlives state changes of its own block (locking, dyeing); only a different block
     * replaces it.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /**
     * 结构镜像只转换方块状态；左右转角互换时，同步镜像每面的槽位列和旋转角度。
     * <p>
     * 1.12.2: upstream detects the mirror in {@code load} (saved {@code SlotShape} against the mirrored block state);
     * here the shape is this block entity's own, and structure templates call {@link #mirror} after loading it.
     */
    @Override
    public void mirror(Mirror mirror) {
        if (mirror == Mirror.NONE || shape == PegboardShape.STRAIGHT) {
            return;
        }
        shape = shape == PegboardShape.CORNER_LEFT ? PegboardShape.CORNER_RIGHT : PegboardShape.CORNER_LEFT;
        ItemStack[] savedItems = items.clone();
        byte[] savedRotations = rotations.clone();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int mirroredSlot = slot - slot % GRID_SIZE + GRID_SIZE - 1 - slot % GRID_SIZE;
            items[mirroredSlot] = savedItems[slot];
            rotations[mirroredSlot] = (byte) (-savedRotations[slot] & 7);
        }
        for (int i = 0; i < displaySlots.length; i++) {
            displaySlotChanged(i);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        PegboardShape previousShape = shape;
        int previousColor = color;
        color = tag.hasKey(COLOR_TAG, Constants.NBT.TAG_INT) ? tag.getInteger(COLOR_TAG) & 0xFFFFFF : -1;
        inheritedColor = color >= 0 && tag.getBoolean(INHERITED_COLOR_TAG);
        shape = PegboardShape.bySerializedName(tag.getString(SLOT_SHAPE_TAG));
        ItemStack[] previousItems = items.clone();
        byte[] previousRotations = rotations.clone();
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
            rotations[i] = 0;
        }
        if (tag.hasKey(ITEMS_TAG, Constants.NBT.TAG_LIST)) {
            NBTTagList list = tag.getTagList(ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                int slot = entry.getByte(SLOT_TAG) & 255;
                if (isValidSlot(slot) && entry.hasKey(ITEM_TAG, Constants.NBT.TAG_COMPOUND)) {
                    items[slot] = new ItemStack(entry.getCompoundTag(ITEM_TAG));
                }
            }
        }
        if (tag.hasKey(ROTATIONS_TAG, Constants.NBT.TAG_BYTE_ARRAY)) {
            byte[] saved = tag.getByteArray(ROTATIONS_TAG);
            System.arraycopy(saved, 0, rotations, 0, Math.min(saved.length, rotations.length));
        }
        for (int i = 0; i < displaySlots.length; i++) {
            DisplayGunSlot handle = displaySlots[i];
            if (handle == null) continue;
            if (shape != previousShape) handle.transformed();
            if (!ItemStack.areItemStacksEqual(previousItems[i], items[i])) handle.changed();
            else if (previousRotations[i] != rotations[i]) handle.transformed();
        }
        // the block model depends on the shape (actual state) and the tint on the color
        if (world != null && world.isRemote && (shape != previousShape || color != previousColor)) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString(SLOT_SHAPE_TAG, shape.getSerializedName());
        if (color >= 0) tag.setInteger(COLOR_TAG, color);
        if (inheritedColor) tag.setBoolean(INHERITED_COLOR_TAG, true);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte(SLOT_TAG, (byte) i);
                entry.setTag(ITEM_TAG, items[i].writeToNBT(new NBTTagCompound()));
                list.appendTag(entry);
            }
        }
        tag.setTag(ITEMS_TAG, list);
        tag.setByteArray(ROTATIONS_TAG, rotations);
        return tag;
    }

    /**
     * 1.12.2: unlike upstream the update tag keeps {@code SlotShape}, the shape is not a block state property here.
     */
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-2, -2, -2), pos.add(3, 3, 3));
    }

    /**
     * 1.12.2: upstream's block entity renderer view distance ({@code RenderConfig.TARGET_RENDER_DISTANCE}).
     */
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        double distance = RenderConfig.TARGET_RENDER_DISTANCE.get();
        return distance * distance;
    }
}
