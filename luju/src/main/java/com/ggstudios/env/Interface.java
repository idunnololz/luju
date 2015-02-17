package com.ggstudios.env;

import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.InterfaceDecl;
import com.ggstudios.types.TypeDecl;

import java.util.List;

public class Interface extends Clazz {
    private InterfaceDecl interfaceDecl;

    public Interface(TypeDecl classDecl, String fileName) {
        super(classDecl, fileName);

        interfaceDecl = (InterfaceDecl) classDecl;
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
}
