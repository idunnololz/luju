package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.VarDecl;

import java.util.List;

public class Method {
    private final Class declaringClass;
    private final MethodDecl methodDecl;
    private String name;

    private Class returnType;
    private Class[] parameterTypes;

    private int modifiers;

    public Method(Class declaringClass, MethodDecl methodDecl, CompositeEnvironment env) {
        this.declaringClass = declaringClass;
        this.methodDecl = methodDecl;
        name = methodDecl.getMethodName().getRaw();

        try {
            returnType = env.lookupClazz(methodDecl.getReturnType());
        } catch (EnvironmentException e) {
            throw new NameResolutionException(declaringClass.getFileName(), methodDecl.getReturnType(),
                    String.format("Cannot resolve symbol '%s'", methodDecl.getReturnType().toString()));
        }

        List<VarDecl> args = methodDecl.getArguments();
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

        modifiers = methodDecl.getModifiers();
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getName() {
        return name;
    }

    public MethodDecl getMethodDecl() {
        return methodDecl;
    }

    public Class getDeclaringClass() {
        return declaringClass;
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

    public String getMethodSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        for (Class type : parameterTypes) {
            sb.append(',');
            sb.append(type.getCanonicalName().replace('.', '$'));
            if (type.isArray()) {
                sb.append("[]");
            }
        }
        sb.append("#");
        return sb.toString();
    }

    public Class getReturnType() {
        return returnType;
    }

    public static String getMethodSignature(String methodName, List<Class> argTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        for (Class type : argTypes) {
            sb.append(',');
            sb.append(type.getCanonicalName().replace('.', '$'));
            if (type.isArray()) {
                sb.append("[]");
            }
        }
        sb.append("#");
        return sb.toString();
    }
}
