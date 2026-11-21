package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import org.jetbrains.annotations.Nullable;
import java.util.Random;

public final class EmitterShapePoint implements EmitterShape {
    private final MolangExpression[] offset;
    private final MolangExpression[] direction;
    private final DirectionMode directionMode;

    public EmitterShapePoint(MolangExpression[] offset, MolangExpression[] direction, DirectionMode directionMode) {
        this.offset = offset;
        this.direction = direction;
        this.directionMode = directionMode;
    }

    public MolangExpression[] offset() {
        return offset;
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
        if (!(o instanceof EmitterShapePoint)) return false;
        EmitterShapePoint that = (EmitterShapePoint) o;
        return java.util.Objects.equals(offset, that.offset) && java.util.Objects.equals(direction, that.direction) && java.util.Objects.equals(directionMode, that.directionMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(offset, direction, directionMode);
    }

    @Override
    public String toString() {
        return "EmitterShapePoint[" + "offset=" + offset + ", " + "direction=" + direction + ", " + "directionMode=" + directionMode + "]";
    }

    @Override
    public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
        p.x = (float) offset[0].evaluate(ctx);
        p.y = (float) offset[1].evaluate(ctx);
        p.z = (float) offset[2].evaluate(ctx);
    }
}
