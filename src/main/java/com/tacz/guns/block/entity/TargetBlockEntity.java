package com.tacz.guns.block.entity;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import static com.tacz.guns.block.TargetBlock.STAND;

public class TargetBlockEntity extends TileEntity implements IWorldNameable, ITickable {
    /**
     * 标靶复位时间，暂定为 5 秒
     */
    private static final int RESET_TIME = 5 * 20;
    private static final String OWNER_TAG = "Owner";
    private static final String CUSTOM_NAME_TAG = "CustomName";
    private static final String OUTPUT_POWER_TAG = "OutputPower";
    public float rot = 0;
    public float oRot = 0;
    private @Nullable GameProfile owner;
    private @Nullable ITextComponent name;
    /**
     * 1.12.2: block states have at most 16 variants, so upstream's {@code OUTPUT_POWER} block state property (0 - 15)
     * lives here, on the lower half. {@link TargetBlock#getOutputPower} reads it for both halves.
     */
    private int outputPower = 0;

    public TargetBlockEntity() {
    }

    /**
     * {@code BlockEntity#getBlockState()}
     */
    public IBlockState getBlockState() {
        return world.getBlockState(pos);
    }

    /**
     * 1.12.2: upstream's {@code BlockEntityTicker} ({@code TargetBlock#getTicker}, client side only) is the tile
     * entity's own tick.
     */
    @Override
    public void update() {
        if (world.isRemote) {
            clientTick(world, pos, getBlockState(), this);
        }
    }

    public static void clientTick(World level, BlockPos pos, IBlockState state, TargetBlockEntity pBlockEntity) {
        pBlockEntity.oRot = pBlockEntity.rot;
        if (state.getValue(STAND)) {
            pBlockEntity.rot = Math.max(pBlockEntity.rot - 18, 0);
        } else {
            pBlockEntity.rot = Math.min(pBlockEntity.rot + 45, 90);
        }
    }

    public int getOutputPower() {
        return outputPower;
    }

    public void setOutputPower(int outputPower) {
        this.outputPower = outputPower;
    }

    @Nullable
    public GameProfile getOwner() {
        return owner;
    }

    public void setOwner(@Nullable GameProfile owner) {
        // 1.12.2: fills the profile (textures) from the profile cache / session server, synchronously
        this.owner = TileEntitySkull.updateGameprofile(owner);
        this.refresh();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey(OWNER_TAG, Constants.NBT.TAG_COMPOUND)) {
            this.owner = NBTUtil.readGameProfileFromNBT(tag.getCompoundTag(OWNER_TAG));
        }
        if (tag.hasKey(CUSTOM_NAME_TAG, Constants.NBT.TAG_STRING)) {
            this.name = ITextComponent.Serializer.jsonToComponent(tag.getString(CUSTOM_NAME_TAG));
        }
        this.outputPower = tag.getInteger(OUTPUT_POWER_TAG);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (owner != null) {
            tag.setTag(OWNER_TAG, NBTUtil.writeGameProfile(new NBTTagCompound(), owner));
        }
        if (this.name != null) {
            tag.setString(CUSTOM_NAME_TAG, ITextComponent.Serializer.componentToJson(this.name));
        }
        tag.setInteger(OUTPUT_POWER_TAG, this.outputPower);
        return tag;
    }

    @Override
    public String getName() {
        return this.name != null ? this.name.getUnformattedText() : "";
    }

    @Nullable
    public ITextComponent getCustomName() {
        return this.name;
    }

    @Override
    public boolean hasCustomName() {
        return this.name != null;
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.name != null ? this.name : Components.literal("");
    }

    public void setCustomName(ITextComponent name) {
        this.name = name;
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
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public void refresh() {
        this.markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-2, 0, -2), pos.add(2, 2, 2));
    }

    public void hit(World level, IBlockState state, RayTraceResult hit, boolean isUpperBlock) {
        if (this.world != null && state.getValue(STAND)) {
            BlockPos blockPos = hit.getBlockPos();
            // 如果是击中上方，把状态移动到下方处理
            if (isUpperBlock) {
                blockPos = blockPos.down();
                state = level.getBlockState(blockPos);
            }
            int redstoneStrength = TargetBlock.getRedstoneStrength(hit, isUpperBlock);
            TargetBlock.setStandState(level, blockPos, state, false, redstoneStrength);
            level.scheduleUpdate(blockPos, state.getBlock(), RESET_TIME);
            // 原版的声音传播距离由 volume 决定
            // 当声音大于 1 时，距离为 = 16 * volume
            float volume = OtherConfig.TARGET_SOUND_DISTANCE.get() / 16.0f;
            volume = Math.max(volume, 0);
            level.playSound(null, blockPos, ModSounds.TARGET_HIT.get(), SoundCategory.BLOCKS, volume, this.world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }
}
