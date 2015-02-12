package com.ggstudios.types;

public class ArrayAccessExpression extends Expression {
    private Expression arrayExpr;
    private Expression indexExpr;

    public Expression getArrayExpr() {
        return arrayExpr;
    }

    public void setArrayExpr(Expression arrayExpr) {
        this.arrayExpr = arrayExpr;
    }

    public Expression getIndexExpr() {
        return indexExpr;
    }

    public void setIndexExpr(Expression indexExpr) {
        this.indexExpr = indexExpr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(arrayExpr.toString());
        sb.append('[');
        sb.append(indexExpr.toString());
        sb.append(']');
        return sb.toString();
    }
}
