package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public final class ParticleLifetimeKillPlane implements IParticleComponentDefinition, IParticleComponent {
    private final float a;
    private final float b;
    private final float c;
    private final float d;

    public ParticleLifetimeKillPlane(float a, float b, float c, float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public float a() {
        return a;
    }

    public float b() {
        return b;
    }

    public float c() {
        return c;
    }

    public float d() {
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleLifetimeKillPlane)) return false;
        ParticleLifetimeKillPlane that = (ParticleLifetimeKillPlane) o;
        return java.util.Objects.equals(a, that.a) && java.util.Objects.equals(b, that.b) && java.util.Objects.equals(c, that.c) && java.util.Objects.equals(d, that.d);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(a, b, c, d);
    }

    @Override
    public String toString() {
        return "ParticleLifetimeKillPlane[" + "a=" + a + ", " + "b=" + b + ", " + "c=" + c + ", " + "d=" + d + "]";
    }


    @Override
    public int order() {
        return 320;
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public void apply(ParticleInstance p) {
        p.insideKillPlane = (a * p.x + b * p.y + c * p.z + d) < 0;
    }

    @Override
    public void update(ParticleInstance p) {
        boolean nowInside = (a * p.x + b * p.y + c * p.z + d) < 0;
        if (nowInside != p.insideKillPlane) {
            p.alive = false;
        }
    }

    public static ParticleLifetimeKillPlane fromJson(JsonElement value) {
        if (!value.isJsonArray()) throw new JsonParseException("particle_lifetime_kill_plane must be a JSON array");
        JsonArray arr = value.getAsJsonArray();
        if (arr.size() != 4) throw new JsonParseException("particle_lifetime_kill_plane array must have 4 elements");
        return new ParticleLifetimeKillPlane(
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),
                arr.get(3).getAsFloat()
        );
    }
}
