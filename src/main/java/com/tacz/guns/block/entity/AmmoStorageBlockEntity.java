package com.tacz.guns.block.entity;

import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.inventory.AmmoStorageInventory;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.12.2: no {@code BlockEntityType} / {@code MenuProvider}; the tile entity is registered by class in
 * {@code ModBlocks#registerTileEntities} and the menu is opened through {@code ModContainer}'s gui handler, which calls
 * {@link #createMenu}.
 */
public class AmmoStorageBlockEntity extends TileEntity {
    public static final int MAX_AMMO_TYPES = 27;

    private final AmmoStorageInventory inventory = new AmmoStorageInventory(MAX_AMMO_TYPES, this::getCapacityStacks,
            AmmoStorageHelper::isValidAmmo, AmmoStorageHelper::getStackSize, this::onInventoryChanged);

    public AmmoStorageBlockEntity() {
    }

    /**
     * {@code BlockEntity#getBlockState()}
     */
    public IBlockState getBlockState() {
        return world.getBlockState(pos);
    }

    public int insertAmmo(ItemStack stack, boolean simulate) {
        return inventory.insert(stack, simulate);
    }

    public ItemStack extractAmmo(ItemStack sample, int count, boolean simulate) {
        return inventory.extract(sample, count, simulate);
    }

    public ItemStack extractAmmo(ResourceLocation ammoId, int count, boolean simulate) {
        for (AmmoStorageInventory.Content content : inventory.getContents()) {
            if (ammoId.equals(AmmoStorageHelper.getAmmoId(content.sample()))) {
                // Never combine differently tagged ammo into one returned ItemStack.
                return inventory.extract(content.sample(), count, simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    public int moveAmmoToInventory(IInventory target, int targetSlots, ItemStack sample, int count) {
        return inventory.moveToInventory(target, targetSlots, sample, count);
    }

    public List<AmmoStorageInventory.Content> getContents() {
        return inventory.getContents();
    }

    public long getInventoryRevision() {
        return inventory.getRevision();
    }

    public long getUsedStacks() {
        return inventory.getUsedStacks();
    }

    public long getRemainingCapacity(ItemStack sample) {
        return inventory.getRemainingCapacity(sample);
    }

    public int getComparatorSignal() {
        return inventory.getComparatorSignal();
    }

    public boolean stillValid(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        inventory.load(tag.getTagList("Entries", Constants.NBT.TAG_COMPOUND));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Entries", inventory.save());
        return tag;
    }

    /**
     * {@code BlockEntity#saveWithoutMetadata()}: the saved data without the id and position tags.
     */
    public NBTTagCompound saveWithoutMetadata() {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.removeTag("id");
        tag.removeTag("x");
        tag.removeTag("y");
        tag.removeTag("z");
        return tag;
    }

    @Override
    public ITextComponent getDisplayName() {
        return Components.translatable("gui.tacz.ammo_storage.title");
    }

    public Container createMenu(int id, InventoryPlayer inventory, EntityPlayer player) {
        return new AmmoStorageMenu(id, inventory, this);
    }

    // 1.12.2: capabilities are not cached in a LazyOptional, so there is nothing to invalidate/revive
    // (upstream's invalidateCaps/reviveCaps)

    @Override
    public boolean hasCapability(@Nonnull Capability<?> cap, @Nullable EnumFacing side) {
        if (!isInvalid() && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(cap, side);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing side) {
        if (!isInvalid() && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(cap, side);
    }

    private void onInventoryChanged() {
        markDirty();
        if (world != null && !world.isRemote) {
            world.updateComparatorOutputLevel(pos, getBlockState().getBlock());
        }
        // Contents are sent only to open menus, never through block-entity/chunk synchronization.
    }

    public int getCapacityStacks() {
        // 1.12.2: tile entities are read from NBT before they get their world
        return world != null && getBlockState().getBlock() instanceof AmmoStorageBlock storage
                ? storage.getCapacityStacks() : AmmoStorageBlock.IRON_CAPACITY_STACKS;
    }
}
