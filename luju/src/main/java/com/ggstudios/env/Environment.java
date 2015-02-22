package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.types.ReferenceType;

public abstract class Environment {
    protected static boolean inStaticMode = false;
    protected static boolean allowNonStatic = true;
    protected static boolean noStaticMode = false;
    protected static boolean allowProtected = true;
    protected static WarningResolver resolver;

    public static final int WARNING_SUSPICIOUS_PROTECTED_ACCESS_FIELD = 1;
    public static final int WARNING_SUSPICIOUS_PROTECTED_ACCESS_METHOD = 2;

    public static final int WARNING_ENSURE_SAME_PACKAGE_OR_SUBCLASS_FIELD = 3;
    public static final int WARNING_ENSURE_SAME_PACKAGE_OR_SUBCLASS_METHOD = 4;

    public Environment() {
        allowProtected = true;
    }

    /**
     * Under certain conditions, the environment can spot interesting referencing and send an alert
     * to help catch errors. This allows for a much more performant error detection system.
     * @param resolver
     */
    public static void turnOnHints(WarningResolver resolver) {
        Environment.resolver = resolver;
    }

    public static void turnOffHints() {
        Environment.resolver = null;
    }

    protected static void warn(int warning, Object extra) {
        if (Environment.resolver != null) {
            Environment.resolver.resolveWarning(warning, extra);
        }
    }

    protected static Class getCurrentClass() {
        if (Environment.resolver != null) {
            return Environment.resolver.getCurrentClass();
        }
        return null;
    }

    public static void setStaticMode(boolean mode) {
        inStaticMode = mode;
    }

    public static void setNoStaticMode(boolean noStaticMode) {
        Environment.noStaticMode = noStaticMode;
    }

    public static boolean isNoStaticMode() {
        return noStaticMode;
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

    public static interface WarningResolver {
        public void resolveWarning(int type, Object extra);
        public Class getCurrentClass();
    }
}