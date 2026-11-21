package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.rate;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IEmitterComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 持续发射。对应 "minecraft:emitter_rate_steady"。
 */
public final class EmitterRateSteady implements RateComponent {
    private final MolangExpression spawnRate;
    private final MolangExpression maxParticles;

    public EmitterRateSteady(MolangExpression spawnRate, MolangExpression maxParticles) {
        this.spawnRate = spawnRate;
        this.maxParticles = maxParticles;
    }

    public MolangExpression spawnRate() {
        return spawnRate;
    }

    public MolangExpression maxParticles() {
        return maxParticles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterRateSteady)) return false;
        EmitterRateSteady that = (EmitterRateSteady) o;
        return java.util.Objects.equals(spawnRate, that.spawnRate) && java.util.Objects.equals(maxParticles, that.maxParticles);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(spawnRate, maxParticles);
    }

    @Override
    public String toString() {
        return "EmitterRateSteady[" + "spawnRate=" + spawnRate + ", " + "maxParticles=" + maxParticles + "]";
    }


    @Override public int order() { return 530; }
    @Override public boolean requireUpdate() { return true; }

    @Override
    public IEmitterComponent createRuntime() {
        return new Runtime(spawnRate, maxParticles);
    }

    public static EmitterRateSteady fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return new EmitterRateSteady(
                molang.compile(getMolang(obj, "spawn_rate", "1")),
                molang.compile(getMolang(obj, "max_particles", "50")));
    }

    // ===== Runtime =====

    static final class Runtime implements IEmitterComponent {
        private final MolangExpression spawnRate;
        private final MolangExpression maxParticles;
        private float spawnAccumulator;

        Runtime(MolangExpression spawnRate, MolangExpression maxParticles) {
            this.spawnRate = spawnRate;
            this.maxParticles = maxParticles;
        }

        @Override
        public void apply(ParticleEmitterInstance emitter) {
            this.spawnAccumulator = 0;
        }

        @Override
        public void update(ParticleEmitterInstance emitter) {
            if (!emitter.isActive()) return;
            MolangContext<?> ctx = emitter.getMolang().getContext();
            int maxP = (int) maxParticles.evaluate(ctx);
            float rate = (float) spawnRate.evaluate(ctx);
            spawnAccumulator += rate * emitter.getDt();
            while (spawnAccumulator >= 1f
                    && emitter.getParticleCount() < maxP) {
                spawnAccumulator -= 1f;
                emitter.spawnParticle();
            }
        }
    }
}
