package com.ggstudios.types;

import com.ggstudios.luju.Token;

public class ThisExpression extends Expression {
    private Token thisToken;

    public ThisExpression(Token thisToken) {
        setType(THIS_EXPRESSION);
        this.thisToken = thisToken;
        setPos(thisToken.getRow(), thisToken.getCol());
    }

    public Token getToken() {
        return thisToken;
    }

    public void setToken(Token thisToken) {
        this.thisToken = thisToken;
    }
}
