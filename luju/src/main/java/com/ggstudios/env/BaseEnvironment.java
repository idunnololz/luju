package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.luju.Ast;
import com.ggstudios.luju.FileNode;
import com.ggstudios.types.InterfaceDecl;
import com.ggstudios.types.TypeDecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BaseEnvironment represents the environment that contains all fields and methods of all classes.
 */
public class BaseEnvironment extends MapEnvironment {

    // A map of maps detailin
    //
    // g packages. For instance, a.b.c.D where a.b.c is the package part
    // and D is the class will be stored as a map: {a:{b:{c:{D:ClassInfo, ...}, ...}, ...}, ...}
    private Map<String, Object> baseMap = new HashMap<>();

    private List<Class> allClasses = new ArrayList<>();

    public static final Class TYPE_INT = new PrimitiveClass("int", true);
    public static final Class TYPE_BOOLEAN = new PrimitiveClass("boolean", true);
    public static final Class TYPE_SHORT = new PrimitiveClass("short", true);
    public static final Class TYPE_CHAR = new PrimitiveClass("char", true);
    public static final Class TYPE_VOID = new PrimitiveClass("void", true);
    public static final Class TYPE_BYTE = new PrimitiveClass("byte", true);
    public static final Class TYPE_NULL = new PrimitiveClass("null", false);
    public static Class TYPE_OBJECT;
    public static Class TYPE_OBJECT_BOOLEAN;
    public static Class TYPE_STRING;
    public static Class TYPE_SERIALIZABLE;
    public static Class TYPE_CLONEABLE;

    public static final Package PRIMITIVE_PACKAGE = new Package("");

    static {
        ((PrimitiveClass)TYPE_INT).forceReinitializeArrayClass();
    }

    public BaseEnvironment(Ast ast) {
        map = baseMap;
        int len = ast.size();

        for (int i = 0; i < len; i++) {
            FileNode fn = ast.get(i);
            TypeDecl decl = fn.getTypeDecl();

            Class cm;
            if (decl instanceof InterfaceDecl) {
                cm = new Interface(decl, fn.getFilePath());
            } else {
                cm = new Class(decl, fn.getFilePath());
            }

            String packageName = decl.getPackage();
            Package p = Package.getPackage(packageName);
            if (p == null) {
                p = new Package(packageName);
                Package.addPackage(p);
            }
            cm.setPackage(p);
            allClasses.add(cm);
            addEntry(packageName, cm);
        }

        TYPE_OBJECT = lookupClazz("java.lang.Object", false);
        TYPE_STRING = lookupClazz("java.lang.String", false);
        TYPE_OBJECT_BOOLEAN = lookupClazz("java.lang.Boolean", false);

        TYPE_CLONEABLE = lookupClazz("java.lang.Cloneable", false);
        TYPE_SERIALIZABLE = lookupClazz("java.io.Serializable", false);

        baseMap.put("int", TYPE_INT);
        baseMap.put("boolean", TYPE_BOOLEAN);
        baseMap.put("short", TYPE_SHORT);
        baseMap.put("char", TYPE_CHAR);
        baseMap.put("byte", TYPE_BYTE);
        baseMap.put("void", TYPE_VOID);
        baseMap.put("null", TYPE_NULL);
    }

    public List<Class> getAllClasses() {
        return allClasses;
    }

    @SuppressWarnings("unchecked")
    private void addEntry(String packageName, Class cMap) {
        String[] seq = packageName.split("\\.");
        Map<String, Object> m = baseMap;
        for (String s : seq) {
            if (m.containsKey(s)) {
                Object o = m.get(s);
                if (o instanceof Class) {
                    Class cm = (Class) o;
                    throw new NameResolutionException(cm.getFileName(), cm.getClassDecl(),
                            String.format("%s name '%s' clashes with package of the same name.",
                                    cm.getDeclType(), cm.getName()));
                }
                m = (Map<String, Object>) o;
            } else {
                Map<String, Object> map = new HashMap<>();
                m.put(s, map);
                m = map;
            }
        }

        // m should now contain the hashmap where we need to place our class info...
        Object o = m.put(cMap.getName(), cMap);
        if (o != null) {
            // name clash...
            if (o instanceof Class) {
                Class cm = (Class) o;
                throw new NameResolutionException(cm.getFileName(), cm.getClassDecl(),
                        String.format("%s name '%s' clashes with another class of the same name.",
                                cm.getDeclType(), cm.getName()));
            } else {
                throw new NameResolutionException(cMap.getFileName(), cMap.getClassDecl(),
                        String.format("%s name '%s' clashes with package of the same name.",
                                cMap.getDeclType(), cMap.getName()));
            }
        }
    }

    public List<Class> getAllClassesInPackage(Package p) {
        return getAllClassesInPackage(p.getName());
    }

    @SuppressWarnings("unchecked")
    public List<Class> getAllClassesInPackage(String packageName) {
        String[] name = packageName.split("\\.");
        List<Class> classes = new ArrayList<>();

        Map<String, Object> m = baseMap;
        for (String s : name) {
            if (m.containsKey(s)) {
                Object o = m.get(s);
                if (o instanceof Class) {
                    throw new EnvironmentException(String.format("Package import refers to a class '%s'", packageName),
                            EnvironmentException.ERROR_PACKAGE_IS_CLASS);
                }
                m = (Map<String, Object>) m.get(s);
            } else {
                throw new EnvironmentException(String.format("Package '%s' doesn't exist", packageName),
                        EnvironmentException.ERROR_PACKAGE_IS_CLASS);
            }
        }

        for (Map.Entry<String, Object> elem : m.entrySet()) {
            if (elem.getValue() instanceof Class) {
                classes.add((Class) elem.getValue());
            }
        }
        return classes;
    }
}