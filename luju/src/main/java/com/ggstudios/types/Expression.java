package com.ggstudios.types;

import com.ggstudios.utils.PrintUtils;

public class Expression extends AstNode {
    public static final int ARRAY_ACCESS_EXPRESSION         = 1;
    public static final int ARRAY_CREATION_EXPRESSION       = 2;
    public static final int ASSIGN_EXPRESSION               = 3;
    public static final int BINARY_EXPRESSION               = 4;
    public static final int ICREATION_EXPRESSION            = 5;
    public static final int LITERAL_EXPRESSION              = 6;
    public static final int METHOD_EXPRESSION               = 7;
    public static final int REFERENCE_TYPE                  = 8;
    public static final int THIS_EXPRESSION                 = 9;
    public static final int UNARY_EXPRESSION                = 10;
    public static final int VARIABLE_EXPRESSION             = 11;


    private boolean enclosedInParen = false;

    private int type;

    protected void setType(int type) {
        this.type = type;
    }

    public int getExpressionType() {
        return type;
    }

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
