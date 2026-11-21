package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;

/**
 * Molang 表达式事件节点。触发时执行 Molang 表达式。
 *
 * @param expression         原始表达式字符串（调试用）
 * @param compiledExpression 预编译的表达式
 */
public final class MolangExpressionEvent implements IEventNode {
    private final String expression;
    private final MolangExpression compiledExpression;

    public MolangExpressionEvent(String expression, MolangExpression compiledExpression) {
        this.expression = expression;
        this.compiledExpression = compiledExpression;
    }

    public String expression() {
        return expression;
    }

    public MolangExpression compiledExpression() {
        return compiledExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MolangExpressionEvent)) return false;
        MolangExpressionEvent that = (MolangExpressionEvent) o;
        return java.util.Objects.equals(expression, that.expression) && java.util.Objects.equals(compiledExpression, that.compiledExpression);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(expression, compiledExpression);
    }

    @Override
    public String toString() {
        return "MolangExpressionEvent[" + "expression=" + expression + ", " + "compiledExpression=" + compiledExpression + "]";
    }


    public static MolangExpressionEvent of(String expression, ParticleMolangEnvironment molang) {
        if (expression == null || expression.isEmpty()) {
            return new MolangExpressionEvent("", MolangExpression.constant(0));
        }
        return new MolangExpressionEvent(expression, molang.compile(expression));
    }
}
