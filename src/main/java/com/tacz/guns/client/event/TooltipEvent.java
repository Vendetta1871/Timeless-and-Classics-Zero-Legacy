package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import net.minecraftforge.fml.relauncher.Side;
import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModItems;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.tacz.guns.legacy.util.Components;


// 1.12.2: ItemTooltipEvent is client-only there (ITooltipFlag)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class TooltipEvent {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getFlags().isAdvanced() && RenderConfig.ENABLE_TACZ_ID_IN_TOOLTIP.get()) {
            if (event.getItemStack().getItem() instanceof GunItemDataAccessor item) {
                event.getToolTip().add(formatTooltip(GunItemDataAccessor.GUN_ID_TAG, item.getGunId(event.getItemStack())).getFormattedText());
            } else if (event.getItemStack().getItem() instanceof AmmoItemDataAccessor item) {
                event.getToolTip().add(formatTooltip(AmmoItemDataAccessor.AMMO_ID_TAG, item.getAmmoId(event.getItemStack())).getFormattedText());
            } else if (event.getItemStack().getItem() instanceof AttachmentItemDataAccessor item) {
                event.getToolTip().add(formatTooltip(AttachmentItemDataAccessor.ATTACHMENT_ID_TAG, item.getAttachmentId(event.getItemStack())).getFormattedText());
            }
           else if (event.getItemStack().getItem() instanceof BlockItemDataAccessor item && !ModItems.GUN_SMITH_TABLE.get().equals(item)) {
               event.getToolTip().add(formatTooltip(BlockItemDataAccessor.BLOCK_ID, item.getBlockId(event.getItemStack())).getFormattedText());
           }
        }
    }

    public static ITextComponent formatTooltip(String key, ResourceLocation value) {
        return Components.literal(String.format("%s: \"%s\"", key, value)).withStyle(TextFormatting.DARK_GRAY);
    }
}
