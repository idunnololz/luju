package com.ggstudios.types;

import com.ggstudios.env.Field;

import java.util.List;

public abstract class VariableExpression extends Expression {
    private List<Field> proper;

    public abstract String getName();

    public VariableExpression() {
        setType(VARIABLE_EXPRESSION);
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setProper(List<Field> proper) {
        this.proper = proper;
    }

    public List<Field> getProper() {
        return proper;
    }
}
