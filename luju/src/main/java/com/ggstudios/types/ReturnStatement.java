package com.ggstudios.types;

import com.ggstudios.utils.PrintUtils;

public class ReturnStatement extends Statement {
    private Expression optExpr;

    public ReturnStatement() {
        super(Statement.TYPE_RETURN);
    }

    @Override
    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
        sb.append(" (");
        if (optExpr != null) {
            sb.append("Expr: ");
            sb.append(optExpr.toString());
        }
        sb.append(")");
    }

    public void setExpression(Expression expr) {
        optExpr = expr;
    }
}