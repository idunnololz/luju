package com.ggstudios.luju;

public class Token {
    private int row, col;
    private String raw;
    private Type type;
    private long val;

    public enum Type {
        EOF(ParseTable.TERM_EOF),
        BOF(ParseTable.TERM_BOF),
        ID(ParseTable.TERM_ID),
        STRINGLIT(ParseTable.TERM_STRINGLIT),
        CHARLIT(ParseTable.TERM_CHARLIT),
        INTLIT(ParseTable.TERM_INTLIT),
        COMMENT(ParseTable.TERM_INVALID),
        DOT(ParseTable.TERM_DOT),
        LT(ParseTable.TERM_LTTHAN),
        LT_EQ(ParseTable.TERM_LTEQ),
        GT(ParseTable.TERM_GTTHAN),
        LPAREN(ParseTable.TERM_LPAREN),
        RPAREN(ParseTable.TERM_RPAREN),
        LBRACK(ParseTable.TERM_LBRACKET),
        RBRACK(ParseTable.TERM_RBRACKET),
        LBRACE(ParseTable.TERM_LBRACE),
        RBRACE(ParseTable.TERM_RBRACE),
        GT_EQ(ParseTable.TERM_GTEQ),
        EQ(ParseTable.TERM_ASSIGN),
        EQ_EQ(ParseTable.TERM_EQUALS),
        NEQ(ParseTable.TERM_NOTEQUALS),
        PLUS(ParseTable.TERM_PLUS),
        MINUS(ParseTable.TERM_MINUS),
        STAR(ParseTable.TERM_TIMES),
        FSLASH(ParseTable.TERM_DIVIDE),
        AMP(ParseTable.TERM_ANDS),
        AMP_AMP(ParseTable.TERM_ANDL),
        MOD(ParseTable.TERM_MOD),
        PIPE(ParseTable.TERM_ORS),
        PIPE_PIPE(ParseTable.TERM_ORL),
        NOT(ParseTable.TERM_NOT),
        COMMA(ParseTable.TERM_COMMA),
        SEMI(ParseTable.TERM_SEMI),
        ABSTRACT(ParseTable.TERM_ABSTRACT),
        BOOLEAN(ParseTable.TERM_BOOLEAN),
        BYTE(ParseTable.TERM_BYTE),
        CHAR(ParseTable.TERM_CHAR),
        CLASS(ParseTable.TERM_CLASS),
        ELSE(ParseTable.TERM_ELSE),
        EXTENDS(ParseTable.TERM_EXTENDS),
        FALSE(ParseTable.TERM_FALSE),
        FINAL(ParseTable.TERM_FINAL),
        FOR(ParseTable.TERM_FOR),
        IF(ParseTable.TERM_IF),
        IMPLEMENTS(ParseTable.TERM_IMPLEMENTS),
        IMPORT(ParseTable.TERM_IMPORT),
        INSTANCEOF(ParseTable.TERM_INSTANCEOF),
        INT(ParseTable.TERM_INT),
        INTERFACE(ParseTable.TERM_INTERFACE),
        NATIVE(ParseTable.TERM_NATIVE),
        NEW(ParseTable.TERM_NEW),
        NULL(ParseTable.TERM_NULL),
        PACKAGE(ParseTable.TERM_PACKAGE),
        PROTECTED(ParseTable.TERM_PROTECTED),
        PUBLIC(ParseTable.TERM_PUBLIC),
        RETURN(ParseTable.TERM_RETURN),
        SHORT(ParseTable.TERM_SHORT),
        STATIC(ParseTable.TERM_STATIC),
        TRUE(ParseTable.TERM_TRUE),
        VOID(ParseTable.TERM_VOID),
        WHILE(ParseTable.TERM_WHILE),
        INVALID(ParseTable.TERM_INVALID),
        THIS(ParseTable.TERM_THIS);

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

    public long getVal() { return val; }

    public void setVal(long val) { this.val = val; }

    @Override
    public String toString() {
        return String.format("Type: %s, Raw: %s, Pos(%d,%d)", type.toString(), raw, row, col);
    }
}
