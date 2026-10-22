package com.tacz.guns.legacy.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.event.LoadingConfigEvent;
import com.tacz.guns.legacy.config.ForgeConfigSpec;
import com.tacz.guns.legacy.config.TaczConfigs;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2: the config screen of one of the three TACZ config files, opened from {@link TaczConfigHub} (the Config
 * button of the Mods screen, and the open-config key). Lists the file's categories as child screens; every property
 * is edited with Forge's standard widget and shows its config comment as a tooltip.
 * <p>
 * Done writes the edited values into the {@link Configuration}'s properties; {@link #onConfigChanged} then re-reads
 * the spec's values from them, writes the files and re-runs the config-dependent initialization, so the changes
 * apply without a restart.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class TaczConfigGui extends GuiConfig {
    public TaczConfigGui(GuiScreen parent, ForgeConfigSpec spec, String titleLine2) {
        super(parent, categoryElements(spec), GunMod.MOD_ID, null, false, false, "TaCZ Legacy Config", titleLine2);
    }

    private static List<IConfigElement> categoryElements(ForgeConfigSpec spec) {
        List<IConfigElement> elements = new ArrayList<>();
        Configuration configuration = spec.getConfiguration();
        if (configuration == null) {
            return elements;
        }
        for (String name : configuration.getCategoryNames()) {
            if (Configuration.CATEGORY_GENERAL.equals(name)) {
                continue;
            }
            ConfigCategory category = configuration.getCategory(name);
            if (category.isChild() || !category.showInGui()
                    || (category.getValues().isEmpty() && category.getChildren().isEmpty())) {
                continue;
            }
            elements.add(new ConfigElement(category));
        }
        return elements;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!GunMod.MOD_ID.equals(event.getModID())) {
            return;
        }
        TaczConfigs.reload();
        TaczConfigs.save();
        LoadingConfigEvent.onReloadingConfig();
    }
}
