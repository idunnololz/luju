package com.ggstudios.env;

import com.ggstudios.types.TypeDecl;

public class PrimitiveClass extends Class {
    private Class arrayClass;
    private String typeName;

    public PrimitiveClass(String typeName) {
        this.typeName = typeName;
        arrayClass = new ArrayClass(this);
    }

    @Override
    public Class getArrayClass() {
        return arrayClass;
    }

    @Override
    public TypeDecl getClassDecl() {
        throw new IllegalStateException(String.format("Primitive type '%s' does not have a class declaration.",
                getName()));
    }

    @Override
    public void resolveSelf(Environment env) {}

    @Override
    public String getName() {
        return typeName;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public String getPackage() {
        return "";
    }

    @Override
    public String getDeclType() {
        return "Primitive";
    }

    @Override
    public String getCanonicalName() {
        return typeName;
    }

    @Override
    public String getFileName() {
        return "";
    }

    @Override
    public void putMethod(Method method) {
        throw new IllegalStateException(String.format("Primitive type '%s' cannot have methods.",
                getName()));
    }
}
