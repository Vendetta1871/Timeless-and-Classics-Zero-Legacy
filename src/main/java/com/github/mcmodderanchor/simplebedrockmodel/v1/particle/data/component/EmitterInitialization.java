package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

public final class EmitterInitialization implements IEmitterComponentDefinition, IEmitterComponent {
    private final MolangExpression creationExpression;
    private final MolangExpression perUpdateExpression;

    public EmitterInitialization(MolangExpression creationExpression, MolangExpression perUpdateExpression) {
        this.creationExpression = creationExpression;
        this.perUpdateExpression = perUpdateExpression;
    }

    public MolangExpression creationExpression() {
        return creationExpression;
    }

    public MolangExpression perUpdateExpression() {
        return perUpdateExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterInitialization)) return false;
        EmitterInitialization that = (EmitterInitialization) o;
        return java.util.Objects.equals(creationExpression, that.creationExpression) && java.util.Objects.equals(perUpdateExpression, that.perUpdateExpression);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(creationExpression, perUpdateExpression);
    }

    @Override
    public String toString() {
        return "EmitterInitialization[" + "creationExpression=" + creationExpression + ", " + "perUpdateExpression=" + perUpdateExpression + "]";
    }


    @Override
    public int order() {
        return 505;
    }

    @Override
    public boolean requireUpdate() {
        return perUpdateExpression != null;
    }

    @Override
    public void apply(ParticleEmitterInstance emitter) {
        if (creationExpression != null) {
            creationExpression.evaluate(emitter.getMolang().getContext());
        }
    }

    @Override
    public void update(ParticleEmitterInstance emitter) {
        if (perUpdateExpression != null) {
            perUpdateExpression.evaluate(emitter.getMolang().getContext());
        }
    }

    public static EmitterInitialization fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        MolangExpression creation = null;
        MolangExpression perUpdate = null;
        if (obj.has("creation_expression")) {
            String expr = getMolang(obj, "creation_expression", "");
            if (!expr.isEmpty()) creation = molang.compile(expr);
        }
        if (obj.has("per_update_expression")) {
            String expr = getMolang(obj, "per_update_expression", "");
            if (!expr.isEmpty()) perUpdate = molang.compile(expr);
        }
        return new EmitterInitialization(creation, perUpdate);
    }
}
