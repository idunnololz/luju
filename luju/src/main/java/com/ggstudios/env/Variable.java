package com.ggstudios.env;

import com.ggstudios.error.TypeException;
import com.ggstudios.types.VarDecl;

public class Variable extends Field {

    private boolean initialized = false;

    public Variable(Method declaringMethod, VarDecl vDecl, Environment env) {
        super(null, vDecl, env);
    }

    @Override
    public Class getDeclaringClass() {
        throw new TypeException(getDeclaringClass().getFileName(), getVarDecl(),
                String.format("Local variable '%s' does not have a declaring class.", getName()));
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
