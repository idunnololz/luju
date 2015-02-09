package com.ggstudios.types;

import com.ggstudios.luju.Token;
import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TypeDecl extends AstNode {
    private Set<Token.Type> modifiers = new HashSet<>();
    private List<MethodDecl> methDecls = new ArrayList<>();
    private String typeName;

    private String declType;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Set<Token.Type> getModifiers() {
        return modifiers;
    }

    public void addModifier(Token.Type modifier) {
        this.modifiers.add(modifier);
    }

    protected void toPrettyStringThis(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getDeclType());
        sb.append(" (TypeName: ");
        sb.append(typeName);
        sb.append(")");
    }

    public void toPrettyString(StringBuilder sb, int level) {
        toPrettyStringThis(sb, level);

        if (methDecls.size() != 0) {
            sb.append("\n");
            for (MethodDecl mDecl : methDecls) {
                mDecl.toPrettyString(sb, level + 1);
                sb.append("\n");
            }
            sb.setLength(sb.length() - 1);
        }
    }

    protected String getDeclType() {
        return declType;
    }

    protected void setDeclType(String declType) {
        this.declType = declType;
    }

    public List<MethodDecl> getMethodDeclarations() {
        return methDecls;
    }

    public void addMethodDeclaration(MethodDecl methDecl) {
        methDecls.add(methDecl);
    }
}
