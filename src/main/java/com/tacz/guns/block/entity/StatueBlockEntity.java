package com.tacz.guns.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import static com.tacz.guns.block.StatueBlock.FACING;

public class StatueBlockEntity extends TileEntity implements ITickable {
    private static final String ITEM_TAG = "Item";
    private ItemStack gunItem = ItemStack.EMPTY;

    public StatueBlockEntity() {
    }

    /**
     * {@code BlockEntity#getBlockState()}
     */
    public IBlockState getBlockState() {
        return world.getBlockState(pos);
    }

    /**
     * 1.12.2: upstream's {@code BlockEntityTicker} ({@code StatueBlock#getTicker}, client side only) is the tile entity's
     * own tick.
     */
    @Override
    public void update() {
        if (world.isRemote) {
            clientTick(world, pos, getBlockState(), this);
        }
    }

    public static void clientTick(World level, BlockPos blockPos, IBlockState state, StatueBlockEntity statueBlockEntity) {
        if (level.getTotalWorldTime() % 100 == 0 && !statueBlockEntity.gunItem.isEmpty()) {
            EnumFacing direction = state.getValue(FACING);

            double x = blockPos.getX() + direction.getXOffset() * 0.75 + 0.5;
            double z = blockPos.getZ() + direction.getZOffset() * 0.75 + 0.5;

            double dx = -0.02 + level.rand.nextDouble() * 0.04;
            double dz = -0.02 + level.rand.nextDouble() * 0.04;
            double dy = -0.02 + level.rand.nextDouble() * 0.04;

            level.spawnParticle(EnumParticleTypes.END_ROD, x, blockPos.getY() + 2.25, z, dx, dy, dz);
        }
    }

    public ItemStack getGunItem() {
        return gunItem;
    }

    public void setGun(ItemStack stack) {
        this.dropItem();
        this.gunItem = stack.copy();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
        this.markDirty();
    }

    public void dropItem() {
        if (!gunItem.isEmpty() && world != null) {
            EnumFacing direction = getBlockState().getValue(FACING);
            Block.spawnAsEntity(world, pos.offset(direction).up(), gunItem);
            this.gunItem = ItemStack.EMPTY;
            if (world != null) {
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 3);
            }
            this.markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey(ITEM_TAG, Constants.NBT.TAG_COMPOUND)) {
            this.gunItem = new ItemStack(tag.getCompoundTag(ITEM_TAG));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag(ITEM_TAG, gunItem.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
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

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-2, 0, -2), pos.add(2, 2, 2));
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
}
