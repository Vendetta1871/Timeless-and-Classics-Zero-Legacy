package com.tacz.guns.legacy.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Coremod entry point: only exists so MixinBooter applies {@code tacz.mixins.json} early enough to patch
 * vanilla classes. All mod logic lives in {@link com.tacz.guns.GunMod}.
 */
@IFMLLoadingPlugin.Name("TaczLoadingPlugin")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class TaczLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("tacz.mixins.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
