package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import org.jetbrains.annotations.Nullable;
import java.util.Random;

public final class EmitterShapeDisc implements EmitterShape {
    private final MolangExpression[] offset;
    private final MolangExpression radius;
    private final PlaneNormal planeNormal;
    private final boolean surfaceOnly;
    private final MolangExpression[] direction;
    private final DirectionMode directionMode;

    public EmitterShapeDisc(MolangExpression[] offset, MolangExpression radius, PlaneNormal planeNormal, boolean surfaceOnly, MolangExpression[] direction, DirectionMode directionMode) {
        this.offset = offset;
        this.radius = radius;
        this.planeNormal = planeNormal;
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

    public PlaneNormal planeNormal() {
        return planeNormal;
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
        if (!(o instanceof EmitterShapeDisc)) return false;
        EmitterShapeDisc that = (EmitterShapeDisc) o;
        return java.util.Objects.equals(offset, that.offset) && java.util.Objects.equals(radius, that.radius) && java.util.Objects.equals(planeNormal, that.planeNormal) && java.util.Objects.equals(surfaceOnly, that.surfaceOnly) && java.util.Objects.equals(direction, that.direction) && java.util.Objects.equals(directionMode, that.directionMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(offset, radius, planeNormal, surfaceOnly, direction, directionMode);
    }

    @Override
    public String toString() {
        return "EmitterShapeDisc[" + "offset=" + offset + ", " + "radius=" + radius + ", " + "planeNormal=" + planeNormal + ", " + "surfaceOnly=" + surfaceOnly + ", " + "direction=" + direction + ", " + "directionMode=" + directionMode + "]";
    }

    @Override
    public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
        float ox = (float) offset[0].evaluate(ctx);
        float oy = (float) offset[1].evaluate(ctx);
        float oz = (float) offset[2].evaluate(ctx);
        float r = (float) radius.evaluate(ctx);
        float angle = (float) (random.nextFloat() * Math.PI * 2);
        float dist = surfaceOnly ? r : r * (float) Math.sqrt(random.nextFloat());
        float lx = dist * (float) Math.cos(angle);
        float lz = dist * (float) Math.sin(angle);
        switch (planeNormal) {
            case Y -> { p.x = ox + lx; p.y = oy; p.z = oz + lz; }
            case X -> { p.x = ox; p.y = oy + lx; p.z = oz + lz; }
            case Z -> { p.x = ox + lx; p.y = oy + lz; p.z = oz; }
            default -> { p.x = ox + lx; p.y = oy; p.z = oz + lz; }
        }
    }
}
