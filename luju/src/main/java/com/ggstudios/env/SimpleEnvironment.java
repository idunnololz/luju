package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.TypeException;

import java.util.Map;

public abstract class SimpleEnvironment extends Environment {
    protected abstract Object lookupNameOneStep(String name);

//    private int b = 34;
//    private static int a = b;

    private void staticCheck(Object o) {
        if (this instanceof LocalVariableEnvironment) return;
        if (o instanceof Field) {
            Field f = (Field) o;
            if (!Environment.allowNonStatic && !Modifier.isStatic(f.getModifiers())) {
                throw new EnvironmentException("Non static field referenced from static context",
                        EnvironmentException.ERROR_NON_STATIC_FIELD_FROM_STATIC_CONTEXT,
                        f);
            }
        } else if (o instanceof Method) {
            Method m = (Method) o;
            if (!Environment.allowNonStatic && !Modifier.isStatic(m.getModifiers())) {
                throw new EnvironmentException("Non static method referenced from static context",
                        EnvironmentException.ERROR_NON_STATIC_METHOD_FROM_STATIC_CONTEXT,
                        m);
            }
        }
    }


    public LookupResult lookup(String[] name) {
        if (name.length == 0) return null;

        boolean typeFound = false;
        Object o = lookupNameOneStep(name[0]);
        int i = 1;
        while (i != name.length) {
            if (o instanceof Class) {
                typeFound = true;
                Environment.allowNonStatic = false;
            }

            staticCheck(o);

            if (o instanceof Map) {
                o = ((Map) o).get(name[i++]);
            } else if (o instanceof Field) {
                o = ((Field) o).getType().get(name[i++]);
                Environment.allowNonStatic = true;
            } else {
                break;
            }
        }

        staticCheck(o);

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
