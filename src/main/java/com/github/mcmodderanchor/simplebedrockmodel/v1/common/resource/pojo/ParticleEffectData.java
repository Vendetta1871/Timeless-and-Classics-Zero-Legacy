package com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo;

import net.minecraft.util.ResourceLocation;

public final class ParticleEffectData {
    private final ResourceLocation effect;
    private final String locator;
    private final String preEffectScript;

    public ParticleEffectData(ResourceLocation effect, String locator, String preEffectScript) {
        this.effect = effect;
        this.locator = locator;
        this.preEffectScript = preEffectScript;
    }

    public ResourceLocation effect() {
        return effect;
    }

    public String locator() {
        return locator;
    }

    public String preEffectScript() {
        return preEffectScript;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleEffectData)) return false;
        ParticleEffectData that = (ParticleEffectData) o;
        return java.util.Objects.equals(effect, that.effect) && java.util.Objects.equals(locator, that.locator) && java.util.Objects.equals(preEffectScript, that.preEffectScript);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(effect, locator, preEffectScript);
    }

    @Override
    public String toString() {
        return "ParticleEffectData[" + "effect=" + effect + ", " + "locator=" + locator + ", " + "preEffectScript=" + preEffectScript + "]";
    }

}
