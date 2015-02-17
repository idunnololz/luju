package com.ggstudios.types;

import com.ggstudios.env.Literal;
import com.ggstudios.luju.Token;

public class LiteralExpression extends Expression {
    private Token literal;
    private Literal proper;

    public LiteralExpression(Token literal) {
        setType(LITERAL_EXPRESSION);

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

    public void setProper(Literal lit) {
        this.proper = lit;
    }
}
