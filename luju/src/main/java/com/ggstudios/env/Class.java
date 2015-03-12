package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.TypeDecl;
import com.ggstudios.utils.MapUtils;
import com.ggstudios.utils.Print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Class extends HashMap<String, Object> {
    private Set<String> methodSignatures = new HashSet<>();

    private List<Method> declaredMethods = new ArrayList<>();
    private List<Field> declaredFields = new ArrayList<>();
    private List<Constructor> declaredConstructors = new ArrayList<>();

    private Map<Interface, Integer> interfaceToIndex = new HashMap<>();

    private Package thisPackage;
    private int modifiers;
    private TypeDecl classDecl;
    private String canonicalName;
    private String fileName;
    protected Class superClass;
    private Class arrayClass;

    private Class[] interfaces;

    private Environment env;

    private boolean isComplete = false;

    private static int LAST_ID = 1;
    private final int id;

    private int numUniqueMethods; // does not include overriden methods
    private boolean cached;

    public static void reset() {
        LAST_ID = 1;
    }

    protected Class() {
        id = -1;
    }

    public Class(TypeDecl classDecl, String fileName) {
        id = LAST_ID++;

        this.fileName = fileName;
        this.classDecl = classDecl;

        arrayClass = new ArrayClass(this);

        canonicalName = classDecl.getPackage() + "." + classDecl.getTypeName();

        modifiers = classDecl.getModifiers();

        arrayClass.resolveSelf(arrayClass.getEnvironment());
    }

    public int getId() {
        return id;
    }

    protected void setPackage(Package p) {
        thisPackage = p;
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

    public Package getPackage() {
        return thisPackage;
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
        declaredMethods.add(method);
    }

    public void putField(Field field) {
        if (put(field.getName(), field) != null) {
            throw new NameResolutionException(getFileName(), field.getVarDecl(),
                    String.format("Variable '%s' is already defined in this scope", field.getName()));
        }

        declaredFields.add(field);
    }

    public void putConstructor(Constructor constructor) {
        if (!methodSignatures.add(constructor.getConstructorSignature())) {
            throw new NameResolutionException(getFileName(), constructor.getConstructorDecl(),
                    String.format("'%s' is already defined in '%s'", constructor.getHumanReadableSignature(), getCanonicalName()));
        }
        put(constructor.getConstructorSignature(), constructor);
        declaredConstructors.add(constructor);
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
     * A class is considered complete if all it's declaredMethods/fields are added and inheritance is done
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

        return !(lhs == BaseEnvironment.TYPE_VOID || lhs == BaseEnvironment.TYPE_VOID) && isSuperClassOf(lhs, rhs);
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
        return MapUtils.getOrDefault(classToLevel, c, 0);
    }

    public static int getCategory(Class c) {
        if (classToCategory.size() == 0) {
            classToCategory.put(BaseEnvironment.TYPE_BYTE, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_CHAR, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_INT, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_SHORT, CATEGORY_NUMBER);
            classToCategory.put(BaseEnvironment.TYPE_BOOLEAN, CATEGORY_BOOLEAN);
        }
        return MapUtils.getOrDefault(classToCategory, c, 0);
    }

    public static boolean isValidCast(Class cast, Class original) {
        int res = getCategory(cast);
        if (res != 0) {
            return res == getCategory(original);
        }

        return (original == BaseEnvironment.TYPE_NULL ||
                isSuperClassOf(cast, original) || isSuperClassOf(original, cast));
    }

    public boolean isSubClassOf(Class c) {
        return isSuperClassOf(c, this);
    }

    private static boolean isSuperClassOf(Class lhs, Class rhs) {
        if (lhs == rhs) return true;
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

    public List<Method> getDeclaredMethods() {
        return declaredMethods;
    }

    public List<Field> getDeclaredFields() {
        return declaredFields;
    }

    public List<Constructor> getDeclaredConstructors() {
        return declaredConstructors;
    }

    public int getNumUniqueNonStaticMethods() {
        return numUniqueMethods;
    }

    public void setNumUniqueMethods(int numUniqueMethods) {
        this.numUniqueMethods = numUniqueMethods;
    }

    public void addInterfaceIndex(Interface interf, int index) {
        interfaceToIndex.put(interf, index);
    }

    public int getInterfaceIndex(Interface i) {
        return interfaceToIndex.get(i);
    }

    private List<Field> completeFieldList;
    private LinkedList<Field> completeNonStaticFields;
    private Map<Field, Integer> fieldToIndex;

    private List<Method> completeMethodList;
    private ArrayList<Method> completeNonStaticMethods;
    private Map<Method, Integer> methodToIndex;

    private String vtableLabel;

    private boolean codeGenInfoGenerated = false;

    public void generateDataForCodeGeneration() {

        if (codeGenInfoGenerated) return;

        int methods = getNumUniqueNonStaticMethods();

        LinkedList<Field> fieldList = new LinkedList<>();
        LinkedList<Method> methodList = new LinkedList<>();
        completeNonStaticFields = new LinkedList<>();
        completeNonStaticMethods = new ArrayList<>(methods);

        for (int i = 0; i < methods; i++) {
            completeNonStaticMethods.add(null);
        }

        fieldToIndex = new HashMap<>();
        methodToIndex = new HashMap<>();
        Class thisClass = this;
        do {
            if (isInterface() && thisClass == BaseEnvironment.TYPE_OBJECT) break;
            for (Field f : thisClass.getDeclaredFields()) {
                fieldList.addFirst(f);

                if (!Modifier.isStatic(f.getModifiers())) {
                    completeNonStaticFields.addFirst(f);
                }
            }
            for (Method m : thisClass.getDeclaredMethods()) {
                methodList.addFirst(env.lookupMethod(m.getMethodSignature()));

                if (!Modifier.isStatic(m.getModifiers())) {
                    int index = m.getMethodIndex();
                    if (completeNonStaticMethods.get(index) == null) {
                        completeNonStaticMethods.set(index, m);
                        methodToIndex.put(m, index);
                    }
                }
            }
        } while ((thisClass = thisClass.getSuperClass()) != null);

        int i = 0;
        for (Field f : completeNonStaticFields) {
            fieldToIndex.put(f, i++);
        }

        completeFieldList = fieldList;
        completeMethodList = methodList;

        vtableLabel = getCanonicalName() + "$vt";
        codeGenInfoGenerated = true;
    }

    public int getFieldIndex(Field f) {
        generateDataForCodeGeneration();
        Integer i;
        if ((i = fieldToIndex.get(f)) == null) {
            throw new RuntimeException("Field " + f.getName() + " index not found in class " + getCanonicalName());
        }
        return i;
    }

    public int getMethodIndex(Method m) {
        generateDataForCodeGeneration();
        Integer i;
        if ((i = methodToIndex.get(m)) == null) {
            throw new RuntimeException("Method " + m.getName() + " index not found in class " + getCanonicalName());
        }
        return i;
    }

    public List<Field> getCompleteNonStaticFields() {
        generateDataForCodeGeneration();
        return completeNonStaticFields;
    }

    public List<Field> getCompleteFieldList() {
        generateDataForCodeGeneration();
        return completeFieldList;
    }

    public String getVtableLabel() {
        generateDataForCodeGeneration();
        return vtableLabel;
    }

    public List<Method> getCompleteMethodList() {
        generateDataForCodeGeneration();
        return completeMethodList;
    }

    public List<Method> getCompleteNonStaticMethodList() {
        generateDataForCodeGeneration();
        return completeNonStaticMethods;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public boolean isCached() {
        return cached;
    }

    public String getUniqueLabel() {
        return getCanonicalName();
    }
}
