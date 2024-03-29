package com.ggstudios.types;

import com.ggstudios.utils.ListUtils;

import java.util.ArrayList;
import java.util.List;

public class ClassDecl extends TypeDecl {
    private List<String> implementsList = new ArrayList<>();
    private List<ConstructorDecl> constructorDecls = new ArrayList<>();
    private List<VarDecl> fieldDecls = new ArrayList<>();

    private String superTypeName;

    public ClassDecl(String packageName) {
        super(packageName);
        setDeclType("Class");
    }

    public List<String> getImplementsList() {
        return implementsList;
    }

    public void addImplement(String impl) {
        this.implementsList.add(impl);
    }

    public List<VarDecl> getFieldDeclarations() {
        return fieldDecls;
    }

    public void addFieldDeclaration(VarDecl fieldDecl) {
        this.fieldDecls.add(fieldDecl);
    }

    public String getSuperTypeName() {
        return superTypeName;
    }

    public void setSuperTypeName(String superTypeName) {
        this.superTypeName = superTypeName;
    }

    @Override
    protected void toPrettyStringThis(StringBuilder sb, int level) {
        super.toPrettyStringThis(sb, level);
        sb.setLength(sb.length() - 1);  // erase the ')'
        sb.append("; Extends: ");
        sb.append(superTypeName);
        sb.append("; Implements: [");

        if (implementsList.size() != 0) {
            for (String s : ListUtils.reverse(implementsList)) {
                sb.append(s);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("])\n");

        if (fieldDecls.size() != 0) {
            for (VarDecl fDecl : fieldDecls) {
                fDecl.toPrettyString(sb, level + 1);
                sb.append("\n");
            }
        }

        if (!constructorDecls.isEmpty()) {
            for (ConstructorDecl cDecl : constructorDecls) {
                cDecl.toPrettyString(sb, level + 1);
                sb.append("\n");
            }
        }

        sb.setLength(sb.length() - 1);
    }

    public List<ConstructorDecl> getConstructorDeclaration() {
        return constructorDecls;
    }

    public void addConstructorDeclaration(ConstructorDecl constructorDecl) {
        this.constructorDecls.add(constructorDecl);
    }
}
