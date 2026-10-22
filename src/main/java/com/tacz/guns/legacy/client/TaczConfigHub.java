package com.tacz.guns.legacy.client;

import com.tacz.guns.legacy.config.TaczConfigs;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * 1.12.2: the mod's main config screen (the Config button of the Mods screen, and the open-config key): picks which
 * of the three config files to edit in a {@link TaczConfigGui}.
 */
@SideOnly(Side.CLIENT)
public class TaczConfigHub extends GuiScreen {
    private static final int ID_CLIENT = 0;
    private static final int ID_COMMON = 1;
    private static final int ID_SERVER = 2;
    private static final int ID_DONE = 3;

    private final GuiScreen parentScreen;

    public TaczConfigHub(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        int y = this.height / 2 - 34;
        this.buttonList.add(new GuiButton(ID_CLIENT, this.width / 2 - 110, y, 220, 20, "Client settings (tacz-client.cfg)"));
        this.buttonList.add(new GuiButton(ID_COMMON, this.width / 2 - 110, y + 24, 220, 20, "Common settings (tacz-common.cfg)"));
        this.buttonList.add(new GuiButton(ID_SERVER, this.width / 2 - 110, y + 48, 220, 20, "Server settings (tacz-server.cfg)"));
        this.buttonList.add(new GuiButton(ID_DONE, this.width / 2 - 100, this.height - 27, 200, 20, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        switch (button.id) {
            case ID_CLIENT -> this.mc.displayGuiScreen(new TaczConfigGui(this, TaczConfigs.CLIENT_SPEC, "Client settings (tacz-client.cfg)"));
            case ID_COMMON -> this.mc.displayGuiScreen(new TaczConfigGui(this, TaczConfigs.COMMON_SPEC, "Common settings (tacz-common.cfg)"));
            case ID_SERVER -> this.mc.displayGuiScreen(new TaczConfigGui(this, TaczConfigs.SERVER_SPEC, "Server settings (tacz-server.cfg)"));
            case ID_DONE -> this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, "TaCZ Legacy Config", this.width / 2, 20, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "Changes are saved when you press Done in a section",
                this.width / 2, 34, 0xAAAAAA);
    }
}
