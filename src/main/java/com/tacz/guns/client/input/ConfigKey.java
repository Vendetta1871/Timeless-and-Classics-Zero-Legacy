package com.tacz.guns.client.input;

import com.tacz.guns.legacy.client.TaczConfigHub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
public class ConfigKey {
    public static final KeyBinding OPEN_CONFIG_KEY = new KeyBinding("key.tacz.open_config.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            Keyboard.KEY_T,
            "key.category.tacz");

    public static void onOpenConfig() {
        if (isInGame()) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            // 1.12.2: upstream opens the Cloth Config screen (compat/cloth/MenuIntegration), which does not exist
            // here; open the Forge config GUI instead - the same one as the Config button of the Mods screen
            Minecraft.getMinecraft().displayGuiScreen(new TaczConfigHub(Minecraft.getMinecraft().currentScreen));
        }
    }
}
