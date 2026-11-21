package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.motion;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponentDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;
import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolangArray3;

/**
 * 动力学运动。对应 "minecraft:particle_motion_dynamic"。
 */
public final class ParticleMotionDynamic implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression[] linearAcceleration;
    private final MolangExpression linearDragCoefficient;
    private final MolangExpression rotationAcceleration;
    private final MolangExpression rotationDragCoefficient;

    public ParticleMotionDynamic(MolangExpression[] linearAcceleration, MolangExpression linearDragCoefficient, MolangExpression rotationAcceleration, MolangExpression rotationDragCoefficient) {
        this.linearAcceleration = linearAcceleration;
        this.linearDragCoefficient = linearDragCoefficient;
        this.rotationAcceleration = rotationAcceleration;
        this.rotationDragCoefficient = rotationDragCoefficient;
    }

    public MolangExpression[] linearAcceleration() {
        return linearAcceleration;
    }

    public MolangExpression linearDragCoefficient() {
        return linearDragCoefficient;
    }

    public MolangExpression rotationAcceleration() {
        return rotationAcceleration;
    }

    public MolangExpression rotationDragCoefficient() {
        return rotationDragCoefficient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleMotionDynamic)) return false;
        ParticleMotionDynamic that = (ParticleMotionDynamic) o;
        return java.util.Objects.equals(linearAcceleration, that.linearAcceleration) && java.util.Objects.equals(linearDragCoefficient, that.linearDragCoefficient) && java.util.Objects.equals(rotationAcceleration, that.rotationAcceleration) && java.util.Objects.equals(rotationDragCoefficient, that.rotationDragCoefficient);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(linearAcceleration, linearDragCoefficient, rotationAcceleration, rotationDragCoefficient);
    }

    @Override
    public String toString() {
        return "ParticleMotionDynamic[" + "linearAcceleration=" + linearAcceleration + ", " + "linearDragCoefficient=" + linearDragCoefficient + ", " + "rotationAcceleration=" + rotationAcceleration + ", " + "rotationDragCoefficient=" + rotationDragCoefficient + "]";
    }


    @Override public int order() { return 300; }
    @Override public boolean requireUpdate() { return true; }

    @Override
    public void apply(ParticleInstance p) { /* 首次由 update 处理 */ }

    @Override
    public void update(ParticleInstance p) {
        if (p.emitter == null) return;
        applyLegacy(p, p.emitter.getMolang().getContext(), 1f / 20f);
    }

    /** 兼容旧调用方（Phase 4 后移除） */
    public void applyLegacy(ParticleInstance p, MolangContext<?> ctx, float dt) {
        if (linearAcceleration != null) {
            p.vx += (float) linearAcceleration[0].evaluate(ctx) * dt;
            p.vy += (float) linearAcceleration[1].evaluate(ctx) * dt;
            p.vz += (float) linearAcceleration[2].evaluate(ctx) * dt;
        }
        if (linearDragCoefficient != null) {
            float factor = Math.max(0, 1f - (float) linearDragCoefficient.evaluate(ctx) * dt);
            p.vx *= factor; p.vy *= factor; p.vz *= factor;
        }
        if (rotationAcceleration != null) {
            p.rotationRate += (float) rotationAcceleration.evaluate(ctx) * dt;
        }
        if (rotationDragCoefficient != null) {
            float rotFactor = Math.max(0, 1f - (float) rotationDragCoefficient.evaluate(ctx) * dt);
            p.rotationRate *= rotFactor;
        }
    }

    public static ParticleMotionDynamic fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return new ParticleMotionDynamic(
                obj.has("linear_acceleration")
                        ? compileArray3(molang, getMolangArray3(obj, "linear_acceleration", "0", "0", "0")) : null,
                obj.has("linear_drag_coefficient")
                        ? molang.compile(getMolang(obj, "linear_drag_coefficient", "0")) : null,
                obj.has("rotation_acceleration")
                        ? molang.compile(getMolang(obj, "rotation_acceleration", "0")) : null,
                obj.has("rotation_drag_coefficient")
                        ? molang.compile(getMolang(obj, "rotation_drag_coefficient", "0")) : null);
    }

    private static MolangExpression[] compileArray3(ParticleMolangEnvironment molang, String[] exprs) {
        return new MolangExpression[]{molang.compile(exprs[0]), molang.compile(exprs[1]), molang.compile(exprs[2])};
    }
}
