package com.ggstudios.env;

import com.ggstudios.types.TypeDecl;

public class ArrayClass extends Class {
    private Class baseClass;
    private Environment env;

    public ArrayClass(Class c) {
        putField(new Field(c, BaseEnvironment.TYPE_INT, "length"));
        baseClass = c;

        env = new ClassEnvironment(this);
    }

    public Class getSuperClass() {
        return baseClass;
    }

    public boolean isArray() {
        return true;
    }

    public String getCanonicalName() {
        return baseClass.getCanonicalName();
    }

    @Override
    public TypeDecl getClassDecl() {
        throw new IllegalStateException(String.format("Array type '%s' does not have a class declaration.",
                getCanonicalName()));
    }

    @Override
    public String getName() {
        return baseClass.getName();
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public String getPackage() {
        return baseClass.getPackage();
    }

    @Override
    public String getDeclType() {
        return "Class[]";
    }

    @Override
    public String getFileName() {
        return baseClass.getFileName();
    }

    @Override
    public void putMethod(Method method) {
        throw new IllegalStateException(String.format("Cannot add methods to array type '%s'.",
                getCanonicalName()));
    }

    @Override
    public Environment getEnvironment() {
        return env;
    }
}
