package com.tacz.guns.client.event;

import com.tacz.guns.resource.PackConvertor;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import com.tacz.guns.legacy.util.Components;


@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT)
public class PlayerEnterWorld {

    @SubscribeEvent
    public static void onPlayerEnterWorld(PlayerEvent.PlayerLoggedInEvent event) {
        File[] files = PackConvertor.FOLDER.toFile().listFiles();
        if (files != null && files.length > 0){
            event.player.sendMessage(pre(Components.translatable("message.tacz.convert_from_legacy.intro")));
            event.player.sendMessage(pre(Components.translatable("message.tacz.convert_from_legacy.intro2")));
            ITextComponent component = Components.translatable("message.tacz.convert_from_legacy")
                    .append(Components.translatable("message.tacz.convert_from_legacy.button")
                            .setStyle(new Style().setColor(TextFormatting.GREEN)
                            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tacz convert"))
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Components.translatable("message.tacz.convert_from_legacy.hover"))
            )));
            event.player.sendMessage(pre(component));
            event.player.sendMessage(pre(Components.translatable("message.tacz.convert_from_legacy.hint")));
            event.player.sendMessage(pre(Components.translatable("message.tacz.convert_from_legacy.hide")));
        }
    }

    private static ITextComponent pre(ITextComponent component) {
        return Components.translatable("message.tacz.pre").append(component);
    }
}
