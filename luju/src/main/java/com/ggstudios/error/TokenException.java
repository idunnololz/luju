package com.ggstudios.error;

import com.ggstudios.luju.Token;

public class TokenException extends RuntimeException {
    private Token t;
    public TokenException(Token t, String message) {
        super(message);
        this.t = t;
    }

    public Token getToken() {
        return t;
    }
}
