package com.ggstudios.types;

public abstract class VariableExpression extends Expression {
    public abstract String getName();

    public VariableExpression() {
        setType(VARIABLE_EXPRESSION);
    }

    @Override
    public String toString() {
        return getName();
    }
}
