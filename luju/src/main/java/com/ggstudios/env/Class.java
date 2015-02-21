package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.TypeDecl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Class extends HashMap<String, Object> {
    private Set<String> methodSignatures = new HashSet<>();

    private int modifiers;
    private TypeDecl classDecl;
    private String canonicalName;
    private String fileName;
    protected Class superClass;
    private Class arrayClass;

    private Class[] interfaces;

    private Environment env;

    private boolean isComplete = false;

    protected Class() {}

    public Class(TypeDecl classDecl, String fileName) {
        this.fileName = fileName;
        this.classDecl = classDecl;

        arrayClass = new ArrayClass(this);

        canonicalName = classDecl.getPackage() + "." + classDecl.getTypeName();

        modifiers = classDecl.getModifiers();

        arrayClass.resolveSelf(arrayClass.getEnvironment());
    }

    public Class getArrayClass() {
        return arrayClass;
    }

    public TypeDecl getClassDecl() {
        return classDecl;
    }

    public String getName() {
        return classDecl.getTypeName();
    }

    public boolean isInterface() {
        return false;
    }

    public String getPackage() {
        return classDecl.getPackage();
    }

    public String getDeclType() {
        return classDecl.getDeclType();
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getFileName() {
        return fileName;
    }

    public void putMethod(Method method) {
        if (!methodSignatures.add(method.getMethodSignature())) {
            throw new NameResolutionException(getFileName(), method.getMethodDecl(),
                    String.format("'%s' is already defined in '%s'", method.getHumanReadableSignature(), getCanonicalName()));
        }
        put(method.getMethodSignature(), method);
    }

    public void putField(Field field) {
        if (put(field.getName(), field) != null) {
            throw new NameResolutionException(getFileName(), field.getVarDecl(),
                    String.format("Variable '%s' is already defined in this scope", field.getName()));
        }
    }

    public void putConstructor(Constructor constructor) {
        if (!methodSignatures.add(constructor.getConstructorSignature())) {
            throw new NameResolutionException(getFileName(), constructor.getConstructorDecl(),
                    String.format("'%s' is already defined in '%s'", constructor.getHumanReadableSignature(), getCanonicalName()));
        }
        put(constructor.getConstructorSignature(), constructor);
    }

    public Class getSuperClass() {
        return superClass;
    }

    public void resolveSelf(Environment env) {
        this.env = env;
        if (classDecl instanceof ClassDecl) {
            ClassDecl clazz = (ClassDecl) classDecl;
            if (clazz.getSuperTypeName() != null) {
                try {
                    superClass = env.lookupClazz(clazz.getSuperTypeName(), false);
                } catch (EnvironmentException e) {
                    throw new NameResolutionException(getFileName(), clazz,
                            String.format("Cannot resolve symbol '%s'", clazz.getSuperTypeName()));
                }
            } else if (this != BaseEnvironment.TYPE_OBJECT) {
                superClass = env.lookupClazz("Object", false);
            }

            setInterfaces(clazz.getImplementsList(), env);
        }
    }

    protected void setEnvironment(ClassEnvironment environment) {
        env = environment;
    }

    public Environment getEnvironment() {
        return env;
    }

    public Class[] getInterfaces() {
        return interfaces;
    }

    protected void setInterfaces(List<String> ints, Environment env) {
        interfaces = new Class[ints.size()];
        Set<String> dupMap = new HashSet<>();

        for (int i = 0; i < ints.size(); i++) {
            try {
                interfaces[i] = env.lookupClazz(ints.get(i), false);
            } catch (EnvironmentException e) {
                throw new NameResolutionException(getFileName(), getClassDecl(),
                        String.format("Cannot resolve symbol '%s'", ints.get(i)));
            }
            if (!dupMap.add(interfaces[i].getCanonicalName())) {
                throw new NameResolutionException(getFileName(), getClassDecl(),
                        String.format("Duplicate implements '%s'", interfaces[i].getCanonicalName()));
            }
        }
    }

    public boolean isArray() {
        return false;
    }

    /**
     * A class is considered complete if all it's methods/fields are added and inheritance is done
     * @return
     */
    public boolean isComplete() {
        return isComplete;
    }

    public void setIsComplete(boolean complete) {
        isComplete = complete;
    }

    public static boolean isValidAssign(Class lhs, Class rhs) {
        int l = getPrimitiveLevel(lhs);
        int r = getPrimitiveLevel(rhs);
        if (lhs == BaseEnvironment.TYPE_SHORT && rhs == BaseEnvironment.TYPE_CHAR) {
            // This is explicitly coded because this is a rule specific to Joos only (and not Java)
            return false;
        }
        if (l != 0 && r != 0) {
            return lhs == rhs || l > r;
        }
        if (rhs == BaseEnvironment.TYPE_NULL) {
            if (lhs.isPrimitive()) {
                return false;
            }
            return true;
        }

        return (lhs == rhs || isSuperClassOf(lhs, rhs));
    }

    private static final HashMap<Class, Integer> classToCategory = new HashMap<>();
    private static final HashMap<Class, Integer> classToLevel = new HashMap<>();
    public static final int CATEGORY_NUMBER = 0x00000001;
    private static final int CATEGORY_BOOLEAN = 0x00000002;

    private static int getPrimitiveLevel(Class c) {
        if (classToLevel.size() == 0) {
            classToLevel.put(BaseEnvironment.TYPE_BYTE,     0x0000000F);
            classToLevel.put(BaseEnvironment.TYPE_CHAR,     0x0000000F);
            classToLevel.put(BaseEnvironment.TYPE_SHORT,    0x00000FFF);
            classToLevel.put(BaseEnvironment.TYPE_INT,      0x0000FFFF);
        }
        return classToLevel.getOrDefault(c, 0);
    }

    public static int getCategory(Class c) {
        if (classToCategory.size() == 0) {
            classToCategory.put(BaseEnvironment.TYPE_BYTE, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_CHAR, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_INT, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_SHORT, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_BOOLEAN, CATEGORY_BOOLEAN);
        }
        return classToCategory.getOrDefault(c, 0);
    }

    public static boolean isValidCast(Class cast, Class original) {
        int res = getCategory(cast);
        if (res != 0) {
            return res == getCategory(original);
        }

        return (original == BaseEnvironment.TYPE_NULL || cast == original
                || isSuperClassOf(cast, original) || isSuperClassOf(original, cast));
    }

    private static boolean isSuperClassOf(Class lhs, Class rhs) {
        if (lhs == BaseEnvironment.TYPE_SERIALIZABLE || lhs == BaseEnvironment.TYPE_CLONEABLE) {
            return true;
        }
        if (lhs.isArray() && rhs.isArray()) {
            return isSuperClassOf(lhs.getSuperClass(), rhs.getSuperClass());
        }
        if (rhs.isArray() && lhs == BaseEnvironment.TYPE_OBJECT) {
            return true;
        }
        else if (lhs.isArray() != rhs.isArray()) {
            return false;
        }

        Class superClass = rhs.getSuperClass();
        Class[] interfaces = rhs.getInterfaces();
        if (superClass == lhs) {
            return true;
        }

        if (interfaces != null) {
            for (Class c : interfaces) {
                if (c == lhs) {
                    return true;
                }
            }
        }

        if (superClass != null && isSuperClassOf(lhs, superClass)) {
            return true;
        }

        if (interfaces != null) {
            for (Class c : interfaces) {
                if (isSuperClassOf(lhs, c)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return getCanonicalName();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isSimple() {
        return false;
    }
}
