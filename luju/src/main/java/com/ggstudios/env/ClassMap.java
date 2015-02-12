package com.ggstudios.env;

import com.ggstudios.types.AstNode;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.TypeDecl;

import java.util.HashMap;

public class ClassMap extends HashMap<String, AstNode> {
    private TypeDecl classDecl;

    public ClassMap(TypeDecl classDecl) {
        this.classDecl = classDecl;
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
}
