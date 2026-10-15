package com.tacz.guns.client.gui.components;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.gui.Button;
import com.tacz.guns.legacy.util.gui.EditBox;
import com.tacz.guns.legacy.util.gui.GuiEventListener;
import com.tacz.guns.legacy.util.gui.Renderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.*;

/**
 * 1.12.2: {@code ContainerObjectSelectionList} does not exist; this is a small scrolling list of widgets with the same
 * constructor and public methods ({@code getRowLeft / getRowWidth / updateSize / setLeftPos}). The entries are laid out
 * as upstream: row {@code i} starts at {@code y0 + 4 - scroll + i * itemHeight}, {@code x0 + 4}.
 * Deviation: the vanilla {@code checkbox.png} does not exist in 1.12.2, the box is drawn with fills.
 */
public class GunPackList implements Renderable, GuiEventListener {
    private final GunSmithTableScreen parent;
    private final List<Checkbox> gunPackList = new ArrayList<>();
    private final Set<String> selectedNamespaces = new HashSet<>();
    private final Checkbox byHandCheckbox;
    private final EditBox byName;

    private final List<Entry> entries = new ArrayList<>();
    protected int width;
    protected int height;
    protected int x0;
    protected int x1;
    protected int y0;
    protected int y1;
    protected final int itemHeight;
    private double scrollAmount;
    private boolean scrolling;

