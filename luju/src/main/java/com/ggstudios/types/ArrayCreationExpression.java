package com.ggstudios.types;

public class ArrayCreationExpression extends Expression {
    private ReferenceType typeExpr;
    private Expression dimExpr;

    public ArrayCreationExpression() {
        setType(ARRAY_CREATION_EXPRESSION);
    }

    public ReferenceType getTypeExpr() {
        return typeExpr;
    }

    public void setTypeExpr(ReferenceType typeExpr) {
        this.typeExpr = typeExpr;
    }

    public Expression getDimExpr() {
        return dimExpr;
    }

    public void setDimExpr(Expression dimExpr) {
        this.dimExpr = dimExpr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(typeExpr.toString());
        sb.append("[");
        sb.append(dimExpr.toString());
        sb.append("]");
        return sb.toString();
    }
}
