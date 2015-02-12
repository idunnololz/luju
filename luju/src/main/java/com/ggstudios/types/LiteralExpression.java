package com.ggstudios.types;

import com.ggstudios.luju.Token;

public class LiteralExpression extends Expression {
    private Token literal;

    public LiteralExpression(Token literal) {
        this.literal = literal;
        setPos(literal.getRow(), literal.getCol());
    }

    public Token getLiteral() {
        return literal;
    }

    public void setLiteral(Token literal) {
        this.literal = literal;
    }

    @Override
    public String toString() {
        if (literal.getType() == Token.Type.INTLIT) {
            return literal.getVal() + "";
        } else {
            return literal.getRaw();
        }
    }
}
