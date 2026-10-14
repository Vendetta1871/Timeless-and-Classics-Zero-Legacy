package com.tacz.guns.block.entity;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public class GunSmithTableBlockEntity extends TileEntity {
    private static final String ID_TAG = "BlockId";

    @Nullable
    private ResourceLocation id = null;

    public GunSmithTableBlockEntity() {
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    @Nullable
    public ResourceLocation getId() {
        return id;
    }

    /**
     * {@code BlockEntity#getBlockState()}
     */
    public IBlockState getBlockState() {
        return world.getBlockState(pos);
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
        return new AxisAlignedBB(pos.add(-2, 0, -2), pos.add(2, 1, 2));
    }

    public ITextComponent getDisplayName() {
        return new TextComponentString("Gun Smith Table");
    }

    @Nullable
    public Container createMenu(int id, InventoryPlayer inventory, EntityPlayer player) {
        return new GunSmithTableMenu(id, inventory, getId());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey(ID_TAG, Constants.NBT.TAG_STRING)) {
            this.id = new ResourceLocation(tag.getString(ID_TAG));
        } else {
            this.id = DefaultAssets.DEFAULT_BLOCK_ID;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (id != null) {
            tag.setString(ID_TAG, id.toString());
        }
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }
}
