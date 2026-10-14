package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.renderer.item.AttachmentItemRenderer;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static com.tacz.guns.util.datafixer.AttachmentIdFix.updateAttachmentIdInTag;

public class AttachmentItem extends Item implements AttachmentItemDataAccessor {
    public AttachmentItem() {
        super();
        this.setMaxStackSize(1);
        this.setTranslationKey(GunMod.MOD_ID + ".attachment");
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.initializeClient();
        }
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        ResourceLocation attachmentId = this.getAttachmentId(stack);
        Optional<ClientAttachmentIndex> attachmentIndex = TimelessAPI.getClientAttachmentIndex(attachmentId);
        if (attachmentIndex.isPresent()) {
            return I18n.format(attachmentIndex.get().getName());
        }
        return super.getItemStackDisplayName(stack);
    }

    private static Comparator<Map.Entry<ResourceLocation, CommonAttachmentIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    public static NonNullList<ItemStack> fillItemCategory(AttachmentType type) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonAttachmentIndex().stream().sorted(idNameSort()).forEach(entry -> {
            if (entry.getValue().getPojo().isHidden()) {
                return;
            }
            if (type.equals(entry.getValue().getType())) {
                ItemStack itemStack = AttachmentItemBuilder.create().setId(entry.getKey()).build();
                stacks.add(itemStack);
            }
        });
        return stacks;
    }

    /**
     * 1.12.2: {@code IClientItemExtensions#getCustomRenderer}
     */
    @SideOnly(Side.CLIENT)
    private void initializeClient() {
        this.setTileEntityItemStackRenderer(new AttachmentItemRenderer());
    }

    @Override
    @Nonnull
    public AttachmentType getType(ItemStack attachmentStack) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (iAttachment != null) {
            ResourceLocation id = iAttachment.getAttachmentId(attachmentStack);
            return TimelessAPI.getCommonAttachmentIndex(id).map(CommonAttachmentIndex::getType).orElse(AttachmentType.NONE);
        } else {
            return AttachmentType.NONE;
        }
    }

    // TODO(port): 1.12.2 has no TooltipComponent / Item#getTooltipImage; AttachmentItemTooltip is a plain class here and needs a tooltip event hook
    public Optional<AttachmentItemTooltip> getTooltipImage(ItemStack stack) {
        return Optional.of(new AttachmentItemTooltip(this.getAttachmentId(stack), this.getType(stack), stack));
    }

    // TODO(port): 1.12.2 has no Item#verifyTagAfterLoad; the attachment id data fix needs a hook where stacks are read
    public void verifyTagAfterLoad(@NotNull NBTTagCompound tag) {
        updateAttachmentIdInTag(tag);
    }
}
