package com.tacz.guns.inventory;

import net.minecraft.item.ItemStack;

import java.util.Objects;

/** A menu-local identity and display snapshot, never a mutable inventory slot. */
public final class AmmoStorageEntry {
    private final int id;
    private final ItemStack sample;
    private final long count;
    private final long remainingCapacity;

    public AmmoStorageEntry(int id, ItemStack sample, long count, long remainingCapacity) {
        // ItemStack#copyWithCount(1)
        ItemStack single = sample.copy();
        single.setCount(1);
        this.id = id;
        this.sample = single;
        this.count = count;
        this.remainingCapacity = remainingCapacity;
    }

    public int id() {
        return id;
    }

    public ItemStack sample() {
        return sample;
    }

    public long count() {
        return count;
    }

    public long remainingCapacity() {
        return remainingCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AmmoStorageEntry)) {
            return false;
        }
        AmmoStorageEntry that = (AmmoStorageEntry) o;
        return id == that.id && count == that.count && remainingCapacity == that.remainingCapacity
                && ItemStack.areItemStacksEqual(sample, that.sample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sample.getItem(), count, remainingCapacity);
    }

    @Override
    public String toString() {
        return "AmmoStorageEntry[id=" + id + ", sample=" + sample + ", count=" + count + ", remainingCapacity=" + remainingCapacity + "]";
    }
}
