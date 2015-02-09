package com.ggstudios.types;

import com.ggstudios.luju.Token;
import com.ggstudios.utils.Print;
import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FieldDecl extends AstNode {
    private Set<Token.Type> modifiers = new HashSet<>();

    private UserType type;

    private Token id;

    public Set<Token.Type> getModifiers() {
        return modifiers;
    }

    public void addModifier(Token.Type mod) {
        this.modifiers.add(mod);
    }

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }

    public Token getId() {
        return id;
    }

    public void setId(Token id) {
        this.id = id;
    }

    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
        sb.append(" (Type: ");
        sb.append(type.toString());
        sb.append("; Id: ");
        sb.append(id.getRaw());
        sb.append(")");

    }
}
