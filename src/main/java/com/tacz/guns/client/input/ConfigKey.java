package com.tacz.guns.client.input;

import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.client.TaczConfigHub;
import com.tacz.guns.legacy.util.KeyInputs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class ConfigKey {
    public static final KeyBinding OPEN_CONFIG_KEY = new KeyBinding("key.tacz.open_config.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            Keyboard.KEY_T,
            "key.category.tacz");

    @SubscribeEvent
    public static void onOpenConfig(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress()
                && KeyInputs.matches(OPEN_CONFIG_KEY)
                && OPEN_CONFIG_KEY.getKeyModifier().equals(KeyModifier.getActiveModifier())) {
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
