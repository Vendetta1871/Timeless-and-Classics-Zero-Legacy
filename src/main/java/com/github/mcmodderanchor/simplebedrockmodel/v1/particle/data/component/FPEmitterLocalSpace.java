package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getBoolean;

public final class FPEmitterLocalSpace implements IEmitterComponentDefinition, IEmitterComponent {
    private final boolean position;
    private final boolean rotation;
    private final boolean velocity;
    private final boolean toWorld;

    public FPEmitterLocalSpace(boolean position, boolean rotation, boolean velocity, boolean toWorld) {
        this.position = position;
        this.rotation = rotation;
        this.velocity = velocity;
        this.toWorld = toWorld;
    }

    public boolean position() {
        return position;
    }

    public boolean rotation() {
        return rotation;
    }

    public boolean velocity() {
        return velocity;
    }

    public boolean toWorld() {
        return toWorld;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FPEmitterLocalSpace)) return false;
        FPEmitterLocalSpace that = (FPEmitterLocalSpace) o;
        return java.util.Objects.equals(position, that.position) && java.util.Objects.equals(rotation, that.rotation) && java.util.Objects.equals(velocity, that.velocity) && java.util.Objects.equals(toWorld, that.toWorld);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(position, rotation, velocity, toWorld);
    }

    @Override
    public String toString() {
        return "FPEmitterLocalSpace[" + "position=" + position + ", " + "rotation=" + rotation + ", " + "velocity=" + velocity + ", " + "toWorld=" + toWorld + "]";
    }


    @Override
    public int order() {
        return 410;
    }

    @Override
    public void apply(ParticleEmitterInstance emitter) {
        emitter.enableFPMode(position, position && rotation, velocity, toWorld);
    }

    public static FPEmitterLocalSpace fromJson(JsonObject obj) {
        return new FPEmitterLocalSpace(
                getBoolean(obj, "position", false),
                getBoolean(obj, "rotation", false),
                getBoolean(obj, "velocity", false),
                getBoolean(obj, "to_world", false));
    }
}
