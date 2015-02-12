package com.ggstudios.types;

import com.ggstudios.luju.Token;

import java.util.ArrayList;
import java.util.List;

public class FieldVariable extends Variable {
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
        sb.append(prefixExpr.toString());
        sb.append(".");
        sb.append(id.getRaw());
        return sb.toString();
    }
}