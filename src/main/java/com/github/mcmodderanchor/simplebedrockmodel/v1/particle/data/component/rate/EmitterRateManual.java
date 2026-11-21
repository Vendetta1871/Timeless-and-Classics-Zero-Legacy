package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.rate;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IEmitterComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 手动发射模式。对应 "minecraft:emitter_rate_manual"。不自动发射粒子，由外部 API 触发。
 */
public final class EmitterRateManual implements RateComponent, IEmitterComponent {
    private final MolangExpression maxParticles;

    public EmitterRateManual(MolangExpression maxParticles) {
        this.maxParticles = maxParticles;
    }

    public MolangExpression maxParticles() {
        return maxParticles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterRateManual)) return false;
        EmitterRateManual that = (EmitterRateManual) o;
        return java.util.Objects.equals(maxParticles, that.maxParticles);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(maxParticles);
    }

    @Override
    public String toString() {
        return "EmitterRateManual[" + "maxParticles=" + maxParticles + "]";
    }


    @Override public int order() { return 530; }
    @Override public boolean requireUpdate() { return false; }

    public static EmitterRateManual fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return new EmitterRateManual(molang.compile(getMolang(obj, "max_particles", "50")));
    }
}
