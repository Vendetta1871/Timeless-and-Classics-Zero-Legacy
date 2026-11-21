package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.item.ItemStack;

import java.util.List;
import com.google.common.collect.ImmutableList;

/**
 * 一把枪的某种配件类型的一页查询结果。
 */
public class GunAttachmentQueryEntry {
    public static final int ATTACHMENTS_PER_PAGE = 54;

    private final ItemStack gunStack;
    private final AttachmentType attachmentType;
    private final List<ItemStack> attachmentStacks;
    private final int totalAttachmentCount;
    private final int pageNumber;
    private final int pageCount;

    GunAttachmentQueryEntry(ItemStack gunStack, AttachmentType attachmentType,
                                    List<ItemStack> attachmentStacks, int totalAttachmentCount,
                                    int pageNumber, int pageCount) {
        this.gunStack = gunStack;
        this.attachmentType = attachmentType;
        this.attachmentStacks = ImmutableList.copyOf(attachmentStacks);
        this.totalAttachmentCount = totalAttachmentCount;
        this.pageNumber = pageNumber;
        this.pageCount = pageCount;
    }

    public static List<GunAttachmentQueryEntry> getAllGunAttachmentQueryEntries() {
        return AttachmentQueryData.load().gunAttachmentQueries();
    }

    public ItemStack getGunStack() {
        return gunStack;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public List<ItemStack> getAttachmentStacks() {
        return attachmentStacks;
    }

    public int getTotalAttachmentCount() {
        return totalAttachmentCount;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageCount() {
        return pageCount;
    }
}
