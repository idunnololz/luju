package com.ggstudios.error;

import com.ggstudios.luju.Token;

public class WeedException extends TokenException {

    public WeedException(Token t, String message) {
        super(t, message);
    }
}
