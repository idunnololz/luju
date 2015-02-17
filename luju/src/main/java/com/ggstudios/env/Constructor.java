package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ConstructorDecl;
import com.ggstudios.types.VarDecl;

import java.util.List;

public class Constructor {
    private Class declaringClass;
    private int modifiers;
    private String name;

    private Class[] parameterTypes;

    private String signature;
    private ConstructorDecl constructorDecl;

    public Constructor(Class c, ConstructorDecl cd, Environment env) {
        declaringClass = c;
        modifiers = cd.getModifiers();
        constructorDecl = cd;

        name = cd.getMethodName().getRaw();

        List<VarDecl> args = cd.getArguments();
        parameterTypes = new Class[args.size()];

        for (int i = 0; i < args.size(); i++) {
            VarDecl a = args.get(i);
            try {
                parameterTypes[i] = env.lookupClazz(a.getType());
            } catch (EnvironmentException e) {
                throw new NameResolutionException(declaringClass.getFileName(), a,
                        String.format("Cannot resolve symbol '%s'", a.getType().toString()));
            }
        }
    }

    public Class getDeclaringClass() {
        return declaringClass;
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getName() {
        return name;
    }

    public String getHumanReadableSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append('(');
        if (parameterTypes.length != 0) {
            for (Class type : parameterTypes) {
                sb.append(type.getName());
                if (type.isArray()) {
                    sb.append("[]");
                }
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append(')');
        return sb.toString();
    }

    public String getConstructorSignature() {
        if (signature == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getName());
            for (Class type : parameterTypes) {
                sb.append(',');
                sb.append(type.getCanonicalName().replace('.', '$'));
                if (type.isArray()) {
                    sb.append("[]");
                }
            }
            sb.append("@");
            signature = sb.toString();
        }
        return signature;
    }

    public static String getConstructorSignature(String constructorName, List<Class> argTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(constructorName);
        for (Class type : argTypes) {
            sb.append(',');
            sb.append(type.getCanonicalName().replace('.', '$'));
            if (type.isArray()) {
                sb.append("[]");
            }
        }
        sb.append("@");
        return sb.toString();
    }

    public ConstructorDecl getConstructorDecl() {
        return constructorDecl;
    }
}
