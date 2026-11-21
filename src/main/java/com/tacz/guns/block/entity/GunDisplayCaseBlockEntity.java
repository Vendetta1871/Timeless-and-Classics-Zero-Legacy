package com.tacz.guns.block.entity;

import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.12.2: block states have at most 16 metadata values, so upstream's {@code LEFT} / {@code RIGHT} / {@code UP} /
 * {@code DOWN} connection properties are kept here ({@link #isConnected}), synced to the client and exposed through
 * {@link GunDisplayCaseBlock#getActualState}.
 */
public class GunDisplayCaseBlockEntity extends TileEntity implements GunDisplayBlockEntity {
    public static final int GRID_SIZE = 4;
    public static final int SLOT_COUNT = GRID_SIZE * GRID_SIZE;
    private static final String ITEMS_TAG = "Items";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String ROTATIONS_TAG = "Rotations";
    private static final String CONNECTIONS_TAG = "Connections";

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final byte[] rotations = new byte[SLOT_COUNT];
    private final DisplayGunSlot[] displaySlots = new DisplayGunSlot[SLOT_COUNT];
    /**
     * Bit mask of the connections, see {@link #connectionBit}.
     */
    private int connections;

    public GunDisplayCaseBlockEntity() {
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    /**
     * {@code BlockEntity#getBlockState()}, with the connections kept by this block entity.
     */
    public IBlockState getBlockState() {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof GunDisplayCaseBlock ? withConnections(state) : state;
    }

    private static int connectionBit(PropertyBool property) {
        if (property == GunDisplayCaseBlock.LEFT) return 1;
        if (property == GunDisplayCaseBlock.RIGHT) return 2;
        if (property == GunDisplayCaseBlock.UP) return 4;
        return 8;
    }

    public boolean isConnected(PropertyBool property) {
        return (connections & connectionBit(property)) != 0;
    }

    public IBlockState withConnections(IBlockState state) {
        return state.withProperty(GunDisplayCaseBlock.LEFT, isConnected(GunDisplayCaseBlock.LEFT))
                .withProperty(GunDisplayCaseBlock.RIGHT, isConnected(GunDisplayCaseBlock.RIGHT))
                .withProperty(GunDisplayCaseBlock.UP, isConnected(GunDisplayCaseBlock.UP))
                .withProperty(GunDisplayCaseBlock.DOWN, isConnected(GunDisplayCaseBlock.DOWN));
    }

    /**
     * Upstream {@code level.setBlock(pos, state.setValue(property, connected), ...)} for a connection property.
     */
    public void setConnected(PropertyBool property, boolean connected) {
        int updated = connected ? connections | connectionBit(property) : connections & ~connectionBit(property);
        if (updated != connections) {
            connections = updated;
            update();
        }
    }

    /**
     * Takes the connections of the placement state ({@code getStateForPlacement} returns them, {@code onBlockAdded}
     * receives the same object).
     */
    public void setConnectionsFrom(IBlockState state) {
        int updated = 0;
        for (PropertyBool property : new PropertyBool[]{GunDisplayCaseBlock.LEFT, GunDisplayCaseBlock.RIGHT, GunDisplayCaseBlock.UP, GunDisplayCaseBlock.DOWN}) {
            if (state.getValue(property)) {
                updated |= connectionBit(property);
            }
        }
        if (updated != connections) {
            connections = updated;
            update();
        }
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? items[slot] : ItemStack.EMPTY;
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
        return getFacing();
    }

    private EnumFacing getFacing() {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof GunDisplayCaseBlock ? state.getValue(GunDisplayCaseBlock.FACING) : EnumFacing.NORTH;
    }

    @Override
    public boolean isDisplayGlowing() {
        return true;
    }

    @Override
    public TileEntity getBlockEntity() {
        return this;
    }

    private void displaySlotChanged(int slot) {
        if (displaySlots[slot] != null) displaySlots[slot].changed();
    }

    public boolean setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot) || !items[slot].isEmpty()) {
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

    public boolean rotateItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty()) {
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
        Block.spawnAsEntity(world, pos.offset(getFacing()), items[slot]);
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
        EnumFacing facing = getFacing();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                Block.spawnAsEntity(world, pos.offset(facing), items[i]);
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
     * 1.12.2: the block entity outlives state changes of its own block (locking); only a different block replaces it.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        int previousConnections = connections;
        connections = tag.getByte(CONNECTIONS_TAG) & 15;
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
            if (!ItemStack.areItemStacksEqual(previousItems[i], items[i])) handle.changed();
            else if (previousRotations[i] != rotations[i]) handle.transformed();
        }
        // the block model (glass and frame pieces) depends on the connections
        if (world != null && world.isRemote && connections != previousConnections) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
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
        tag.setByte(CONNECTIONS_TAG, (byte) connections);
        return tag;
    }

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
