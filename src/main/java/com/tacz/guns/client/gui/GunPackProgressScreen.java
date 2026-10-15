package com.tacz.guns.client.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.MutableComponent;
import com.tacz.guns.legacy.util.gui.Button;
import com.tacz.guns.legacy.util.gui.Screen;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * 1.12.2: {@code ProgressListener} is {@link IProgressUpdate}; its methods call the upstream ones (the gun pack
 * downloader that drives this screen is disabled upstream as well).
 */
public class GunPackProgressScreen extends Screen implements IProgressUpdate {
    private @Nullable ITextComponent header;
    private @Nullable ITextComponent stage;
    private int progress;
    private boolean stop;

    public GunPackProgressScreen() {
        // GameNarrator.NO_TITLE
        super(Components.literal(""));
    }

    @Override
    public void init() {
        Button button = new Button((width - 200) / 2, 120, 200, 20,
                Components.translatable("gui.tacz.client_gun_pack_downloader.background_download"), b -> this.stop());
        this.addRenderableWidget(button);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (this.stop) {
            this.getMinecraft().displayGuiScreen(null);
        } else {
            this.renderBackground(gui);
            if (this.header != null) {
                gui.drawCenteredString(this.font, this.header, this.width / 2, 70, 16777215);
            }
            if (this.stage != null && this.progress > 0) {
                MutableComponent text = Components.literal(this.stage.getFormattedText()).append(" " + this.progress + "%");
                gui.drawCenteredString(this.font, text, this.width / 2, 90, 16777215);
            }
            super.render(gui, mouseX, mouseY, partialTick);
        }
    }

    public void progressStartNoAbort(ITextComponent component) {
        this.progressStart(component);
    }

    public void progressStart(ITextComponent header) {
        this.header = Components.translatable("gui.tacz.client_gun_pack_downloader.downloading");
    }

    public void progressStage(ITextComponent component) {
        this.stage = component;
        this.progressStagePercentage(0);
    }

    public void progressStagePercentage(int progress) {
        this.progress = progress;
    }

    public void stop() {
        this.stop = true;
    }

    // IProgressUpdate -> the ProgressListener methods above
    @Override
    public void displaySavingString(@NotNull String message) {
        this.progressStartNoAbort(Components.literal(message));
    }

    @Override
    public void resetProgressAndMessage(@NotNull String message) {
        this.progressStart(Components.literal(message));
    }

    @Override
    public void displayLoadingString(@NotNull String message) {
        this.progressStage(Components.literal(message));
    }

    @Override
    public void setLoadingProgress(int progress) {
        this.progressStagePercentage(progress);
    }

    @Override
    public void setDoneWorking() {
        this.stop();
    }
}
