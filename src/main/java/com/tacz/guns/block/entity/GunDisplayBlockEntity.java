package com.tacz.guns.block.entity;

import net.minecraft.util.EnumFacing;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.NotNull;

/** 展示方块实体的共同契约；每个展示面按网格大小的平方连续编号。 */
public interface GunDisplayBlockEntity {
    ItemStack getItem(int slot);

    int getRotation(int slot);

    DisplayGunSlot getDisplaySlot(int slot);

    int getGridSize();

    EnumFacing getSlotFacing(int slot);

    /** 展示物品是否使用满亮度。 */
    boolean isDisplayGlowing();

    @NotNull
    TileEntity getBlockEntity();
}
