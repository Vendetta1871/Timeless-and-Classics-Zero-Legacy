package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.tinting;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponentDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import org.jetbrains.annotations.Nullable;

public final class ParticleTintingStatic implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression r;
    private final MolangExpression g;
    private final MolangExpression b;
    private final MolangExpression a;

    public ParticleTintingStatic(MolangExpression r, MolangExpression g, MolangExpression b, MolangExpression a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public MolangExpression r() {
        return r;
    }

    public MolangExpression g() {
        return g;
    }

    public MolangExpression b() {
        return b;
    }

    public MolangExpression a() {
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleTintingStatic)) return false;
        ParticleTintingStatic that = (ParticleTintingStatic) o;
        return java.util.Objects.equals(r, that.r) && java.util.Objects.equals(g, that.g) && java.util.Objects.equals(b, that.b) && java.util.Objects.equals(a, that.a);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(r, g, b, a);
    }

    @Override
    public String toString() {
        return "ParticleTintingStatic[" + "r=" + r + ", " + "g=" + g + ", " + "b=" + b + ", " + "a=" + a + "]";
    }


    @Override public int order() { return 210; }
    @Override public boolean requireUpdate() { return false; }

    @Override
    public void apply(ParticleInstance p) {
        if (p.emitter == null) return;
        MolangContext<?> ctx = p.emitter.getMolang().getContext();
        p.r = (float) r.evaluate(ctx);
        p.g = (float) g.evaluate(ctx);
        p.b = (float) b.evaluate(ctx);
        p.a = a != null ? (float) a.evaluate(ctx) : 1f;
    }
}
