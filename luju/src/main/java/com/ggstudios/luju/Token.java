package com.ggstudios.luju;

public class Token {
    private int row, col;
    private String raw;
    private Type type;
    private int val;

    public enum Type {
        EOF(LalrTable.TERM_EOF),
        BOF(LalrTable.TERM_BOF),
        ID(LalrTable.TERM_ID),
        STRINGLIT(LalrTable.TERM_STRINGLIT),
        CHARLIT(LalrTable.TERM_CHARLIT),
        INTLIT(LalrTable.TERM_INTLIT),
        COMMENT(LalrTable.TERM_INVALID),
        DOT(LalrTable.TERM_DOT),
        LT(LalrTable.TERM_LTTHAN),
        LT_EQ(LalrTable.TERM_LTEQ),
        GT(LalrTable.TERM_GTTHAN),
        LPAREN(LalrTable.TERM_LPAREN),
        RPAREN(LalrTable.TERM_RPAREN),
        LBRACK(LalrTable.TERM_LBRACKET),
        RBRACK(LalrTable.TERM_RBRACKET),
        LBRACE(LalrTable.TERM_LBRACE),
        RBRACE(LalrTable.TERM_RBRACE),
        GT_EQ(LalrTable.TERM_GTEQ),
        EQ(LalrTable.TERM_ASSIGN),
        EQ_EQ(LalrTable.TERM_EQUALS),
        NEQ(LalrTable.TERM_NOTEQUALS),
        PLUS(LalrTable.TERM_PLUS),
        MINUS(LalrTable.TERM_MINUS),
        STAR(LalrTable.TERM_TIMES),
        FSLASH(LalrTable.TERM_DIVIDE),
        AMP(LalrTable.TERM_ANDS),
        AMP_AMP(LalrTable.TERM_ANDL),
        MOD(LalrTable.TERM_MOD),
        PIPE(LalrTable.TERM_ORS),
        PIPE_PIPE(LalrTable.TERM_ORL),
        NOT(LalrTable.TERM_NOT),
        COMMA(LalrTable.TERM_COMMA),
        SEMI(LalrTable.TERM_SEMI),
        ABSTRACT(LalrTable.TERM_ABSTRACT),
        BOOLEAN(LalrTable.TERM_BOOLEAN),
        BYTE(LalrTable.TERM_BYTE),
        CHAR(LalrTable.TERM_CHAR),
        CLASS(LalrTable.TERM_CLASS),
        ELSE(LalrTable.TERM_ELSE),
        EXTENDS(LalrTable.TERM_EXTENDS),
        FALSE(LalrTable.TERM_FALSE),
        FINAL(LalrTable.TERM_FINAL),
        FOR(LalrTable.TERM_FOR),
        IF(LalrTable.TERM_IF),
        IMPLEMENTS(LalrTable.TERM_IMPLEMENTS),
        IMPORT(LalrTable.TERM_IMPORT),
        INSTANCEOF(LalrTable.TERM_INSTANCEOF),
        INT(LalrTable.TERM_INT),
        INTERFACE(LalrTable.TERM_INTERFACE),
        NATIVE(LalrTable.TERM_NATIVE),
        NEW(LalrTable.TERM_NEW),
        NULL(LalrTable.TERM_NULL),
        PACKAGE(LalrTable.TERM_PACKAGE),
        PROTECTED(LalrTable.TERM_PROTECTED),
        PUBLIC(LalrTable.TERM_PUBLIC),
        RETURN(LalrTable.TERM_RETURN),
        SHORT(LalrTable.TERM_SHORT),
        STATIC(LalrTable.TERM_STATIC),
        TRUE(LalrTable.TERM_TRUE),
        VOID(LalrTable.TERM_VOID),
        WHILE(LalrTable.TERM_WHILE),
        INVALID(LalrTable.TERM_INVALID);

        private final int value;
        private Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public Token(Type type, String raw, int col, int row) {
        this.type = type;
        this.raw = raw;
        this.col = col + 1;
        this.row = row;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getRaw() {
        return raw;
    }

    public Type getType() {
        return type;
    }

    public int getVal() { return val; }

    public void setVal(int val) { this.val = val; }

    @Override
    public String toString() {
        return String.format("Type: %s, Raw: %s, Pos(%d,%d)", type.toString(), raw, row, col);
    }
}
