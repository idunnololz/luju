package com.ggstudios.types;

public abstract class Statement extends AstNode {
    public static final int TYPE_BLOCK          = 1;
    public static final int TYPE_EXPRESSION     = 2;
    public static final int TYPE_FOR            = 3;
    public static final int TYPE_IF             = 4;
    public static final int TYPE_IF_BLOCK       = 6;
    public static final int TYPE_RETURN         = 7;
    public static final int TYPE_VARDECL        = 8;
    public static final int TYPE_WHILE          = 9;

    private int type;

    protected Statement(int type) {
        this.type = type;
    }

    public int getStatementType() {
        return type;
    }
}
