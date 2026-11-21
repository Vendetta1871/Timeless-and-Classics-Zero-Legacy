package com.tacz.guns.client.gui.overlay;

import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.FontUtil;
import com.tacz.guns.legacy.util.IGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class DisplayCaseLockHintOverlay implements IGuiOverlay {
    private static final int DISPLAY_TICKS = 40;
    private static final int Y_OFFSET = 32;
    /**
     * {@code ChatFormatting.YELLOW.getColor()}
     */
    private static final int YELLOW = 0xFFFF55;
    private static ITextComponent message = Components.literal("");
    private static int ticksRemaining;

    public static void show(ITextComponent component) {
        message = component;
        ticksRemaining = DISPLAY_TICKS;
    }

    @Override
    public void render(GuiIngame gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (ticksRemaining <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        graphics.drawString(font, message, (int) ((width - FontUtil.width(font, message)) / 2.0F), (int) (height / 2.0F + Y_OFFSET), YELLOW, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksRemaining > 0) {
            ticksRemaining--;
        }
    }
}
