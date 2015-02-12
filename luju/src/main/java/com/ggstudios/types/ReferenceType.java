package com.ggstudios.types;

public class ReferenceType extends Expression {
    private String type;

    public ReferenceType(NameVariable nVar, boolean isArray) {
        type = nVar.getName();
        if (isArray) {
            type += "[]";
        }
    }

    public ReferenceType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
