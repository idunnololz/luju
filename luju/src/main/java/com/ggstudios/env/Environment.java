package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.types.ReferenceType;

public abstract class Environment {
    protected static boolean inStaticMode = false;
    protected static boolean allowNonStatic = true;

    public static void setStaticMode(boolean mode) {
        inStaticMode = mode;
    }

    public abstract LookupResult lookup(String[] name);

    public LookupResult lookupName(String[] name) {
        allowNonStatic = !inStaticMode;
        return lookup(name);
    }

    public Class lookupClazz(ReferenceType type) {
        return lookupClazz(type.getTypeAsArray(), type.isArray());
    }

    public Class lookupClazz(String name, boolean isArray) {
        return lookupClazz(name.split("\\."), isArray);
    }

    public Class lookupClazz(String[] name, boolean isArray) {
        CompositeEnvironment thisComp = null;
        if (this instanceof LocalVariableEnvironment) {
            thisComp = ((LocalVariableEnvironment) this).getClassScopeEnv();
        } else if (this instanceof CompositeEnvironment) {
            thisComp = (CompositeEnvironment) this;
        }

        LookupResult r;
        if (thisComp != null) {
            // TODO implement this better...
            r = thisComp.lookupName(name, true);
        } else {
            r = lookup(name);
        }

        if (r == null || r.result == null || !(r.result instanceof Class) || r.tokensConsumed != name.length) {
            throw new EnvironmentException("Could not find class: " + nameListToName(name), EnvironmentException.ERROR_NOT_FOUND,
                    nameListToName(name));
        }
        if (r.result instanceof ErrorClass) {
            throw new EnvironmentException("Name clash", EnvironmentException.ERROR_CLASS_NAME_CLASH,
                    r.result);
        }
        if (isArray) {
            return ((Class) r.result).getArrayClass();
        }
        return (Class) r.result;
    }

    public Field lookupField(String name) {
        return lookupField(name.split("\\."));
    }

    public Field lookupField(String[] name) {
        LookupResult r = lookupName(name);
        if (r == null || r.result == null || !(r.result instanceof Field) || r.tokensConsumed != name.length) {
            String fullName = nameListToName(name);
            throw new EnvironmentException("Could not find field: " + fullName, EnvironmentException.ERROR_NOT_FOUND,
                    fullName);
        }
        return (Field) r.result;
    }

    public Method lookupMethod(String name) {
        return lookupMethod(name.split("\\."));
    }

    public Method lookupMethod(String[] name) {
        LookupResult r = lookupName(name);
        if (r == null || r.result == null || !(r.result instanceof Method) || r.tokensConsumed != name.length) {
            String fullName = nameListToName(name);
            throw new EnvironmentException("Could not find method: " + fullName, EnvironmentException.ERROR_NOT_FOUND,
                    fullName);
        }
        return (Method) r.result;
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

    public Constructor lookupConstructor(String name) {
        return lookupConstructor(name.split("\\."));
    }

    public Constructor lookupConstructor(String[] name) {
        LookupResult r = lookupName(name);
        if (r == null || r.result == null || !(r.result instanceof Constructor) || r.tokensConsumed != name.length) {
            String fullName = nameListToName(name);
            throw new EnvironmentException("Could not find constructor: " + fullName, EnvironmentException.ERROR_NOT_FOUND,
                    fullName);
        }
        return (Constructor) r.result;
    }
}