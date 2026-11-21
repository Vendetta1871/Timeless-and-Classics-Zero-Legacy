package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.motion;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponentDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getBoolean;
import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;
import com.google.common.collect.ImmutableList;

/**
 * 粒子碰撞运动组件。对应 "minecraft:particle_motion_collision"。
 */
public final class ParticleMotionCollision implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression enabled;
    private final float collisionDrag;
    private final float coefficientOfRestitution;
    private final float collisionRadius;
    private final boolean expireOnContact;
    private final List<CollisionEvent> events;

    public ParticleMotionCollision(MolangExpression enabled, float collisionDrag, float coefficientOfRestitution, float collisionRadius, boolean expireOnContact, List<CollisionEvent> events) {
        this.enabled = enabled;
        this.collisionDrag = collisionDrag;
        this.coefficientOfRestitution = coefficientOfRestitution;
        this.collisionRadius = collisionRadius;
        this.expireOnContact = expireOnContact;
        this.events = events;
    }

    public MolangExpression enabled() {
        return enabled;
    }

    public float collisionDrag() {
        return collisionDrag;
    }

    public float coefficientOfRestitution() {
        return coefficientOfRestitution;
    }

    public float collisionRadius() {
        return collisionRadius;
    }

    public boolean expireOnContact() {
        return expireOnContact;
    }

    public List<CollisionEvent> events() {
        return events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleMotionCollision)) return false;
        ParticleMotionCollision that = (ParticleMotionCollision) o;
        return java.util.Objects.equals(enabled, that.enabled) && java.util.Objects.equals(collisionDrag, that.collisionDrag) && java.util.Objects.equals(coefficientOfRestitution, that.coefficientOfRestitution) && java.util.Objects.equals(collisionRadius, that.collisionRadius) && java.util.Objects.equals(expireOnContact, that.expireOnContact) && java.util.Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(enabled, collisionDrag, coefficientOfRestitution, collisionRadius, expireOnContact, events);
    }

    @Override
    public String toString() {
        return "ParticleMotionCollision[" + "enabled=" + enabled + ", " + "collisionDrag=" + collisionDrag + ", " + "coefficientOfRestitution=" + coefficientOfRestitution + ", " + "collisionRadius=" + collisionRadius + ", " + "expireOnContact=" + expireOnContact + ", " + "events=" + events + "]";
    }


    @Override public int order() { return 350; }
    @Override public boolean requireUpdate() { return enabled != null || !events.isEmpty(); }

    public static final class CollisionEvent {
        private final String event;
        private final float minSpeed;

        public CollisionEvent(String event, float minSpeed) {
            this.event = event;
            this.minSpeed = minSpeed;
        }

        public String event() {
            return event;
        }

        public float minSpeed() {
            return minSpeed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CollisionEvent)) return false;
            CollisionEvent that = (CollisionEvent) o;
            return java.util.Objects.equals(event, that.event) && java.util.Objects.equals(minSpeed, that.minSpeed);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(event, minSpeed);
        }

        @Override
        public String toString() {
            return "CollisionEvent[" + "event=" + event + ", " + "minSpeed=" + minSpeed + "]";
        }
}

    public static ParticleMotionCollision fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        MolangExpression enabled = obj.has("enabled") ? molang.compile(getMolang(obj, "enabled", "1")) : null;
        float drag = obj.has("collision_drag") ? obj.get("collision_drag").getAsFloat() : 0;
        float restitution = obj.has("coefficient_of_restitution") ? obj.get("coefficient_of_restitution").getAsFloat() : 0;
        float radius = obj.has("collision_radius") ? obj.get("collision_radius").getAsFloat() : 0;
        boolean expire = getBoolean(obj, "expire_on_contact", false);
        return new ParticleMotionCollision(enabled, drag, restitution, radius, expire, parseEvents(obj));
    }

    private static List<CollisionEvent> parseEvents(JsonObject obj) {
        if (!obj.has("events") || !obj.get("events").isJsonArray()) return ImmutableList.of();
        JsonArray arr = obj.getAsJsonArray("events");
        List<CollisionEvent> list = new ArrayList<>();
        for (JsonElement e : arr) {
            if (e.isJsonObject()) {
                JsonObject eventObj = e.getAsJsonObject();
                String event = eventObj.has("event") ? eventObj.get("event").getAsString() : "";
                float minSpeed = eventObj.has("min_speed") ? eventObj.get("min_speed").getAsFloat() : 2f;
                if (!event.isEmpty()) list.add(new CollisionEvent(event, minSpeed));
            }
        }
        return list;
    }
}
