package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;

import java.util.Map;

public abstract class SimpleEnvironment extends Environment {
    protected abstract Object lookupNameOneStep(String name);

    private void staticAndProtectedCheck(Object o, boolean needToEnsureSamePackage) {
        if (o instanceof Variable) return;
        if (o instanceof Field) {
            Field f = (Field) o;
            if (!Environment.allowNonStatic && !Modifier.isStatic(f.getModifiers())) {
                throw new EnvironmentException("Non static field referenced from static context",
                        EnvironmentException.ERROR_NON_STATIC_FIELD_FROM_STATIC_CONTEXT,
                        f);
            } else if (Environment.allowNonStatic && Environment.noStaticMode && Modifier.isStatic(f.getModifiers())) {
                throw new EnvironmentException("Static field referenced from non static context",
                        EnvironmentException.ERROR_STATIC_FIELD_FROM_NON_STATIC_CONTEXT,
                        f);
            }

            if (Modifier.isProtected(f.getModifiers())) {
                if (needToEnsureSamePackage) {
                    warn(WARNING_ENSURE_SAME_PACKAGE_OR_SUBCLASS_FIELD, f);
                } else if (!Environment.allowProtected) {
                    warn(WARNING_SUSPICIOUS_PROTECTED_ACCESS_FIELD, f);
                }
            }
        }
        // method modifier checks are done in NameResolver since multiple methods cannot be packed
        // into the same expression
    }

    public LookupResult lookup(String[] name) {
        if (name.length == 0) return null;

        boolean needToEnsureSamePackage = false;
        boolean temp = Environment.allowProtected;
        boolean typeFound = false;
        Object o = lookupNameOneStep(name[0]);
        int i = 1;
        while (i != name.length) {
            if (o instanceof Class) {
                typeFound = true;
                Environment.allowNonStatic = false;
            }

            staticAndProtectedCheck(o, needToEnsureSamePackage);

            if (o instanceof Map) {
                o = ((Map) o).get(name[i++]);
            } else if (o instanceof Field) {
                Field f = (Field) o;
                o = f.getType().get(name[i++]);
                Environment.allowNonStatic = true;
                Environment.allowProtected = false;

                Class curClass = getCurrentClass();
                if (curClass != null && !f.getType().isSubClassOf(curClass)) {
                    needToEnsureSamePackage = true;
                }
            } else {
                break;
            }
        }

        staticAndProtectedCheck(o, needToEnsureSamePackage);

        Environment.allowProtected = temp;

        if (o instanceof Environment) {
            return ((Environment) o).lookup(name);
        } else if (o == null) {
            if (typeFound) {
                return new LookupResult(o, i - 1);
            }
            return null;
        }
        return new LookupResult(o, i);
    }
}
