package com.ggstudios.env;

import com.ggstudios.error.NameResolutionException;
import com.ggstudios.luju.Ast;
import com.ggstudios.luju.FileNode;
import com.ggstudios.types.AstNode;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.TypeDecl;

import java.util.HashMap;
import java.util.Map;

/**
 * BaseEnvironment represents the environment that contains all fields and methods of all classes.
 */
public class BaseEnvironment extends Environment {
    private static final String CLASS = "Class";
    private static final String INTERFACE = "Interface";

    // A map of maps detailing packages. For instance, a.b.c.D where a.b.c is the package part
    // and D is the class will be stored as a map: {a:{b:{c:{D:ClassInfo, ...}, ...}, ...}, ...}
    private Map<String, Object> baseMap = new HashMap<>();

    public BaseEnvironment(Ast ast) {
        int len = ast.size();
        for (int i = 0; i < len; i++) {
            FileNode fn = ast.get(i);
            TypeDecl decl = fn.getTypeDecl();

        }
    }

    public void addEntry(String packageName, ClassMap cMap) {
        String[] seq = packageName.split(".");
        Map<String, Object> m = baseMap;
        for (String s : seq) {
            if (m.containsKey(s)) {
                Object o = baseMap.get(s);
                if (o instanceof ClassMap) {
                    ClassMap cm = (ClassMap) o;
                    throw new NameResolutionException(cm.getClassDecl(),
                            String.format("%s name clashes with package of the same name.",
                                    cm.isInterface() ? INTERFACE : CLASS));
                }
                m = (Map<String, Object>) o;
            } else {
                Map<String, Object> map = new HashMap<>();
                m.put(s, map);
            }
        }

        // m should now contain the hashmap where we need to place our class info...
        Object o = m.put(cMap.getClassDecl().getTypeName(), cMap);
        if (o != null) {
            // name clash...
            if (o instanceof ClassMap) {
                ClassMap cm = (ClassMap) o;
                throw new NameResolutionException(cm.getClassDecl(),
                        String.format("%s name clashes with another class of the same name.",
                                cm.isInterface() ? INTERFACE : CLASS));
            } else {
                throw new NameResolutionException(((ClassMap) o).getClassDecl(),
                        String.format("%s name clashes with package of the same name.",
                                cMap.isInterface() ? INTERFACE : CLASS));
            }
        }
    }

    @Override
    public AstNode lookupName(String name) {
        return null;
    }
}