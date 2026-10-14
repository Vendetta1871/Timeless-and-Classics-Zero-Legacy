package com.tacz.guns.api.item.nbt;

import com.tacz.guns.legacy.util.StackNbt;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.util.ResourceLocations;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface BlockItemDataAccessor extends IBlock {
    String BLOCK_ID = "BlockId";

    @Override
    @Nonnull
    default ResourceLocation getBlockId(ItemStack block) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(block);
        if (nbt.hasKey(BLOCK_ID, Constants.NBT.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocations.tryParse(nbt.getString(BLOCK_ID));
            return gunId != null ? gunId : DefaultAssets.EMPTY_BLOCK_ID;
        }
        return DefaultAssets.EMPTY_BLOCK_ID;
    }

    @Override
    default void setBlockId(ItemStack block, @Nullable ResourceLocation blockId) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(block);
        if (blockId != null) {
            nbt.setString(BLOCK_ID, blockId.toString());
            return;
        }
        nbt.setString(BLOCK_ID, DefaultAssets.EMPTY_BLOCK_ID.toString());
    }

}
