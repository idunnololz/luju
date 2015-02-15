package com.ggstudios.env;

import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.TypeDecl;
import com.ggstudios.types.VarDecl;

public class PrimitiveClazz extends Clazz {
    private Clazz arrayClass;
    private String typeName;

    public PrimitiveClazz(String typeName) {
        this.typeName = typeName;
        arrayClass = new ArrayClazz(this);
    }

    @Override
    public Clazz getArrayClass() {
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
