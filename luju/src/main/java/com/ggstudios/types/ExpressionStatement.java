package com.ggstudios.types;

import com.ggstudios.utils.Print;
import com.ggstudios.utils.PrintUtils;

public class ExpressionStatement extends Statement {

    private Expression expr;

    public ExpressionStatement(Expression expr) {
        super(Statement.TYPE_EXPRESSION);
        setPos(expr);
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append('s');
        expr.toPrettyString(sb, 0);
    }

    @Override
    public String toString() {
        return expr.toString();
    }
}
