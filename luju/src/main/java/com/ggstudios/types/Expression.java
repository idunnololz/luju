package com.ggstudios.types;

import com.ggstudios.utils.PrintUtils;

public class Expression extends AstNode {
    private boolean enclosedInParen = false;

    @Override
    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
        sb.append(' ');
        sb.append(toString());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public boolean isEnclosedInParen() {
        return enclosedInParen;
    }

    public void setEnclosedInParen(boolean enclosedInParen) {
        this.enclosedInParen = enclosedInParen;
    }
}