    public GunPackList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight,
                       Map<ResourceLocation, List<ResourceLocation>> recipes, GunSmithTableScreen parent) {
        this.width = pWidth;
        this.height = pHeight;
        this.y0 = pY0;
        this.y1 = pY1;
        this.x0 = 0;
        this.x1 = pWidth;
        this.itemHeight = pItemHeight;
        this.parent = parent;
        Set<String> namespaces = new HashSet<>();
        for (List<ResourceLocation> entry : recipes.values()) {
            entry.forEach((resourceLocation) -> namespaces.add(resourceLocation.getNamespace()));
        }

        this.byName = new EditBox(pMinecraft.fontRenderer, 3, 0, 94, 10, Components.literal(""));
        this.byName.setHint(Components.translatable("gui.tacz.gun_smith_table.filter.search"));
        this.byName.setResponder((pText) -> {
            parent.init();
            parent.setIndexPage(0);
        });
        this.entries.add(new GunPackList.Entry(byName));

        this.byHandCheckbox = new Checkbox(0, 0, 10, 10, Components.translatable("gui.tacz.gun_smith_table.filter.handgun"), false) {
            @Override
            public void onPress() {
                super.onPress();
                parent.init();
                parent.setIndexPage(0);
            }
        };
        this.entries.add(new GunPackList.Entry(byHandCheckbox));

        Checkbox checkbox1 = new Checkbox(0, 0, 10, 10, Components.translatable("gui.tacz.gun_smith_table.filter.all"), true) {
            @Override
            public void onPress() {
                super.onPress();
                gunPackList.forEach((checkbox) -> checkbox.selected = this.selected);
                updateSelectedNamespaces();
            }
        };
        this.entries.add(new GunPackList.Entry(checkbox1));

        for (String namespace : namespaces) {
            PackInfo packInfo = ClientAssetsManager.INSTANCE.getPackInfo(namespace);
            ITextComponent name = packInfo == null ? Components.literal(namespace) : Components.translatable(packInfo.getName());

            Checkbox checkbox = new Checkbox(0, 0, 10, 10, name, namespace, true) {
                @Override
                public void onPress() {
                    super.onPress();
                    checkbox1.selected = gunPackList.stream().allMatch(Checkbox::selected);
                    updateSelectedNamespaces();
                }
            };
            gunPackList.add(checkbox);
            selectedNamespaces.add(namespace);
            this.entries.add(new GunPackList.Entry(checkbox));
        }
    }

    public String getSearchText() {
        return byName.getValue();
    }

    public boolean isByHandSelected() {
        return byHandCheckbox.selected;
    }

    public void setByHandSelected(boolean selected) {
        byHandCheckbox.selected = selected;
    }

    public Set<String> namespaceList() {
        return selectedNamespaces;
    }

    public void updateSelectedNamespaces() {
        selectedNamespaces.clear();
        gunPackList.forEach((checkbox) -> {
            if (checkbox.selected) {
                selectedNamespaces.add(checkbox.getId());
            }
        });
        parent.init();
        parent.setIndexPage(0);
    }

    public void updateSize(int width, int height, int y0, int y1) {
        this.width = width;
        this.height = height;
        this.y0 = y0;
        this.y1 = y1;
        this.x1 = this.x0 + width;
        this.scrollAmount = MathHelper.clamp(this.scrollAmount, 0.0, this.getMaxScroll());
    }

    public void setLeftPos(int left) {
        this.x0 = left;
        this.x1 = left + this.width;
    }

    protected int getScrollbarPosition() {
        return this.x1 - 2;
    }

    private int getMaxPosition() {
        return this.entries.size() * this.itemHeight + 4;
    }

    private int getMaxScroll() {
        return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
    }

    private int getRowTop(int index) {
        return this.y0 + 4 - (int) this.scrollAmount + index * this.itemHeight;
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseY >= this.y0 && mouseY <= this.y1 && mouseX >= this.x0 && mouseX <= this.x1;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.fill(this.x0, this.y0, this.x1, this.y1, 0x80000000);
        int i = this.getScrollbarPosition();
        int j = i + 6;

        pGuiGraphics.enableScissor(this.x0, this.y0, this.x1, this.y1);
        for (int index = 0; index < this.entries.size(); index++) {
            int top = this.getRowTop(index);
            if (top + this.itemHeight < this.y0 || top > this.y1) {
                continue;
            }
            this.entries.get(index).render(pGuiGraphics, index, top, this.getRowLeft(), this.getRowWidth(), this.itemHeight, pMouseX, pMouseY, pPartialTick);
        }
        pGuiGraphics.disableScissor();

        int i2 = this.getMaxScroll();
        if (i2 > 0) {
            int j2 = (int) ((float) ((this.y1 - this.y0) * (this.y1 - this.y0)) / (float) this.getMaxPosition());
            j2 = MathHelper.clamp(j2, 32, this.y1 - this.y0 - 8);
            int k1 = (int) this.scrollAmount * (this.y1 - this.y0 - j2) / i2 + this.y0;
            if (k1 < this.y0) {
                k1 = this.y0;
            }
            pGuiGraphics.fill(i, k1, j, k1 + j2, -8355712);
            pGuiGraphics.fill(i, k1, j - 1, k1 + j2 - 1, -4144960);
        }

        RenderSystem.disableBlend();
    }

    public int getRowLeft() {
        return this.x0 + 4;
    }

    public int getRowWidth() {
        return this.width;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // the search box has to see every click to lose its focus
        boolean inside = this.isInside(mouseX, mouseY);
        if (!inside) {
            byName.mouseClicked(-1, -1, button);
            return false;
        }
        if (button == 0 && this.getMaxScroll() > 0 && mouseX >= this.getScrollbarPosition() && mouseX <= this.getScrollbarPosition() + 6) {
            this.scrolling = true;
            return true;
        }
        for (Entry entry : this.entries) {
            if (entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            int maxScroll = this.getMaxScroll();
            double barHeight = Math.max(1, this.y1 - this.y0);
            this.scrollAmount = MathHelper.clamp(this.scrollAmount + dragY * Math.max(1, maxScroll) / barHeight * 2, 0.0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.isInside(mouseX, mouseY)) {
            return false;
        }
        this.scrollAmount = MathHelper.clamp(this.scrollAmount - delta * (double) this.itemHeight / 2.0, 0.0, this.getMaxScroll());
        return true;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        return byName.keyTyped(typedChar, keyCode);
    }

    @Override
    public void tick() {
        byName.tick();
    }

    public static class Entry {
        private final Renderable renderable;
        private final GuiEventListener listener;
        private final java.util.function.IntConsumer setX;
        private final java.util.function.IntConsumer setY;

        public Entry(Button widget) {
            this.renderable = widget;
            this.listener = widget;
            this.setX = widget::setX;
            this.setY = widget::setY;
        }

        public Entry(EditBox widget) {
            this.renderable = widget;
            this.listener = widget;
            this.setX = widget::setX;
            this.setY = widget::setY;
        }

        public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, float pPartialTick) {
            this.setX.accept(pLeft);
            this.setY.accept(pTop);
            this.renderable.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.listener.mouseClicked(mouseX, mouseY, button);
        }
    }

    public static class Checkbox extends Button {
        protected boolean selected;
        protected final boolean showLabel;
        private String id;

        public Checkbox(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, String id, boolean pSelected) {
            this(pX, pY, pWidth, pHeight, pMessage, pSelected, true);
            this.id = id;
        }

        public Checkbox(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, boolean pSelected) {
            this(pX, pY, pWidth, pHeight, pMessage, pSelected, true);
        }

        public Checkbox(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, boolean pSelected, boolean pShowLabel) {
            super(pX, pY, pWidth, pHeight, pMessage, b -> {
            });
            this.selected = pSelected;
            this.showLabel = pShowLabel;
        }

        public String getId() {
            return id;
        }

        @Override
        public void onPress() {
            this.selected = !this.selected;
        }

        public boolean selected() {
            return this.selected;
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            Minecraft minecraft = Minecraft.getMinecraft();
            RenderSystem.enableDepthTest();
            FontRenderer font = minecraft.fontRenderer;
            RenderSystem.enableBlend();
            // 1.12.2: no checkbox.png, a frame, a dark box and a white square for the check mark
            int x = this.getX();
            int y = this.getY();
            pGuiGraphics.fill(x, y, x + 10, y + 10, this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
            pGuiGraphics.fill(x + 1, y + 1, x + 9, y + 9, 0xFF000000);
            if (this.selected) {
                pGuiGraphics.fill(x + 3, y + 3, x + 7, y + 7, 0xFFFFFFFF);
            }
            if (this.showLabel) {
                pGuiGraphics.drawString(font, this.getMessage(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | 255 << 24);
            }
        }
    }
}
