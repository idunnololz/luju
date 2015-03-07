package com.ggstudios.types;

import com.ggstudios.env.Constructor;

public class ConstructorDecl extends MethodDecl {

    Constructor proper;

    public ConstructorDecl(ClassDecl classDecl) {
        super.setReturnType(new ReferenceType(classDecl.getTypeName()));
    }

    @Override
    public void setReturnType(ReferenceType returnType) {
        throw new IllegalStateException("Cannot set return type for a constructor.");
    }

    public void setCProper(Constructor c) {
        this.proper = c;
    }

    public Constructor getCProper() {
        return proper;
    }
}
