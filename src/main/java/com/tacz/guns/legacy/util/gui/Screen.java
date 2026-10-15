package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * 1.20.1 {@code Screen} on top of {@link GuiScreen}: {@code init()} / {@code render(GuiGraphics, ..)} /
 * {@code onClose()} / {@code isPauseScreen()} / {@code mouseScrolled} keep their upstream shape, widgets are added with
 * {@link #addRenderableWidget} (see {@link ScreenWidgets}). Create a {@link GuiGraphics} per frame in {@code drawScreen}.
 */
@SideOnly(Side.CLIENT)
public abstract class Screen extends GuiScreen {
    private final ScreenWidgets widgets = new ScreenWidgets();
    protected final ITextComponent title;
    protected Minecraft minecraft;
    protected FontRenderer font;
    protected final List<Renderable> renderables = widgets.renderables;
    // keeps the ScreenWidgets alive for the sibling classes of this package
    final ScreenWidgets widgets() {
        return widgets;
    }

    protected Screen(ITextComponent title) {
        this.title = title;
    }

    public Minecraft getMinecraft() {
        return mc;
    }

    public ITextComponent getTitle() {
        return title;
    }

    /**
     * Called from {@link #initGui()}; rebuild the widgets here ({@link #clearWidgets()} first).
     */
    public void init() {
    }

    protected <T> T addRenderableWidget(T widget) {
        return widgets.add(widget);
    }

    protected void clearWidgets() {
        widgets.clear();
    }

    public void onClose() {
        this.mc.displayGuiScreen(null);
    }

    public boolean isPauseScreen() {
        return true;
    }

    public void tick() {
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        widgets.render(graphics, mouseX, mouseY, partialTick, font);
    }

    public void renderBackground(GuiGraphics graphics) {
        this.drawDefaultBackground();
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

    /**
     * 1.20.1 {@code keyPressed} + {@code charTyped} in one: return true when the key was consumed.
     */
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
        if (keyCode == Keyboard.KEY_ESCAPE) {
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
