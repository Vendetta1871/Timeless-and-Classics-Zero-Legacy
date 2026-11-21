package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.lifetime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IEmitterComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 单次发射生命周期。对应 "minecraft:emitter_lifetime_once"。
 */
public final class EmitterLifetimeOnce implements LifetimeComponent {
    private final MolangExpression activeTime;

    public EmitterLifetimeOnce(MolangExpression activeTime) {
        this.activeTime = activeTime;
    }

    public MolangExpression activeTime() {
        return activeTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterLifetimeOnce)) return false;
        EmitterLifetimeOnce that = (EmitterLifetimeOnce) o;
        return java.util.Objects.equals(activeTime, that.activeTime);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(activeTime);
    }

    @Override
    public String toString() {
        return "EmitterLifetimeOnce[" + "activeTime=" + activeTime + "]";
    }


    @Override public int order() { return 500; }
    @Override public boolean requireUpdate() { return true; }

    @Override
    public IEmitterComponent createRuntime() {
        return new Runtime(activeTime);
    }

    public static EmitterLifetimeOnce fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return new EmitterLifetimeOnce(molang.compile(getMolang(obj, "active_time", "1")));
    }

    // ===== Runtime =====

    public static final class Runtime implements IEmitterComponent {
        private final MolangExpression activeTimeExpr;
        private float emitterAge;

        public Runtime(MolangExpression activeTime) { this.activeTimeExpr = activeTime; }

        @Override
        public void apply(ParticleEmitterInstance emitter) {
            this.emitterAge = 0;
            emitter.setRemoved(false);
            emitter.setActive(true);
            emitter.setEmitterAge(0);

            MolangContext<?> ctx = emitter.getMolang().getContext();
            emitter.setEmitterLifetime((float) activeTimeExpr.evaluate(ctx));
        }

        @Override
        public void update(ParticleEmitterInstance emitter) {
            emitterAge += emitter.getDt();
            emitter.setEmitterAge(emitterAge);
            emitter.bindContextAndCurves();

            MolangContext<?> ctx = emitter.getMolang().getContext();
            if (emitterAge >= (float) activeTimeExpr.evaluate(ctx)) {
                emitter.fireExpirationEvents();
                emitter.setRemoved(true);
                emitter.setActive(false);
            }
        }
    }
}
