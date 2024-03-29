package com.ggstudios.env;

import com.ggstudios.types.TypeDecl;

public class PrimitiveClass extends Class {
    private Class arrayClass;
    private String typeName;
    private boolean simple;

    public PrimitiveClass(String typeName, boolean simple) {
        this.typeName = typeName;
        arrayClass = new ArrayClass(this);

        setEnvironment(new ClassEnvironment(this));
        this.simple = simple;
    }

    /**
     * This is for the very special case of the primitive class int as it is a circular reference...
     */
    void forceReinitializeArrayClass() {
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
    public String getName() {
        return typeName;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public Package getPackage() {
        return BaseEnvironment.PRIMITIVE_PACKAGE;
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

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isSimple() {
        return simple;
    }
}
