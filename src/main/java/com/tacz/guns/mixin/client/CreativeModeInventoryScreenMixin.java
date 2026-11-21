package com.tacz.guns.mixin.client;

import com.tacz.guns.client.gui.components.CreativePackFilters;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.gui.Button;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2: {@code CreativeModeInventoryScreen} is {@link GuiContainerCreative}. It has no widget tree, so the filter
 * widgets live in {@link #tacz$widgets} and this mixin draws them and delivers clicks to them.
 */
@Mixin(GuiContainerCreative.class)
public abstract class CreativeModeInventoryScreenMixin extends InventoryEffectRenderer {
    @Shadow
    private static int selectedTabIndex;
    @Shadow
    private float currentScroll;
    @Unique
    private CreativePackFilters tacz$packFilters;
    /**
     * 1.12.2: the widgets upstream adds with {@code addRenderableWidget}.
     */
    @Unique
    private final List<Button> tacz$widgets = new ArrayList<>();
    /**
     * 1.12.2: a click consumed by a filter widget; its release must not reach the container (it would drop the
     * carried stack, the widgets are outside the panel).
     */
    @Unique
    private boolean tacz$consumedClick;

    protected CreativeModeInventoryScreenMixin(Container container) {
        super(container);
    }

    @Unique
    private static CreativeTabs tacz$selectedTab() {
        return CreativeTabs.CREATIVE_TAB_ARRAY[selectedTabIndex];
    }

    @Inject(method = "initGui()V", at = @At("TAIL"))
    private void tacz$initPackFilters(CallbackInfo ci) {
        // 1.12.2: outside creative mode initGui opens the survival inventory instead
        if (!this.mc.playerController.isInCreativeMode()) {
            return;
        }
        if (tacz$packFilters == null) {
            tacz$packFilters = new CreativePackFilters((GuiContainerCreative) (Object) this, () -> currentScroll = 0);
        }
        // 1.12.2: Screen#init clears the widgets of the previous init (resize)
        tacz$widgets.clear();
        // 直接 this::addRenderableWidget 会导致编译器生成妙妙方法直接引用 CreativeModeInventoryScreenMixin 然后原地爆炸
        tacz$packFilters.init(widget -> tacz$widgets.add(widget), tacz$selectedTab());
    }

    @Inject(method = "setCurrentCreativeTab(Lnet/minecraft/creativetab/CreativeTabs;)V", at = @At("TAIL"))
    private void tacz$selectPackFilterTab(CreativeTabs tab, CallbackInfo ci) {
        if (tacz$packFilters != null && tab != null) {
            tacz$packFilters.updateTab(tab, true);
        }
    }

    // 1.12.2: there is no refreshCurrentTabContents (tab contents are rebuilt only when a tab is selected, which
    // setCurrentCreativeTab above covers)

    @Inject(method = "drawScreen(IIF)V", at = @At("HEAD"))
    private void tacz$updatePackFilters(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (tacz$packFilters != null) {
            tacz$packFilters.updateTab(tacz$selectedTab(), false);
        }
    }

    /**
     * 1.12.2: the widgets are drawn after the screen background and before the slots, as
     * {@code AbstractContainerScreen#render} draws its renderables.
     */
    @Inject(method = "drawGuiContainerBackgroundLayer(FII)V", at = @At("TAIL"))
    private void tacz$renderPackFilters(float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (tacz$packFilters == null || tacz$widgets.isEmpty()) {
            return;
        }
        GuiGraphics graphics = new GuiGraphics();
        for (Button widget : tacz$widgets) {
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("TAIL"))
    private void tacz$renderPackFilterTooltips(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (tacz$packFilters != null) {
            tacz$packFilters.renderTooltips(new GuiGraphics(), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void tacz$clickPackFilters(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        // the last added widget is drawn on top
        for (int i = tacz$widgets.size() - 1; i >= 0; i--) {
            if (tacz$widgets.get(i).mouseClicked(mouseX, mouseY, mouseButton)) {
                tacz$consumedClick = true;
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "mouseReleased(III)V", at = @At("HEAD"), cancellable = true)
    private void tacz$releasePackFilters(int mouseX, int mouseY, int state, CallbackInfo ci) {
        if (tacz$consumedClick) {
            tacz$consumedClick = false;
            ci.cancel();
        }
    }

    /**
     * 1.12.2: {@code mouseScrolled}; the wheel is part of {@code handleMouseInput}.
     */
    @Inject(method = "handleMouseInput()V", at = @At("HEAD"), cancellable = true)
    private void tacz$scrollPackFilters(CallbackInfo ci) {
        int wheel = Mouse.getEventDWheel();
        if (tacz$packFilters == null || wheel == 0) {
            return;
        }
        double mouseX = (double) Mouse.getEventX() * this.width / this.mc.displayWidth;
        double mouseY = this.height - (double) Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (tacz$packFilters.mouseScrolled(mouseX, mouseY, wheel)) {
            ci.cancel();
        }
    }
}
