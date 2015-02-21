package com.ggstudios.types;

import com.ggstudios.env.Field;
import com.ggstudios.env.Modifier;
import com.ggstudios.luju.Token;
import com.ggstudios.utils.PrintUtils;

import java.util.HashSet;
import java.util.Set;

public class VarDecl extends Statement {
    private int modifiers = 0;

    private ReferenceType type;

    private Token id;
    private Field proper;

    public VarDecl() {
        super(Statement.TYPE_VARDECL);
    }

    public int getModifiers() {
        return modifiers;
    }

    public void addModifier(Token.Type mod) {
        switch (mod) {
            case PUBLIC:
                modifiers |= Modifier.PUBLIC;
                break;
            case PROTECTED:
                modifiers |= Modifier.PROTECTED;
                break;
            case STATIC:
                modifiers |= Modifier.STATIC;
                break;
            case FINAL:
                modifiers |= Modifier.FINAL;
                break;
            case NATIVE:
                modifiers |= Modifier.NATIVE;
                break;
            case ABSTRACT:
                modifiers |= Modifier.ABSTRACT;
                break;
        }
    }

    public ReferenceType getType() {
        return type;
    }

    public void setType(ReferenceType type) {
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

    public String getName() {
        return id.getRaw();
    }

    public void setProper(Field proper) {
        this.proper = proper;
    }

    public Field getProper() {
        return proper;
    }
}
