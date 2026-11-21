package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

public final class ParticleInitialSpin implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression rotation;
    private final MolangExpression rotationRate;

    public ParticleInitialSpin(MolangExpression rotation, MolangExpression rotationRate) {
        this.rotation = rotation;
        this.rotationRate = rotationRate;
    }

    public MolangExpression rotation() {
        return rotation;
    }

    public MolangExpression rotationRate() {
        return rotationRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleInitialSpin)) return false;
        ParticleInitialSpin that = (ParticleInitialSpin) o;
        return java.util.Objects.equals(rotation, that.rotation) && java.util.Objects.equals(rotationRate, that.rotationRate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(rotation, rotationRate);
    }

    @Override
    public String toString() {
        return "ParticleInitialSpin[" + "rotation=" + rotation + ", " + "rotationRate=" + rotationRate + "]";
    }


    @Override
    public int order() {
        return -490;
    }

    @Override
    public void apply(ParticleInstance p) {
        if (p.emitter == null) return;
        MolangContext<?> ctx = p.emitter.getMolang().getContext();
        p.rotation = (float) rotation.evaluate(ctx);
        p.rotationRate = (float) rotationRate.evaluate(ctx);
    }

    public static ParticleInitialSpin fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        return new ParticleInitialSpin(
                molang.compile(getMolang(obj, "rotation", "0")),
                molang.compile(getMolang(obj, "rotation_rate", "0")));
    }
}
