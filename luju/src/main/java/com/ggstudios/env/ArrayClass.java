package com.ggstudios.env;

import com.ggstudios.types.TypeDecl;

import java.util.HashMap;
import java.util.Map;

public class ArrayClass extends Class {
    private static final String ARRAY_LABEL_SUFFIX = "#Array";

    private Class baseClass;
    private Environment env;
    private Map<Field, Integer> fieldToIndex = new HashMap<>();

    public ArrayClass(Class c) {
        Field lengthField = new Field(this, BaseEnvironment.TYPE_INT, "length", Modifier.FINAL);
        putField(lengthField);
        fieldToIndex.put(lengthField, 2);

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

    public String getUniqueLabel() {
        return baseClass.getUniqueLabel() + ARRAY_LABEL_SUFFIX;
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
    public Package getPackage() {
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

    @Override
    public int getFieldIndex(Field f) {
        Integer i;
        if ((i = fieldToIndex.get(f)) == null) {
            throw new RuntimeException("Field " + f.getName() + " index not found in class " + getCanonicalName());
        }
        return i;
    }

    public String getVtableLabel() {
        return BaseEnvironment.TYPE_OBJECT.getVtableLabel();
    }
}
