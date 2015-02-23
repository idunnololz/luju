package com.ggstudios.types;

public class VarInitDecl extends VarDecl {
    private Expression expr;

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public void toPrettyString(StringBuilder sb, int level) {
        super.toPrettyString(sb, level);
        sb.setLength(sb.length() - 1);
        sb.append("; Expr(=): ");
        sb.append(expr.toString());
        sb.append(")");
    }
}
