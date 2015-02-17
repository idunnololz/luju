package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.TypeDecl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Clazz extends HashMap<String, Object> {
    private Set<String> methodSignatures = new HashSet<>();

    private TypeDecl classDecl;
    private String canonicalName;
    private String fileName;
    protected Clazz superClass;
    private Clazz arrayClass;

    private Clazz[] interfaces;

    private Environment env;

    private boolean isComplete = false;

    protected Clazz() {}

    public Clazz(TypeDecl classDecl, String fileName) {
        this.fileName = fileName;
        this.classDecl = classDecl;

        arrayClass = new ArrayClazz(this);

        canonicalName = classDecl.getPackage() + "." + classDecl.getTypeName();
    }

    public Clazz getArrayClass() {
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

    public Clazz getSuperClass() {
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

    public Environment getEnvironment() {
        return env;
    }

    public Clazz[] getInterfaces() {
        return interfaces;
    }

    protected void setInterfaces(List<String> ints, Environment env) {
        interfaces = new Clazz[ints.size()];
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

    public static boolean isValidAssign(Clazz lhs, Clazz rhs) {
        return (lhs == rhs || rhs == BaseEnvironment.TYPE_NULL || isSuperClassOf(lhs, rhs));
    }

    private static final HashMap<Clazz, Integer> classToCategory = new HashMap<>();
    private static final int CATEGORY_NUMBER = 0x00000001;
    private static final int CATEGORY_BOOLEAN = 0x00000002;

    public static boolean isValidCast(Clazz cast, Clazz original) {
        if (classToCategory.size() == 0) {
            classToCategory.put(BaseEnvironment.TYPE_BYTE, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_CHAR, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_INT, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_SHORT, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_BOOLEAN, CATEGORY_BOOLEAN);
        }

        Integer res = classToCategory.get(cast);
        if (res != null) {
            return res == classToCategory.get(original);
        }

        return (original == BaseEnvironment.TYPE_NULL || cast == original
                || isSuperClassOf(cast, original) || isSuperClassOf(original, cast));
    }

    private static boolean isSuperClassOf(Clazz lhs, Clazz rhs) {
        if (lhs.isArray() && rhs.isArray()) {
            return isSuperClassOf(lhs.getSuperClass(), rhs.getSuperClass());
        } else if (lhs.isArray() != rhs.isArray()) {
            return false;
        }

        Clazz superClass = rhs.getSuperClass();
        Clazz[] interfaces = rhs.getInterfaces();
        if (superClass == lhs) {
            return true;
        }

        if (interfaces != null) {
            for (Clazz c : interfaces) {
                if (c == lhs) {
                    return true;
                }
            }
        }

        if (superClass != null && isSuperClassOf(lhs, superClass)) {
            return true;
        }

        if (interfaces != null) {
            for (Clazz c : interfaces) {
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
}
