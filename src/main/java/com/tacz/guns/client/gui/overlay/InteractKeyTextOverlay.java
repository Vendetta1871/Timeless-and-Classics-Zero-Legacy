package com.tacz.guns.client.gui.overlay;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.client.input.InteractKey;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.client.gui.GuiIngame;
import com.tacz.guns.legacy.util.FontUtil;
import com.tacz.guns.legacy.util.IGuiOverlay;
import org.apache.commons.lang3.StringUtils;
import com.tacz.guns.legacy.util.Components;

public class InteractKeyTextOverlay implements IGuiOverlay {

    @Override
    public void render(GuiIngame gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (RenderConfig.DISABLE_INTERACT_HUD_TEXT.get()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        RayTraceResult hitResult = mc.objectMouseOver;
        if (hitResult == null) {
            return;
        }
        if (hitResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            renderBlockText(graphics, width, height, hitResult, player, mc);
            return;
        }
        if (hitResult.typeOfHit == RayTraceResult.Type.ENTITY) {
            renderEntityText(graphics, width, height, hitResult, mc);
        }
    }

    private static void renderBlockText(GuiGraphics graphics, int width, int height, RayTraceResult blockHitResult, EntityPlayerSP player, Minecraft mc) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        IBlockState block = player.world.getBlockState(blockPos);
        if (InteractKeyConfigRead.canInteractBlock(block)) {
            boolean mainHandHoldGun = IGun.mainHandHoldGun(player);
            boolean hasGunSmithTableFilterItem = hasGunSmithTableFilterItem(block, player);
            boolean willFilterByHand = RenderConfig.AUTO_SELECT_GUN_SMITH_TABLE_FILTER.get() && hasGunSmithTableFilterItem;
            if (mainHandHoldGun) {
                renderText(graphics, width, height, mc.fontRenderer, InteractKey.INTERACT_KEY.getDisplayName(), willFilterByHand);
            } else if (hasGunSmithTableFilterItem) {
                renderText(graphics, width, height, mc.fontRenderer, mc.gameSettings.keyBindUseItem.getDisplayName(), willFilterByHand);
            }
        }
    }

    private static void renderEntityText(GuiGraphics graphics, int width, int height, RayTraceResult entityHitResult, Minecraft mc) {
        if (mc.player == null || !IGun.mainHandHoldGun(mc.player)) {
            return;
        }
        Entity entity = entityHitResult.entityHit;
        if (InteractKeyConfigRead.canInteractEntity(entity)) {
            renderText(graphics, width, height, mc.fontRenderer, InteractKey.INTERACT_KEY.getDisplayName(), false);
        }
    }

    private static boolean hasGunSmithTableFilterItem(IBlockState block, EntityPlayerSP player) {
        if (!(block.getBlock() instanceof AbstractGunSmithTableBlock)) {
            return false;
        }
        ItemStack stack = player.getHeldItemMainhand();
        Item item = stack.getItem();
        return item instanceof IGun || item instanceof IAttachment || item instanceof IAmmo;
    }

    private static void renderText(GuiGraphics graphics, int width, int height, FontRenderer font, String keyName, boolean willFilterByHand) {
        ITextComponent title = Components.translatable("gui.tacz.interact_key.text.desc", StringUtils.capitalize(keyName));
        graphics.drawString(font, title, (int) ((width - FontUtil.width(font, title)) / 2.0f), (int) (height / 2.0f - 25), 0xFFFF55, false);
        if (willFilterByHand) {
            ITextComponent filter = Components.translatable("gui.tacz.interact_key.text.gun_smith_table_filter");
            graphics.drawString(font, filter, (int) ((width - FontUtil.width(font, filter)) / 2.0f), (int) (height / 2.0f - 14), 0xAAAAAA, false);
        }
    }
}
