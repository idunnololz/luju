package com.ggstudios.env;

import com.ggstudios.error.TypeException;
import com.ggstudios.types.VarDecl;

public class Variable extends Field {

    public Variable(Method declaringMethod, VarDecl vDecl, Environment env) {
        super(null, vDecl, env);
    }

    @Override
    public Clazz getDeclaringClass() {
        throw new TypeException(getDeclaringClass().getFileName(), getVarDecl(),
                String.format("Local variable '%s' does not have a declaring class.", getName()));
    }
}
