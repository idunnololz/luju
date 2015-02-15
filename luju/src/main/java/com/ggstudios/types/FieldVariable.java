package com.ggstudios.types;

import com.ggstudios.luju.Token;

public class FieldVariable extends VariableExpression {
    private Token id;
    private Expression prefixExpr;

    public FieldVariable(Token id, Expression prefixExpr) {
        this.id = id;
        this.prefixExpr = prefixExpr;

        setPos(prefixExpr);
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
//        sb.append("f(");
        sb.append(prefixExpr.toString());
        sb.append(".");
        sb.append(id.getRaw());
//        sb.append(")");
        return sb.toString();
    }
}
