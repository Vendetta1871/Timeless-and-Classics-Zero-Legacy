package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import org.jetbrains.annotations.Nullable;

/**
 * 粒子效果事件节点。触发时创建子发射器。
 *
 * @param effect              子粒子效果标识符
 * @param type                发射器类型
 * @param preEffectExpression 创建子发射器前执行的 Molang 表达式（原始字符串，调试用）
 * @param compiledPreEffect   预编译的 pre_effect_expression
 */
public final class ParticleEffectEvent implements IEventNode {
    private final String effect;
    private final Type type;
    private final String preEffectExpression;
    private final MolangExpression compiledPreEffect;

    public ParticleEffectEvent(String effect, Type type, String preEffectExpression, MolangExpression compiledPreEffect) {
        this.effect = effect;
        this.type = type;
        this.preEffectExpression = preEffectExpression;
        this.compiledPreEffect = compiledPreEffect;
    }

    public String effect() {
        return effect;
    }

    public Type type() {
        return type;
    }

    public String preEffectExpression() {
        return preEffectExpression;
    }

    public MolangExpression compiledPreEffect() {
        return compiledPreEffect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleEffectEvent)) return false;
        ParticleEffectEvent that = (ParticleEffectEvent) o;
        return java.util.Objects.equals(effect, that.effect) && java.util.Objects.equals(type, that.type) && java.util.Objects.equals(preEffectExpression, that.preEffectExpression) && java.util.Objects.equals(compiledPreEffect, that.compiledPreEffect);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(effect, type, preEffectExpression, compiledPreEffect);
    }

    @Override
    public String toString() {
        return "ParticleEffectEvent[" + "effect=" + effect + ", " + "type=" + type + ", " + "preEffectExpression=" + preEffectExpression + ", " + "compiledPreEffect=" + compiledPreEffect + "]";
    }


    public enum Type {
        EMITTER,
        EMITTER_BOUND,
        PARTICLE,
        PARTICLE_WITH_VELOCITY;

        public static Type fromString(String str) {
            return switch (str.toLowerCase()) {
                case "emitter_bound" -> EMITTER_BOUND;
                case "particle" -> PARTICLE;
                case "particle_with_velocity" -> PARTICLE_WITH_VELOCITY;
                default -> EMITTER;
            };
        }
    }

    public static ParticleEffectEvent of(String effect, Type type, @Nullable String preExpr, ParticleMolangEnvironment molang) {
        MolangExpression compiled = (preExpr != null && !preExpr.isEmpty())
                ? molang.compile(preExpr) : null;
        return new ParticleEffectEvent(effect, type, preExpr, compiled);
    }
}
