package com.ggstudios.types;

import com.ggstudios.luju.Token;

public class UnaryExpression extends Expression {
    private Token op;
    private Expression expression;

    public Token getOp() {
        return op;
    }

    public void setOp(Token op) {
        this.op = op;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return op.getRaw() + " " + expression.toString();
    }
}
