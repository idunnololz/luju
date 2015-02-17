package com.ggstudios.env;

import com.ggstudios.types.VarDecl;

public class Field {
    protected Class declaringClass;
    protected VarDecl varDecl;

    protected Class type;
    protected String name;

    protected Field() {}

    public Field(Class declaringClass, VarDecl vDecl, Environment env) {
        this.declaringClass = declaringClass;
        this.varDecl = vDecl;

        type = env.lookupClazz(vDecl.getType());
        name = varDecl.getName();
    }

    public Field(Class declaringClass, Class type, String name) {
        this.declaringClass = declaringClass;

        this.type = type;
        this.name = name;
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
}
