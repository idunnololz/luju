package com.ggstudios.types;

import com.ggstudios.luju.Token;

public class BinaryExpression extends Expression {
    private Token op;
    private Expression leftExpr;
    private Expression rightExpr;

    public BinaryExpression() {
        setType(BINARY_EXPRESSION);
    }

    public Expression getLeftExpr() {
        return leftExpr;
    }

    public void setLeftExpr(Expression leftExpr) {
        this.leftExpr = leftExpr;
    }

    public Expression getRightExpr() {
        return rightExpr;
    }

    public void setRightExpr(Expression rightExpr) {
        this.rightExpr = rightExpr;
    }

    public Token getOp() {
        return op;
    }

    public void setOp(Token op) {
        this.op = op;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(leftExpr.toString());
        sb.append(' ');
        sb.append(op.getRaw());
        sb.append(' ');
        sb.append(rightExpr.toString());
        sb.append(')');
        return sb.toString();
    }
}
