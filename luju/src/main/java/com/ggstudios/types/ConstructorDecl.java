package com.ggstudios.types;

public class ConstructorDecl extends MethodDecl {

    public ConstructorDecl(ClassDecl classDecl) {
        super.setReturnType(new UserType(classDecl.getTypeName()));
    }

    @Override
    public void setReturnType(UserType returnType) {
        throw new IllegalStateException("Cannot set return type for a constructor.");
    }
}
