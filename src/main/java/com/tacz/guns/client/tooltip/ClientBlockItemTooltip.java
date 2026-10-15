package com.tacz.guns.client.tooltip;

import com.google.common.collect.Lists;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.ClientTooltipComponent;
import com.tacz.guns.legacy.util.FontUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import com.tacz.guns.legacy.util.Components;

public class ClientBlockItemTooltip implements ClientTooltipComponent {
    private final ResourceLocation blockId;
    private final List<ITextComponent> components = Lists.newArrayList();
    private @Nullable ITextComponent packInfo;

    public ClientBlockItemTooltip(BlockItemTooltip tooltip) {
        this.blockId = tooltip.getBlockId();
        this.addText();
        this.addPackInfo();
    }

    private void addPackInfo() {
        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(blockId);
        if (packInfoObject != null) {
            packInfo = Components.translatable(packInfoObject.getName()).withStyle(TextFormatting.BLUE).withStyle(TextFormatting.ITALIC);
        }
    }


    @Override
    public int getHeight() {
        return components.size() * 10 + (packInfo != null ? 16 : 0);
    }

    @Override
    public int getWidth(FontRenderer font) {
        int[] width = new int[]{0};
        if (packInfo != null) {
            width[0] = Math.max(width[0], FontUtil.width(font, packInfo) + 4);
        }
        components.forEach(c -> width[0] = Math.max(width[0], FontUtil.width(font, c)));
        return width[0];
    }

    @Override
    public void renderText(FontRenderer font, int pX, int pY, GuiGraphics graphics) {
        int yOffset = pY;
        for (ITextComponent component : this.components) {
            graphics.drawString(font, component, pX, yOffset, 0xffaa00, false);
            yOffset += 10;
        }
        // 枪包名
        if (packInfo != null) {
            graphics.drawString(font, this.packInfo, pX, yOffset + 6, 0xffffff, false);
        }
    }

    @Override
    public void renderImage(FontRenderer font, int mouseX, int mouseY, GuiGraphics gui) {
    }

    private void addText() {
        TimelessAPI.getClientBlockIndex(blockId).ifPresent(index -> {
            @Nullable String tooltipKey = index.getTooltipKey();
            if (tooltipKey != null) {
                String text = I18n.format(tooltipKey);
                String[] split = text.split("\n");
                Arrays.stream(split).forEach(s -> components.add(Components.literal(s).withStyle(TextFormatting.GRAY)));
            }
        });
    }
}
