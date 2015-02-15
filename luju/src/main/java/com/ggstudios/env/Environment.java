package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.types.ReferenceType;

public abstract class Environment {
    public abstract LookupResult lookupName(String[] name);

    public Clazz lookupClazz(ReferenceType type) {
        return lookupClazz(type.getTypeAsArray(), type.isArray());
    }

    public Clazz lookupClazz(String name, boolean isArray) {
        return lookupClazz(name.split("\\."), isArray);
    }

    public Clazz lookupClazz(String[] name, boolean isArray) {
        LookupResult r = lookupName(name);
        if (r == null || r.result == null || !(r.result instanceof Clazz) || r.tokensConsumed != name.length) {
            throw new EnvironmentException("Could not find class: " + nameListToName(name), EnvironmentException.ERROR_NOT_FOUND,
                    nameListToName(name));
        }
        if (r.result instanceof ErrorClass) {
            throw new EnvironmentException("Name clash", EnvironmentException.ERROR_CLASS_NAME_CLASH,
                    r.result);
        }
        if (isArray) {
            return ((Clazz) r.result).getArrayClass();
        }
        return (Clazz) r.result;
    }

    public Field lookupField(String name) {
        return lookupField(name.split("\\."));
    }

    public Field lookupField(String[] name) {
        LookupResult r = lookupName(name);
        if (r == null || r.result == null || !(r.result instanceof Field) || r.tokensConsumed != name.length) {
            throw new EnvironmentException("Could not find field: " + nameListToName(name), EnvironmentException.ERROR_NOT_FOUND);
        }
        return (Field) r.result;
    }

    public static String nameListToName(String[] name) {
        StringBuilder sb = new StringBuilder();
        for (String s : name) {
            sb.append(s);
            sb.append('.');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}