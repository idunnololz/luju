package com.ggstudios.error;

import com.ggstudios.luju.Token;

public class ParseException extends TokenException {
    public ParseException(String fileName, String message) {
        super(fileName, new Token(Token.Type.INVALID, "", 0, 0), message);
    }

    public ParseException(String fileName, Token t, String message) {
        super(fileName, t, message);
    }
}
