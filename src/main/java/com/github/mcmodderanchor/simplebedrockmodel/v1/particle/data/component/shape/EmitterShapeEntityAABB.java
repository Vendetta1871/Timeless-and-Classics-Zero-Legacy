package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import org.jetbrains.annotations.Nullable;
import java.util.Random;

public final class EmitterShapeEntityAABB implements EmitterShape {
    private final MolangExpression[] offset;
    private final boolean surfaceOnly;
    private final MolangExpression[] direction;
    private final DirectionMode directionMode;

    public EmitterShapeEntityAABB(MolangExpression[] offset, boolean surfaceOnly, MolangExpression[] direction, DirectionMode directionMode) {
        this.offset = offset;
        this.surfaceOnly = surfaceOnly;
        this.direction = direction;
        this.directionMode = directionMode;
    }

    public MolangExpression[] offset() {
        return offset;
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
        if (!(o instanceof EmitterShapeEntityAABB)) return false;
        EmitterShapeEntityAABB that = (EmitterShapeEntityAABB) o;
        return java.util.Objects.equals(offset, that.offset) && java.util.Objects.equals(surfaceOnly, that.surfaceOnly) && java.util.Objects.equals(direction, that.direction) && java.util.Objects.equals(directionMode, that.directionMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(offset, surfaceOnly, direction, directionMode);
    }

    @Override
    public String toString() {
        return "EmitterShapeEntityAABB[" + "offset=" + offset + ", " + "surfaceOnly=" + surfaceOnly + ", " + "direction=" + direction + ", " + "directionMode=" + directionMode + "]";
    }

    @Override
    public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
        float ox = (float) offset[0].evaluate(ctx);
        float oy = (float) offset[1].evaluate(ctx);
        float oz = (float) offset[2].evaluate(ctx);
        p.x = ox + (random.nextFloat() * 2 - 1) * 0.5f;
        p.y = oy + random.nextFloat();
        p.z = oz + (random.nextFloat() * 2 - 1) * 0.5f;
    }
}
