package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import org.jetbrains.annotations.Nullable;
import java.util.Random;

public final class EmitterShapeSphere implements EmitterShape {
    private final MolangExpression[] offset;
    private final MolangExpression radius;
    private final boolean surfaceOnly;
    private final MolangExpression[] direction;
    private final DirectionMode directionMode;

    public EmitterShapeSphere(MolangExpression[] offset, MolangExpression radius, boolean surfaceOnly, MolangExpression[] direction, DirectionMode directionMode) {
        this.offset = offset;
        this.radius = radius;
        this.surfaceOnly = surfaceOnly;
        this.direction = direction;
        this.directionMode = directionMode;
    }

    public MolangExpression[] offset() {
        return offset;
    }

    public MolangExpression radius() {
        return radius;
    }

    public boolean surfaceOnly() {
        return surfaceOnly;
    }

    public MolangExpression[] direction() {
        return direction;
    }

    public DirectionMode directionMode() {
        return directionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmitterShapeSphere)) return false;
        EmitterShapeSphere that = (EmitterShapeSphere) o;
        return java.util.Objects.equals(offset, that.offset) && java.util.Objects.equals(radius, that.radius) && java.util.Objects.equals(surfaceOnly, that.surfaceOnly) && java.util.Objects.equals(direction, that.direction) && java.util.Objects.equals(directionMode, that.directionMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(offset, radius, surfaceOnly, direction, directionMode);
    }

    @Override
    public String toString() {
        return "EmitterShapeSphere[" + "offset=" + offset + ", " + "radius=" + radius + ", " + "surfaceOnly=" + surfaceOnly + ", " + "direction=" + direction + ", " + "directionMode=" + directionMode + "]";
    }

    @Override
    public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
        float ox = (float) offset[0].evaluate(ctx);
        float oy = (float) offset[1].evaluate(ctx);
        float oz = (float) offset[2].evaluate(ctx);
        float r = (float) radius.evaluate(ctx);
        float theta = (float) (random.nextFloat() * Math.PI * 2);
        float phi = (float) (Math.acos(2 * random.nextFloat() - 1));
        float dist = surfaceOnly ? r : r * (float) Math.cbrt(random.nextFloat());
        p.x = ox + dist * (float) (Math.sin(phi) * Math.cos(theta));
        p.y = oy + dist * (float) Math.cos(phi);
        p.z = oz + dist * (float) (Math.sin(phi) * Math.sin(theta));
    }
}
