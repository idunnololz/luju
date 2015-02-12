package com.ggstudios.types;

import java.util.List;

public class ICreationExpression extends Expression {
    private String className;
    private List<Expression> argList;

    public List<Expression> getArgList() {
        return argList;
    }

    public void setArgList(List<Expression> argList) {
        this.argList = argList;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
