package com.ggstudios.types;

public class FieldInitDecl extends FieldDecl {
    private Expression expr;

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }
}
