package com.tacz.guns.legacy.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.tooltip.ClientAmmoBoxTooltip;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.client.tooltip.ClientBlockItemTooltip;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.item.AttachmentItem;
import com.tacz.guns.item.GunSmithTableItem;
import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stands in for 1.20.1's {@code TooltipComponent} / {@code ClientTooltipComponent} tooltip images (upstream:
 * {@code Item#getTooltipImage} + {@code RegisterClientTooltipComponentFactoriesEvent}, the image is inserted right under
 * the item name). 1.12.2 tooltips are plain string lists: {@link ItemTooltipEvent} inserts blank marker lines
 * of the component's size at index 1, {@link RenderTooltipEvent.PostText} draws the component over them.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class TooltipComponentHandler {
    /**
     * Invisible formatting codes: unique enough to recognise our own filler lines.
     */
    private static final String MARKER = "§r§r§r";
    private static final int LINE_HEIGHT = 10;

    private static ItemStack lastStack = ItemStack.EMPTY;
    private static ClientTooltipComponent lastComponent;

    private TooltipComponentHandler() {
    }

    /**
     * {@code Item#getTooltipImage} of the items that have one + the {@code ClientSetupEvent} factory registration.
     */
    @Nullable
    public static ClientTooltipComponent createComponent(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof AbstractGunItem) {
            Optional<GunTooltip> image = ((AbstractGunItem) item).getTooltipImage(stack);
            return image.isPresent() ? new ClientGunTooltip(image.get()) : null;
        } else if (item instanceof AmmoBoxItem) {
            Optional<AmmoBoxTooltip> image = ((AmmoBoxItem) item).getTooltipImage(stack);
            return image.isPresent() ? new ClientAmmoBoxTooltip(image.get()) : null;
        } else if (item instanceof AttachmentItem) {
            Optional<AttachmentItemTooltip> image = ((AttachmentItem) item).getTooltipImage(stack);
            return image.isPresent() ? new ClientAttachmentItemTooltip(image.get()) : null;
        } else if (item instanceof GunSmithTableItem) {
            Optional<BlockItemTooltip> image = ((GunSmithTableItem) item).getTooltipImage(stack);
            return image.isPresent() ? new ClientBlockItemTooltip(image.get()) : null;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<String> lines = event.getToolTip();
        if (stack.isEmpty() || lines.isEmpty()) {
            return;
        }
        ClientTooltipComponent component = createComponent(stack);
        lastStack = stack;
        lastComponent = component;
        if (component == null) {
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int count = Math.max(1, (component.getHeight() + LINE_HEIGHT - 1) / LINE_HEIGHT);
        // the widest filler line makes the tooltip box wide enough (a space is 4 pixels wide)
        int spaceWidth = Math.max(1, font.getStringWidth(" "));
        StringBuilder first = new StringBuilder(MARKER);
        for (int i = 0; i < (component.getWidth(font) + spaceWidth - 1) / spaceWidth; i++) {
            first.append(' ');
        }
        List<String> filler = new ArrayList<>(count);
        filler.add(first.toString());
        for (int i = 1; i < count; i++) {
            filler.add(MARKER);
        }
        lines.addAll(1, filler);
    }

    @SubscribeEvent
    public static void onRenderTooltipText(RenderTooltipEvent.PostText event) {
        List<String> lines = event.getLines();
        int index = -1;
        for (int i = 1; i < lines.size(); i++) {
            // vanilla puts a gray formatting code in front of every line below the title
            if (lines.get(i).contains(MARKER)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        ItemStack stack = event.getStack();
        ClientTooltipComponent component;
        if (stack == lastStack || ItemStack.areItemStacksEqual(stack, lastStack)) {
            component = lastComponent;
        } else {
            component = createComponent(stack);
        }
        if (component == null) {
            return;
        }
        // GuiUtils / GuiScreen#drawHoveringText: the title is followed by 2 more pixels
        int x = event.getX();
        int y = event.getY() + 2 + index * LINE_HEIGHT;
        GuiGraphics graphics = new GuiGraphics();
        component.renderText(event.getFontRenderer(), x, y, graphics);
        component.renderImage(event.getFontRenderer(), x, y, graphics);
    }
}
