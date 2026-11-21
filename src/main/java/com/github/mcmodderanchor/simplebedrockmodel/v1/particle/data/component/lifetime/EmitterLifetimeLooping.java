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
 * 循环发射生命周期。对应 "minecraft:emitter_lifetime_looping"。
 */
public final class EmitterLifetimeLooping implements LifetimeComponent {
    private final MolangExpression activeTime;
    private final MolangExpression sleepTime;

    public EmitterLifetimeLooping(MolangExpression activeTime, MolangExpression sleepTime) {
        this.activeTime = activeTime;
        this.sleepTime = sleepTime;
    }

    public MolangExpression activeTime() {
        return activeTime;
    }

    public MolangExpression sleepTime() {
        return sleepTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterLifetimeLooping)) return false;
        EmitterLifetimeLooping that = (EmitterLifetimeLooping) o;
        return java.util.Objects.equals(activeTime, that.activeTime) && java.util.Objects.equals(sleepTime, that.sleepTime);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(activeTime, sleepTime);
    }

    @Override
    public String toString() {
        return "EmitterLifetimeLooping[" + "activeTime=" + activeTime + ", " + "sleepTime=" + sleepTime + "]";
    }


    @Override
    public int order() {
        return 500;
    }
    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public IEmitterComponent createRuntime() {
        return new Runtime(activeTime, sleepTime);
    }

    public static EmitterLifetimeLooping fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return new EmitterLifetimeLooping(
                molang.compile(getMolang(obj, "active_time", "1")),
                molang.compile(getMolang(obj, "sleep_time", "0")));
    }

    // ===== Runtime =====

    public static final class Runtime implements IEmitterComponent {
        private final MolangExpression activeTimeExpr;
        private final MolangExpression sleepTimeExpr;
        private float emitterAge;

        public Runtime(MolangExpression activeTime, MolangExpression sleepTime) {
            this.activeTimeExpr = activeTime;
            this.sleepTimeExpr = sleepTime;
        }

        @Override
        public void apply(ParticleEmitterInstance emitter) {
            this.emitterAge = 0;
            emitter.setRemoved(false);
            emitter.setSleeping(false);
            emitter.setActive(true);
            emitter.setEmitterAge(0);

            MolangContext<?> ctx = emitter.getMolang().getContext();
            emitter.setEmitterLifetime((float) activeTimeExpr.evaluate(ctx));
            emitter.bindContextAndCurves();
        }

        @Override
        public void update(ParticleEmitterInstance emitter) {
            emitterAge += emitter.getDt();
            emitter.setEmitterAge(emitterAge);
            emitter.bindContextAndCurves(); // 后续组件需要更新后的 molang

            MolangContext<?> ctx = emitter.getMolang().getContext();
            float lifetime = (float) activeTimeExpr.evaluate(ctx);

            if (emitterAge >= lifetime) {
                emitter.fireExpirationEvents();
                emitter.setSleeping(true);
                emitter.setActive(false);

                float st = (float) sleepTimeExpr.evaluate(ctx);
                if (st <= 0) {
                    // 立即重启：需要重置所有 emitter 级 Runtime 组件，
                    // 避免上一周期的状态（如 spawnAccumulator、timeline 索引）污染新周期
                    for (IEmitterComponent c : emitter.getEmitterUpdateComponents()) {
                        c.apply(emitter);
                    }
                    emitter.fireCreationEvents();
                } else {
                    emitter.setSleepTimer(st);
                }
            }
        }
    }
}
