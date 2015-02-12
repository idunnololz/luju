package com.ggstudios.types;

public abstract class Variable extends Expression {
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }
}
