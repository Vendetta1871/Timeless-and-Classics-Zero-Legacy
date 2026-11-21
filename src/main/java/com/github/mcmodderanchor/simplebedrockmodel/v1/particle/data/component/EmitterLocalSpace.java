package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getBoolean;

/**
 * 发射器局部空间组件。对应 "minecraft:emitter_local_space"。
 */
public final class EmitterLocalSpace implements IEmitterComponentDefinition, IEmitterComponent {
    private final boolean position;
    private final boolean rotation;
    private final boolean velocity;

    public EmitterLocalSpace(boolean position, boolean rotation, boolean velocity) {
        this.position = position;
        this.rotation = rotation;
        this.velocity = velocity;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterLocalSpace)) return false;
        EmitterLocalSpace that = (EmitterLocalSpace) o;
        return java.util.Objects.equals(position, that.position) && java.util.Objects.equals(rotation, that.rotation) && java.util.Objects.equals(velocity, that.velocity);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(position, rotation, velocity);
    }

    @Override
    public String toString() {
        return "EmitterLocalSpace[" + "position=" + position + ", " + "rotation=" + rotation + ", " + "velocity=" + velocity + "]";
    }


    @Override
    public int order() {
        return 400;
    }

    @Override
    public void apply(ParticleEmitterInstance emitter) {
        emitter.setLocalSpaceFlags(position, position && rotation, velocity);
    }

    public static EmitterLocalSpace fromJson(JsonObject obj) {
        return new EmitterLocalSpace(
                getBoolean(obj, "position", false),
                getBoolean(obj, "rotation", false),
                getBoolean(obj, "velocity", false));
    }
}
