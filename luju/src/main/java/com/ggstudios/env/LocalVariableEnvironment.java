package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;

public class LocalVariableEnvironment extends SimpleEnvironment {
    private String key;
    private Variable val;
    private Environment rest;
    private CompositeEnvironment classScopeEnv;

    public LocalVariableEnvironment(String name, Variable o, Environment rest) {
        key = name;
        val = o;
        this.rest = rest;

        lookForNameClash(key);

        if (rest instanceof LocalVariableEnvironment) {
            classScopeEnv = ((LocalVariableEnvironment) rest).classScopeEnv;
        } else if (rest instanceof CompositeEnvironment) {
            classScopeEnv = (CompositeEnvironment) rest;
        } else {
            throw new IllegalStateException("Assumption regarding environment parent is not true.");
        }
    }

    public CompositeEnvironment getClassScopeEnv() {
        return classScopeEnv;
    }

    private void lookForNameClash(String name) {
        Environment r = rest;
        while (r instanceof LocalVariableEnvironment) {
            LocalVariableEnvironment e = (LocalVariableEnvironment) r;
            if (e.key.equals(key)) {
                throw new EnvironmentException("Variable already defined in this scope",
                        EnvironmentException.ERROR_SAME_VARIABLE_IN_SCOPE, name);
            }
            r = e.rest;
        }
    }

    @Override
    protected Object lookupNameOneStep(String name) {
        if (name.equals(key)) {
            return val;
        } else {
            return rest;
        }
    }
}
