package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.curve;

/**
 * Bezier Chain 曲线节点。
 *
 * @param value 节点值
 * @param slope 节点斜率
 */
public final class CurveNode {
    private final float value;
    private final float slope;

    public CurveNode(float value, float slope) {
        this.value = value;
        this.slope = slope;
    }

    public float value() {
        return value;
    }

    public float slope() {
        return slope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurveNode)) return false;
        CurveNode that = (CurveNode) o;
        return java.util.Objects.equals(value, that.value) && java.util.Objects.equals(slope, that.slope);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value, slope);
    }

    @Override
    public String toString() {
        return "CurveNode[" + "value=" + value + ", " + "slope=" + slope + "]";
    }

}
