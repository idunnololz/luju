package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.VarDecl;
import com.ggstudios.utils.MapUtils;

import java.util.HashMap;
import java.util.List;

public class Method {
    private static final HashMap<String, Integer> nameCount = new HashMap<>();

    private Method overrideMethod;
    private int methodId = -1;

    private final Class declaringClass;
    private final MethodDecl methodDecl;
    private String name;
    protected String uniqueName;
    protected Environment env;

    private Class returnType;
    private Class[] parameterTypes;

    private int modifiers;

    public static void reset() {
        nameCount.clear();
    }

    public Method(Method toCopy) {
        this(toCopy.getDeclaringClass(), toCopy.getMethodDecl(), toCopy.env);
    }

    public Method(Class declaringClass, MethodDecl methodDecl, Environment env) {
        this.declaringClass = declaringClass;
        this.methodDecl = methodDecl;
        this.env = env;
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

        uniqueName = generateUniqueName(name);
    }

    protected static String generateUniqueName(String name) {
        int count = MapUtils.getOrDefault(nameCount, name, 0);
        String uniqueName = "_" + name + "@" + count++;
        nameCount.put(name, count);
        return uniqueName;
    }

    public String getUniqueName() {
        return uniqueName;
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

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public Method getOverrideMethod() {
        return overrideMethod;
    }

    public void setOverrideMethod(Method overrideMethod) {
        this.overrideMethod = overrideMethod;
    }

    private int METHOD_INDEX_MASK = 0x0000FFFF;
    private int METHOD_INTERFACE_MASK = 0xFFFF0000;

    public int getMethodId() {
        return methodId;
    }

    public int getMethodIndex() {
        return METHOD_INDEX_MASK & methodId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public void setMethodIndex(int index) {
        methodId = (methodId & METHOD_INTERFACE_MASK) + index;
    }

    public void setInterfaceId(int id) {
        methodId = (methodId & METHOD_INDEX_MASK) + (id << 16);
    }

    public int getInterfaceId() {
        return (methodId & METHOD_INTERFACE_MASK) >> 16;
    }

    public boolean isInterfaceMethod() {
        return getInterfaceId() != 0;
    }
}
