package com.ggstudios.env;

import com.ggstudios.types.VarDecl;

public class Field {
    protected Clazz declaringClass;
    protected VarDecl varDecl;

    protected Clazz type;
    protected String name;

    protected Field() {}

    public Field(Clazz declaringClass, VarDecl vDecl, Environment env) {
        this.declaringClass = declaringClass;
        this.varDecl = vDecl;

        type = env.lookupClazz(vDecl.getType());
        name = varDecl.getName();
    }

    public Field(Clazz declaringClass, Clazz type, Environment env) {
        this.declaringClass = declaringClass;

        type = type;
        name = varDecl.getName();
    }

    public String getName() {
        return name;
    }

    public Clazz getType() {
        return type;
    }

    public Clazz getDeclaringClass() {
        return declaringClass;
    }

    public VarDecl getVarDecl() {
        return varDecl;
    }

    public boolean isLiteral() { return false; }
}
