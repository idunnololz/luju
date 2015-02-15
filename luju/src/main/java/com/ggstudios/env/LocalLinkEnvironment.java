package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;

public class LocalLinkEnvironment extends Environment {
    private String key;
    private Object val;
    private Environment rest;

    public LocalLinkEnvironment(String name, Object o, Environment rest) {
        key = name;
        val = o;
        this.rest = rest;

        lookForNameClash(key);
    }

    private void lookForNameClash(String name) {
        Environment r = rest;
        while (r instanceof LocalLinkEnvironment) {
            LocalLinkEnvironment e = (LocalLinkEnvironment) r;
            if (e.key.equals(key)) {
                throw new EnvironmentException("Variable already defined in this scope",
                        EnvironmentException.ERROR_SAME_VARIABLE_IN_SCOPE, name);
            }
            r = e.rest;
        }
    }

    @Override
    public LookupResult lookupName(String[] name) {
        if (name[0].equals(key)) {
            return new LookupResult(val, 1);
        } else {
            return rest.lookupName(name);
        }
    }
}
