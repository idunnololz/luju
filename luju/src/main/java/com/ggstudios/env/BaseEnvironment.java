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
public class BaseEnvironment extends Environment {

    // A map of maps detailin
    //
    // g packages. For instance, a.b.c.D where a.b.c is the package part
    // and D is the class will be stored as a map: {a:{b:{c:{D:ClassInfo, ...}, ...}, ...}, ...}
    private Map<String, Object> baseMap = new HashMap<>();

    private List<Clazz> allClasses = new ArrayList<>();

    private static final String[] PRIMITIVE_TYPES = {
        "int", "boolean", "short", "char", "void", "byte"
    };

    public BaseEnvironment(Ast ast) {
        int len = ast.size();

        for (int i = 0; i < len; i++) {
            FileNode fn = ast.get(i);
            TypeDecl decl = fn.getTypeDecl();

            Clazz cm;
            if (decl instanceof InterfaceDecl) {
                cm = new Interface(decl, fn.getFilePath());
            } else {
                cm = new Clazz(decl, fn.getFilePath());
            }

            allClasses.add(cm);
            addEntry(cm.getPackage(), cm);
        }

        for (String s : PRIMITIVE_TYPES) {
            baseMap.put(s, new PrimitiveClazz(s));
        }
    }

    public List<Clazz> getAllClasses() {
        return allClasses;
    }

    @SuppressWarnings("unchecked")
    private void addEntry(String packageName, Clazz cMap) {
        String[] seq = packageName.split("\\.");
        Map<String, Object> m = baseMap;
        for (String s : seq) {
            if (m.containsKey(s)) {
                Object o = m.get(s);
                if (o instanceof Clazz) {
                    Clazz cm = (Clazz) o;
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
            if (o instanceof Clazz) {
                Clazz cm = (Clazz) o;
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

    @SuppressWarnings("unchecked")
    public List<Clazz> getAllClassesInPackage(String packageName) {
        String[] name = packageName.split("\\.");
        List<Clazz> classes = new ArrayList<>();

        Map<String, Object> m = baseMap;
        for (String s : name) {
            if (m.containsKey(s)) {
                Object o = m.get(s);
                if (o instanceof Clazz) {
                    throw new EnvironmentException("Given package name refers to a class. Given: " + packageName,
                            EnvironmentException.ERROR_PACKAGE_IS_CLASS);
                }
                m = (Map<String, Object>) m.get(s);
            } else {
                return classes;
            }
        }

        for (Map.Entry<String, Object> elem : m.entrySet()) {
            if (elem.getValue() instanceof Clazz) {
                classes.add((Clazz) elem.getValue());
            }
        }
        return classes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LookupResult lookupName(String[] name) {
        Map<String, Object> m = baseMap;
        for (int i = 0; i < name.length; i++) {
            String s = name[i];

            if (m.containsKey(s)) {
                Object o = m.get(s);
                if (o instanceof Clazz) {
                    Clazz cm = (Clazz) o;
                    if (name.length == i + 1) {
                        return new LookupResult(cm, i + 1);
                    } else {
                        return new LookupResult(cm.get(name[i + 1]), i + 2);
                    }
                } else {
                    m = (Map<String, Object>) m.get(s);
                }
            } else {
                return null;
            }
        }
        return null;
    }
}