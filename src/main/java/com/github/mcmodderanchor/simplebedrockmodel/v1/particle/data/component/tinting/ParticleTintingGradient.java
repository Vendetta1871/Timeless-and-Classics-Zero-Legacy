package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.tinting;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponentDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;

public final class ParticleTintingGradient implements IParticleComponentDefinition, IParticleComponent {
    private final MolangExpression interpolant;
    private final float[] stops;
    private final MolangExpression[][] colors;

    public ParticleTintingGradient(MolangExpression interpolant, float[] stops, MolangExpression[][] colors) {
        this.interpolant = interpolant;
        this.stops = stops;
        this.colors = colors;
    }

    public MolangExpression interpolant() {
        return interpolant;
    }

    public float[] stops() {
        return stops;
    }

    public MolangExpression[][] colors() {
        return colors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleTintingGradient)) return false;
        ParticleTintingGradient that = (ParticleTintingGradient) o;
        return java.util.Objects.equals(interpolant, that.interpolant) && java.util.Objects.equals(stops, that.stops) && java.util.Objects.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(interpolant, stops, colors);
    }

    @Override
    public String toString() {
        return "ParticleTintingGradient[" + "interpolant=" + interpolant + ", " + "stops=" + stops + ", " + "colors=" + colors + "]";
    }


    @Override public int order() { return 210; }
    @Override public boolean requireUpdate() { return true; }

    @Override
    public void apply(ParticleInstance p) {
        updateColor(p);
    }

    @Override
    public void update(ParticleInstance p) {
        updateColor(p);
    }

    private void updateColor(ParticleInstance p) {
        if (p.emitter == null || colors.length == 0) return;
        MolangContext<?> ctx = p.emitter.getMolang().getContext();
        float t = (float) interpolant.evaluate(ctx);

        if (colors.length == 1) {
            p.r = (float) colors[0][0].evaluate(ctx);
            p.g = (float) colors[0][1].evaluate(ctx);
            p.b = (float) colors[0][2].evaluate(ctx);
            p.a = (float) colors[0][3].evaluate(ctx);
            return;
        }

        t = Math.max(stops[0], Math.min(stops[stops.length - 1], t));
        int idx = 0;
        for (int i = 0; i < stops.length - 1; i++) {
            if (t >= stops[i]) idx = i;
        }
        if (idx >= colors.length - 1) idx = colors.length - 2;

        float segStart = stops[idx], segEnd = stops[idx + 1];
        float frac = segEnd > segStart ? (t - segStart) / (segEnd - segStart) : 0;
        frac = Math.max(0, Math.min(1, frac));

        MolangExpression[] c0 = colors[idx], c1 = colors[idx + 1];
        p.r = lerp((float) c0[0].evaluate(ctx), (float) c1[0].evaluate(ctx), frac);
        p.g = lerp((float) c0[1].evaluate(ctx), (float) c1[1].evaluate(ctx), frac);
        p.b = lerp((float) c0[2].evaluate(ctx), (float) c1[2].evaluate(ctx), frac);
        p.a = lerp((float) c0[3].evaluate(ctx), (float) c1[3].evaluate(ctx), frac);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
