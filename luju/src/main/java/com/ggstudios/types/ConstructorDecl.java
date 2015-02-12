package com.ggstudios.types;

public class ConstructorDecl extends MethodDecl {

    public ConstructorDecl(ClassDecl classDecl) {
        super.setReturnType(new ReferenceType(classDecl.getTypeName()));
    }

    @Override
    public void setReturnType(ReferenceType returnType) {
        throw new IllegalStateException("Cannot set return type for a constructor.");
    }
}
