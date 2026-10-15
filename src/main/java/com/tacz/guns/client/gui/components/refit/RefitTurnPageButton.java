package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.gui.Button;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import com.tacz.guns.legacy.util.Components;

public class RefitTurnPageButton extends Button implements IComponentTooltip {
    private final boolean isUpPage;

    public RefitTurnPageButton(int pX, int pY, boolean isUpPage, OnPress pOnPress) {
        super(pX, pY, 18, 8, Button.empty(), pOnPress);
        this.isUpPage = isUpPage;
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        int x = getX(), y = getY();
        int yOffset = isUpPage ? 0 : 80;
        if (isHoveredOrFocused()) {
            graphics.blit(GunRefitScreen.TURN_PAGE_TEXTURE, x, y, width, height, 0, yOffset, 180, 80, 180, 160);
        } else {
            graphics.blit(GunRefitScreen.TURN_PAGE_TEXTURE, x + 1, y + 1, width - 2, height - 2, 10, yOffset + 10, 180 - 20, 80 - 20, 180, 160);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @Override
    public void renderTooltip(Consumer<List<ITextComponent>> consumer) {
        if (this.isHoveredOrFocused()) {
            String key = isUpPage ? "tooltip.tacz.page.previous" : "tooltip.tacz.page.next";
            consumer.accept(Collections.singletonList(Components.translatable(key)));
        }
    }
}
