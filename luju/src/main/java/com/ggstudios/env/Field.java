package com.ggstudios.env;

import com.ggstudios.types.VarDecl;
import com.ggstudios.utils.MapUtils;

import java.util.HashMap;

public class Field {
    private static final HashMap<String, Integer> nameCount = new HashMap<>();

    protected Class declaringClass;
    protected VarDecl varDecl;

    protected Class type;
    protected String name;
    protected String uniqueName;

    private int modifiers;
    private boolean initialized = false;

    public static void reset() {
        nameCount.clear();
    }

    protected Field() {}

    public Field(Class declaringClass, VarDecl vDecl, Environment env) {
        this.declaringClass = declaringClass;
        this.varDecl = vDecl;

        type = env.lookupClazz(vDecl.getType());
        name = varDecl.getName();
        modifiers = vDecl.getModifiers();

        generateUniqueName();
    }

    public Field(Class declaringClass, Class type, String name, int modifiers) {
        this.declaringClass = declaringClass;

        this.type = type;
        this.name = name;
        this.modifiers = modifiers;

        generateUniqueName();
    }

    private void generateUniqueName() {
        int count = MapUtils.getOrDefault(nameCount, name, 0);
        if (count++ != 0) {
            uniqueName = "?" + name + "@" + count;
        } else {
            uniqueName = "?" + name;
        }
        nameCount.put(name, count);
    }

    public String getUniqueName() {
        return uniqueName;
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
