package com.ggstudios.types;

import java.util.List;

public class MethodExpression extends Expression {
    private VariableExpression methodIdExpr;
    private List<Expression> argList;

    public MethodExpression() {
        setType(METHOD_EXPRESSION);
    }

    public List<Expression> getArgList() {
        return argList;
    }

    public void setArgList(List<Expression> argList) {
        this.argList = argList;
    }

    public VariableExpression getMethodIdExpr() {
        return methodIdExpr;
    }

    public void setMethodIdExpr(VariableExpression methodIdExpr) {
        this.methodIdExpr = methodIdExpr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodIdExpr.toString());
        sb.append("(");
        if (!argList.isEmpty()) {
            for (Expression a : argList) {
                sb.append(a.toString());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append(")");
        return sb.toString();
    }
}
