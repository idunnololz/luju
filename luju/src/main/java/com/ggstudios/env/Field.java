package com.ggstudios.env;

import com.ggstudios.types.VarDecl;

public class Field {
    protected Class declaringClass;
    protected VarDecl varDecl;

    protected Class type;
    protected String name;

    private int modifiers;
    private boolean initialized = false;

    protected Field() {}

    public Field(Class declaringClass, VarDecl vDecl, Environment env) {
        this.declaringClass = declaringClass;
        this.varDecl = vDecl;

        type = env.lookupClazz(vDecl.getType());
        name = varDecl.getName();
        modifiers = vDecl.getModifiers();
    }

    public Field(Class declaringClass, Class type, String name, int modifiers) {
        this.declaringClass = declaringClass;

        this.type = type;
        this.name = name;
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public Class getDeclaringClass() {
        return declaringClass;
    }

    public VarDecl getVarDecl() {
        return varDecl;
    }

    public boolean isLiteral() { return false; }

    public int getModifiers() { return modifiers; }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        initialized = true;
    }
}
