package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape.EmitterShape;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;

import java.util.Random;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.molangFromElement;

public final class ParticleInitialSpeed implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression speed;

    public ParticleInitialSpeed(MolangExpression speed) {
        this.speed = speed;
    }

    public MolangExpression speed() {
        return speed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleInitialSpeed)) return false;
        ParticleInitialSpeed that = (ParticleInitialSpeed) o;
        return java.util.Objects.equals(speed, that.speed);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(speed);
    }

    @Override
    public String toString() {
        return "ParticleInitialSpeed[" + "speed=" + speed + "]";
    }


    @Override
    public int order() {
        return -500;
    }

    @Override
    public void apply(ParticleInstance p) {
        if (p.emitter == null) return;
        MolangContext<?> ctx = p.emitter.getMolang().getContext();
        float s = (float) speed.evaluate(ctx);
        if (s == 0) return;
        EmitterShape shape = p.emitter.getDefinition().emitterPreset().find(EmitterShape.class);
        if (shape != null) {
            shape.applyDirection(p, ctx, new Random(), s);
        }
    }

    public static ParticleInitialSpeed fromJson(JsonElement value, ParticleMolangEnvironment molang) {
        return new ParticleInitialSpeed(molang.compile(molangFromElement(value, "0")));
    }
}
