package com.ggstudios.types;

import com.ggstudios.luju.Token;

import java.util.List;

public class MethodExpression extends Expression {
    private Expression prefixExpr;
    private String methodName;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (prefixExpr != null) {
            sb.append(prefixExpr.toString());
            sb.append(".");
        }
        sb.append(methodName);
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

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setPrefixExpression(Expression prefixExpression) {
        this.prefixExpr = prefixExpression;
    }

    public Expression getPrefixExpression() {
        return prefixExpr;
    }
}
