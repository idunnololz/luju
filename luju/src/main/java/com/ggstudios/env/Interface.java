package com.ggstudios.env;

import com.ggstudios.types.InterfaceDecl;
import com.ggstudios.types.TypeDecl;

import java.util.HashMap;

public class Interface extends Class {
    private InterfaceDecl interfaceDecl;

    private static HashMap<Integer, Interface> idToInterface = new HashMap<>();

    public static void reset() {
        idToInterface.clear();
    }

    public Interface(TypeDecl classDecl, String fileName) {
        super(classDecl, fileName);

        interfaceDecl = (InterfaceDecl) classDecl;
        idToInterface.put(getId(), this);
    }

    @Override
    public boolean isInterface() {
        return true;
    }

    @Override
    public void resolveSelf(Environment env) {
        super.resolveSelf(env);

        setInterfaces(interfaceDecl.getExtendsList(), env);
        superClass = env.lookupClazz("Object", false);
    }

    public static Interface getInterfaceForId(int id) {
        return idToInterface.get(id);
    }
}
