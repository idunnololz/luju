package com.ggstudios.error;

import com.ggstudios.luju.Token;

public class ParseException extends TokenException {
    public ParseException(String message) {
        super(new Token(Token.Type.INVALID, "", 0, 0), message);
    }

    public ParseException(Token t, String message) {
        super(t, message);
    }
}
