package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.text.TextComponentString;
import java.util.ArrayList;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;

import java.util.List;
import java.util.function.Consumer;

public interface IComponentTooltip {
    /**
     * 获取物品的文本提示
     */
    static List<ITextComponent> getTooltipFromItem(ItemStack stack) {
        GameSettings options = Minecraft.getMinecraft().gameSettings;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        // 1.12.2: tooltip lines are formatted strings
        List<ITextComponent> lines = new ArrayList<>();
        for (String line : stack.getTooltip(player, options.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL)) {
            lines.add(new TextComponentString(line));
        }
        return lines;
    }

    /**
     * 添加此接口，会调用此渲染文本提示
     *
     * @param consumer 需要渲染的文本提示
     */
    void renderTooltip(Consumer<List<ITextComponent>> consumer);
}
