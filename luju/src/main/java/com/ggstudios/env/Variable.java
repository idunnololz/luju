package com.ggstudios.env;

import com.ggstudios.asm.RegisterExpression;
import com.ggstudios.error.TypeException;
import com.ggstudios.types.VarDecl;

public class Variable extends Field {

    private boolean initialized = false;

    private Method declaringMethod;
    private Constructor declaringConstructor;
    private RegisterExpression address;

    public Variable(Method declaringMethod, VarDecl vDecl, Environment env) {
        super(null, vDecl, env);

        if (declaringMethod == null) {
            throw new RuntimeException("Declaring method is null!");
        }

        this.declaringMethod = declaringMethod;
    }

    public Variable(Constructor declaringConstructor, VarDecl vDecl, Environment env) {
        super(null, vDecl, env);

        if (declaringConstructor == null) {
            throw new RuntimeException("Declaring constructor is null!");
        }

        this.declaringConstructor = declaringConstructor;
    }

    @Override
    public Class getDeclaringClass() {
        throw new TypeException(declaringMethod.getDeclaringClass().getFileName(), getVarDecl(),
                String.format("Local variable '%s' does not have a declaring class.", getName()));
    }

    public Method getDeclaringMethod() {
        return declaringMethod;
    }

    public Constructor getDeclaringConstructor() {
        return declaringConstructor;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setAddress(RegisterExpression address) {
        this.address = address;
    }

    public RegisterExpression getAddress() {
        return address;
    }
}
