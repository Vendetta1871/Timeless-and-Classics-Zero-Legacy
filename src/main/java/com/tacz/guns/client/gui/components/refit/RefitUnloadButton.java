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

public class RefitUnloadButton extends Button implements IComponentTooltip {
    public RefitUnloadButton(int pX, int pY, Button.OnPress pOnPress) {
        super(pX, pY, 8, 8, Button.empty(), pOnPress);
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        int x = getX(), y = getY();
        if (isHoveredOrFocused()) {
            graphics.blit(GunRefitScreen.UNLOAD_TEXTURE, x, y, width, height, 0, 0, 80, 80, 160, 80);
        } else {
            graphics.blit(GunRefitScreen.UNLOAD_TEXTURE, x, y, width, height, 80, 0, 80, 80, 160, 80);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @Override
    public void renderTooltip(Consumer<List<ITextComponent>> consumer) {
        if (this.isHoveredOrFocused()) {
            consumer.accept(Collections.singletonList(Components.translatable("tooltip.tacz.refit.unload")));
        }
    }
}
