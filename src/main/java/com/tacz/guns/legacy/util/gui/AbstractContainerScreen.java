package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * 1.20.1 {@code AbstractContainerScreen} on top of {@link GuiContainer}: {@code leftPos / topPos / imageWidth /
 * imageHeight / menu}, {@code renderBg} / {@code renderLabels} and {@code init()} keep their upstream shape. Like the
 * {@link Screen} shim, widgets are drawn and clicked through {@link ScreenWidgets}.
 * <p>
 * Deviation: container slots are not drawn or clickable (the only user, the gun smith table menu, has none).
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractContainerScreen<T extends Container> extends GuiContainer {
    private final ScreenWidgets widgets = new ScreenWidgets();
    protected final T menu;
    protected final ITextComponent title;
    protected Minecraft minecraft;
    protected FontRenderer font;
    protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected int leftPos;
    protected int topPos;
    protected final List<Renderable> renderables = widgets.renderables;

    protected AbstractContainerScreen(T menu, InventoryPlayer inventory, ITextComponent title) {
        super(menu);
        this.menu = menu;
        this.title = title;
    }

    public Minecraft getMinecraft() {
        return mc;
    }

    /**
     * Computes {@code leftPos / topPos} from {@code imageWidth / imageHeight}; subclasses call {@code super.init()}
     * first and then rebuild their widgets.
     */
    public void init() {
        this.xSize = this.imageWidth;
        this.ySize = this.imageHeight;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.leftPos = this.guiLeft;
        this.topPos = this.guiTop;
    }

    protected <W> W addRenderableWidget(W widget) {
        return widgets.add(widget);
    }

    protected void clearWidgets() {
        widgets.clear();
    }

    public void onClose() {
        this.mc.player.closeScreen();
    }

    public boolean isPauseScreen() {
        return true;
    }

    public void tick() {
    }

    protected abstract void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    public void renderBackground(GuiGraphics graphics) {
        this.drawDefaultBackground();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        widgets.render(graphics, mouseX, mouseY, partialTick, font);
        this.renderLabels(graphics, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return widgets.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return widgets.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return widgets.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return widgets.mouseScrolled(mouseX, mouseY, delta);
    }

    public boolean keyPressed(char typedChar, int keyCode) {
        return widgets.keyTyped(typedChar, keyCode);
    }

    @Override
    public final void initGui() {
        this.minecraft = this.mc;
        this.font = this.fontRenderer;
        this.init();
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.render(new GuiGraphics(), mouseX, mouseY, partialTicks);
    }

    @Override
    protected final void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }

    @Override
    public final void updateScreen() {
        widgets.tick();
        this.tick();
    }

    @Override
    public final boolean doesGuiPauseGame() {
        return isPauseScreen();
    }

    @Override
    protected final void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.keyPressed(typedChar, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.onClose();
        }
    }

    @Override
    protected final void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.mouseClicked((double) mouseX, (double) mouseY, mouseButton);
    }

    @Override
    protected final void mouseReleased(int mouseX, int mouseY, int state) {
        if (state >= 0) {
            this.mouseReleased((double) mouseX, (double) mouseY, state);
        }
    }

    @Override
    protected final void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        this.mouseDragged((double) mouseX, (double) mouseY, clickedMouseButton, 0, 0);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.mouseScrolled((double) mouseX, (double) mouseY, wheel > 0 ? 1.0 : -1.0);
        }
    }
}
